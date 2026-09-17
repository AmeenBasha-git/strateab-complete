package com.quantplatform.core.backtest.service;

import com.quantplatform.core.backtest.broker.HistoricalBrokerAdapter;
import com.quantplatform.core.backtest.domain.BacktestDataset;
import com.quantplatform.core.backtest.domain.BacktestRun;
import com.quantplatform.core.backtest.repository.BacktestDatasetRepository;
import com.quantplatform.core.backtest.repository.BacktestRunRepository;
import com.quantplatform.core.common.exception.ResourceNotFoundException;
import com.quantplatform.core.execution.broker.Candle;
import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.engine.StrategyManager;
import com.quantplatform.core.strategy.engine.TradingStrategy;
import com.quantplatform.core.strategy.repository.StrategyRepository;
import com.quantplatform.core.strategy.service.UserStrategyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Orchestrates the backtest execution loop:
 * <ol>
 *   <li>Load dataset metadata from DB</li>
 *   <li>Create a {@link HistoricalBrokerAdapter}</li>
 *   <li>Look up the target {@link TradingStrategy} from the strategy registry</li>
 *   <li>Stream candles from DB: advance the adapter, evaluate the strategy</li>
 *   <li>Compute results and persist the {@link BacktestRun}</li>
 * </ol>
 */
@Service
public class BacktestManager {

    private static final Logger log = LoggerFactory.getLogger(BacktestManager.class);

    private final BacktestDatasetRepository datasetRepository;
    private final BacktestRunRepository runRepository;
    private final StrategyManager strategyManager;
    private final StrategyRepository strategyRepository;
    private final UserStrategyLoader userStrategyLoader;
    private final JdbcTemplate jdbcTemplate;

    public BacktestManager(BacktestDatasetRepository datasetRepository,
                           BacktestRunRepository runRepository,
                           StrategyManager strategyManager,
                           StrategyRepository strategyRepository,
                           UserStrategyLoader userStrategyLoader,
                           JdbcTemplate jdbcTemplate) {
        this.datasetRepository = datasetRepository;
        this.runRepository = runRepository;
        this.strategyManager = strategyManager;
        this.strategyRepository = strategyRepository;
        this.userStrategyLoader = userStrategyLoader;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public BacktestRun execute(UUID datasetId, String strategyName,
                               BigDecimal startingEquity, UUID userId) {
        return execute(datasetId, strategyName, startingEquity, userId,
                new BigDecimal("5"), new BigDecimal("1.00"));
    }

    public BacktestRun execute(UUID datasetId, String strategyName,
                               BigDecimal startingEquity, UUID userId,
                               BigDecimal slippageBps, BigDecimal commissionPerTrade) {
        BacktestDataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset not found: " + datasetId));

        // Find strategy from engine registry first, then fall back to user strategies in DB
        TradingStrategy template = strategyManager.getAllStrategies().stream()
                .filter(s -> s.getStrategyName().equals(strategyName))
                .findFirst()
                .orElse(null);

        if (template == null) {
            Strategy dbStrategy = strategyRepository.findByNameAndOwnerIdAndDeletedFalse(strategyName, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Strategy not found: " + strategyName));
            template = userStrategyLoader.load(dbStrategy);
        }

        TradingStrategy strategy = template.createBacktestInstance();

        BacktestRun run = BacktestRun.create(datasetId, strategyName, startingEquity, userId);
        runRepository.save(run);

        try {
            // Create the historical broker with slippage and commission modeling
            HistoricalBrokerAdapter broker = new HistoricalBrokerAdapter(
                    dataset.getSymbol(), startingEquity, slippageBps, commissionPerTrade
            );

            strategy.setSymbol(dataset.getSymbol());
            strategy.setActive(true);

            log.info("Starting backtest: strategy={}, symbol={}, dataset={}",
                    strategyName, dataset.getSymbol(), dataset.getName());

            String sql = "SELECT * FROM historical_bars WHERE symbol = ? AND timestamp_et >= ? AND timestamp_et <= ? ORDER BY timestamp_et ASC";

            java.sql.Timestamp startTs = java.sql.Timestamp.valueOf(dataset.getStartDate().atStartOfDay());
            java.sql.Timestamp endTs = java.sql.Timestamp.valueOf(dataset.getEndDate().atTime(23, 59, 59));

            try (Stream<Candle> stream = jdbcTemplate.queryForStream(sql,
                    (rs, rowNum) -> new Candle(
                            rs.getTimestamp("timestamp_et").toInstant().atZone(ZoneId.of("America/New_York")),
                            rs.getBigDecimal("open"),
                            rs.getBigDecimal("high"),
                            rs.getBigDecimal("low"),
                            rs.getBigDecimal("close"),
                            rs.getBigDecimal("volume"),
                            rs.getBigDecimal("vwap_rth"),
                            rs.getBigDecimal("vwap_eth")
                    ),
                    dataset.getSymbol(), startTs, endTs)) {

                stream.forEach(candle -> {
                    broker.addBar(candle);
                    try {
                        strategy.evaluate(broker);
                    } catch (Exception e) {
                        log.warn("Strategy error at bar {}: {}", broker.getCurrentBarIndex(), e.getMessage());
                    }
                });
            }

            if (broker.getTotalBars() < 2) {
                run.markFailed("Dataset has fewer than 2 bars");
                runRepository.save(run);
                return run;
            }

            // Close any remaining open positions at the last bar's price
            for (String openSymbol : broker.getOpenPositions()) {
                broker.closePosition(openSymbol);
            }

            broker.finalizeEquityCurve();

            // Compute results from executed trades
            List<HistoricalBrokerAdapter.TradeEntry> trades = broker.getExecutedTrades();
            BigDecimal finalEquity = broker.getAccountEquity();
            BigDecimal totalPnl = finalEquity.subtract(startingEquity);

            List<HistoricalBrokerAdapter.TradeEntry> closingTrades = trades.stream()
                    .filter(HistoricalBrokerAdapter.TradeEntry::isClosing)
                    .toList();

            int totalTrades = closingTrades.size();
            long wins = closingTrades.stream().filter(t -> t.pnl().compareTo(BigDecimal.ZERO) > 0).count();

            BigDecimal winRate = totalTrades > 0
                    ? BigDecimal.valueOf(wins).divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            BigDecimal grossProfit = closingTrades.stream()
                    .map(HistoricalBrokerAdapter.TradeEntry::pnl)
                    .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal grossLoss = closingTrades.stream()
                    .map(HistoricalBrokerAdapter.TradeEntry::pnl)
                    .filter(p -> p.compareTo(BigDecimal.ZERO) < 0)
                    .map(BigDecimal::abs)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal profitFactor = grossLoss.compareTo(BigDecimal.ZERO) > 0
                    ? grossProfit.divide(grossLoss, 4, RoundingMode.HALF_UP)
                    : grossProfit.compareTo(BigDecimal.ZERO) > 0
                        ? new BigDecimal("999.0000")
                        : BigDecimal.ZERO;

            run.markCompleted(finalEquity, totalTrades, winRate, profitFactor,
                    totalPnl, broker.getMaxDrawdown());

            // === Professional metrics computation ===
            Map<LocalDate, BigDecimal> dailyEquity = broker.getDailyEquity();
            List<LocalDate> dates = new ArrayList<>(dailyEquity.keySet());
            int tradingDays = dates.size();

            // Daily returns
            List<BigDecimal> dailyReturns = new ArrayList<>();
            for (int i = 1; i < dates.size(); i++) {
                BigDecimal prev = dailyEquity.get(dates.get(i - 1));
                BigDecimal curr = dailyEquity.get(dates.get(i));
                if (prev.compareTo(BigDecimal.ZERO) > 0) {
                    dailyReturns.add(curr.subtract(prev).divide(prev, 8, RoundingMode.HALF_UP));
                }
            }

            BigDecimal sharpeRatio = BigDecimal.ZERO;
            BigDecimal sortinoRatio = BigDecimal.ZERO;
            BigDecimal annualizedVol = BigDecimal.ZERO;
            BigDecimal annualizedReturn = BigDecimal.ZERO;
            BigDecimal calmarRatio = BigDecimal.ZERO;

            if (!dailyReturns.isEmpty()) {
                BigDecimal meanReturn = dailyReturns.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(dailyReturns.size()), 8, RoundingMode.HALF_UP);

                BigDecimal variance = dailyReturns.stream()
                        .map(r -> r.subtract(meanReturn).pow(2))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(dailyReturns.size()), 8, RoundingMode.HALF_UP);
                double stdDev = Math.sqrt(variance.doubleValue());

                BigDecimal downsideVariance = dailyReturns.stream()
                        .filter(r -> r.compareTo(BigDecimal.ZERO) < 0)
                        .map(r -> r.pow(2))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(dailyReturns.size()), 8, RoundingMode.HALF_UP);
                double downsideDev = Math.sqrt(downsideVariance.doubleValue());

                double sqrt252 = Math.sqrt(252.0);
                annualizedVol = BigDecimal.valueOf(stdDev * sqrt252).setScale(4, RoundingMode.HALF_UP);

                if (stdDev > 0) {
                    sharpeRatio = BigDecimal.valueOf(meanReturn.doubleValue() / stdDev * sqrt252)
                            .setScale(4, RoundingMode.HALF_UP);
                }
                if (downsideDev > 0) {
                    sortinoRatio = BigDecimal.valueOf(meanReturn.doubleValue() / downsideDev * sqrt252)
                            .setScale(4, RoundingMode.HALF_UP);
                }

                // CAGR
                if (tradingDays > 1 && finalEquity.compareTo(BigDecimal.ZERO) > 0
                        && startingEquity.compareTo(BigDecimal.ZERO) > 0) {
                    double totalReturn = finalEquity.doubleValue() / startingEquity.doubleValue();
                    double years = tradingDays / 252.0;
                    annualizedReturn = BigDecimal.valueOf(Math.pow(totalReturn, 1.0 / years) - 1.0)
                            .setScale(4, RoundingMode.HALF_UP);
                }

                if (broker.getMaxDrawdown().compareTo(BigDecimal.ZERO) > 0) {
                    calmarRatio = annualizedReturn.divide(broker.getMaxDrawdown(), 4, RoundingMode.HALF_UP);
                }
            }

            // Trade statistics
            List<BigDecimal> winPnls = closingTrades.stream()
                    .map(HistoricalBrokerAdapter.TradeEntry::pnl)
                    .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            List<BigDecimal> lossPnls = closingTrades.stream()
                    .map(HistoricalBrokerAdapter.TradeEntry::pnl)
                    .filter(p -> p.compareTo(BigDecimal.ZERO) < 0)
                    .toList();

            BigDecimal avgWin = winPnls.isEmpty() ? BigDecimal.ZERO
                    : winPnls.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(winPnls.size()), 4, RoundingMode.HALF_UP);
            BigDecimal avgLoss = lossPnls.isEmpty() ? BigDecimal.ZERO
                    : lossPnls.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(lossPnls.size()), 4, RoundingMode.HALF_UP);
            BigDecimal largestWin = winPnls.isEmpty() ? BigDecimal.ZERO
                    : winPnls.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal largestLoss = lossPnls.isEmpty() ? BigDecimal.ZERO
                    : lossPnls.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            BigDecimal expectancy = totalTrades > 0
                    ? totalPnl.divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal riskRewardRatio = avgLoss.compareTo(BigDecimal.ZERO) < 0
                    ? avgWin.divide(avgLoss.abs(), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Consecutive wins/losses streaks
            int maxConsecWins = 0, maxConsecLosses = 0, curWins = 0, curLosses = 0;
            for (HistoricalBrokerAdapter.TradeEntry t : closingTrades) {
                if (t.pnl().compareTo(BigDecimal.ZERO) > 0) {
                    curWins++;
                    curLosses = 0;
                    maxConsecWins = Math.max(maxConsecWins, curWins);
                } else {
                    curLosses++;
                    curWins = 0;
                    maxConsecLosses = Math.max(maxConsecLosses, curLosses);
                }
            }

            // Directional breakdown
            List<HistoricalBrokerAdapter.TradeEntry> longClosing = closingTrades.stream()
                    .filter(t -> "SELL".equals(t.side())).toList();
            List<HistoricalBrokerAdapter.TradeEntry> shortClosing = closingTrades.stream()
                    .filter(t -> "BUY_TO_COVER".equals(t.side())).toList();

            int longTradeCount = longClosing.size();
            int shortTradeCount = shortClosing.size();

            long longWins = longClosing.stream().filter(t -> t.pnl().compareTo(BigDecimal.ZERO) > 0).count();
            long shortWins = shortClosing.stream().filter(t -> t.pnl().compareTo(BigDecimal.ZERO) > 0).count();

            BigDecimal longWinRateVal = longTradeCount > 0
                    ? BigDecimal.valueOf(longWins).divide(BigDecimal.valueOf(longTradeCount), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            BigDecimal shortWinRateVal = shortTradeCount > 0
                    ? BigDecimal.valueOf(shortWins).divide(BigDecimal.valueOf(shortTradeCount), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            // Equity curve JSON for charting
            List<Map<String, Object>> equityCurveJson = new ArrayList<>();
            for (Map.Entry<LocalDate, BigDecimal> entry : dailyEquity.entrySet()) {
                Map<String, Object> point = new HashMap<>();
                point.put("date", entry.getKey().toString());
                point.put("equity", entry.getValue().setScale(2, RoundingMode.HALF_UP));
                equityCurveJson.add(point);
            }

            run.setExtendedMetrics(
                    sharpeRatio, sortinoRatio, calmarRatio,
                    annualizedReturn, annualizedVol,
                    avgWin, avgLoss, largestWin, largestLoss,
                    expectancy, riskRewardRatio,
                    maxConsecWins, maxConsecLosses,
                    longTradeCount, shortTradeCount,
                    longWinRateVal, shortWinRateVal,
                    broker.getTotalCommission(),
                    equityCurveJson
            );

            log.info("Backtest complete: strategy={}, P&L={}, trades={}, winRate={}%, sharpe={}, sortino={}, calmar={}",
                    strategyName, totalPnl.setScale(2, RoundingMode.HALF_UP),
                    totalTrades, winRate.setScale(1, RoundingMode.HALF_UP),
                    sharpeRatio, sortinoRatio, calmarRatio);

        } catch (Exception e) {
            log.error("Backtest failed: {}", e.getMessage(), e);
            run.markFailed(e.getMessage());
        }

        runRepository.save(run);
        return run;
    }
}
