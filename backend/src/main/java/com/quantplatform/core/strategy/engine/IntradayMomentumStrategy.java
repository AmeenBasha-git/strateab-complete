package com.quantplatform.core.strategy.engine;

import com.quantplatform.core.execution.broker.BrokerAdapter;
import com.quantplatform.core.execution.broker.Candle;
import com.quantplatform.core.execution.model.TradeRecord;
import com.quantplatform.core.execution.repository.TradeRecordRepository;
import com.quantplatform.core.strategy.risk.RiskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class IntradayMomentumStrategy implements TradingStrategy {

    private static final Logger log = LoggerFactory.getLogger(IntradayMomentumStrategy.class);
    
    private final RiskManager riskManager;
    private final TradeRecordRepository tradeRepository;
    
    // Core State
    private String symbol = "SPY";
    private boolean active = false;
    private String positionState = "NO_POSITION";
    private int positionSize = 0;
    private BigDecimal entryPrice = BigDecimal.ZERO;
    private BigDecimal stopLoss = BigDecimal.ZERO;
    private BigDecimal takeProfit = BigDecimal.ZERO;
    
    // Drawdown Protection
    private BigDecimal peakEquity = BigDecimal.ZERO;
    private boolean isKillSwitchTriggered = false;

    // Day tracking for EOD close in backtests
    private java.time.LocalDate lastBarDate = null;

    // UI State for Dashboard
    private BigDecimal currentPrice = BigDecimal.ZERO;
    private BigDecimal currentVwap = BigDecimal.ZERO;
    private BigDecimal upperBand = BigDecimal.ZERO;
    private BigDecimal lowerBand = BigDecimal.ZERO;

    public IntradayMomentumStrategy(RiskManager riskManager, TradeRecordRepository tradeRepository) {
        this.riskManager = riskManager;
        this.tradeRepository = tradeRepository;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public Map<String, Object> getLiveState() {
        Map<String, Object> state = new HashMap<>();
        state.put("position", positionState);
        state.put("positionSize", positionSize);
        state.put("isKillSwitchTriggered", isKillSwitchTriggered);
        state.put("currentPrice", currentPrice);
        state.put("vwap", currentVwap);
        state.put("upperBand", upperBand);
        state.put("lowerBand", lowerBand);
        state.put("entryPrice", entryPrice);
        state.put("stopLoss", stopLoss);
        state.put("takeProfit", takeProfit);
        return state;
    }

    @Override
    public String getStrategyName() {
        return "INTRADAY_MOMENTUM_" + symbol;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public String getDescription() {
        return "A high-frequency intraday momentum strategy that trades breakouts around the Volume Weighted Average Price (VWAP).";
    }

    @Override
    public List<String> getEntryRules() {
        return List.of(
            "Calculates the VWAP using the last 100 1-minute bars.",
            "Establishes a trading channel at VWAP ± 0.5%.",
            "Goes LONG if the current price crosses above the upper channel boundary.",
            "Goes SHORT if the current price crosses below the lower channel boundary."
        );
    }

    @Override
    public List<String> getExitRules() {
        return List.of(
            "Take Profit: 1.0% from entry price.",
            "Stop Loss: 0.5% from entry price.",
            "Time Stop: Closes all positions automatically at the end of the trading session."
        );
    }

    @Override
    public void evaluate(BrokerAdapter broker) {
        if (!active) {
            return;
        }

        if (isKillSwitchTriggered) {
            log.warn("[{}] Kill switch is active (30% Max Drawdown limit reached). Strategy is permanently halted.", getStrategyName());
            return;
        }

        BigDecimal accountEquity = broker.getAccountEquity();
        
        // 1. Drawdown Check
        if (accountEquity.compareTo(peakEquity) > 0) {
            peakEquity = accountEquity; // Record new high water mark
        } else if (peakEquity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal drawdown = peakEquity.subtract(accountEquity).divide(peakEquity, 4, RoundingMode.HALF_UP);
            if (drawdown.compareTo(new BigDecimal("0.30")) >= 0) {
                log.error("[{}] CRITICAL: 30% MAX DRAWDOWN LIMIT REACHED! (Peak: ${}, Current: ${}). Activating Kill Switch.", 
                        getStrategyName(), peakEquity, accountEquity);
                
                isKillSwitchTriggered = true;
                
                // Panic close open positions
                if (!positionState.equals("NO_POSITION") && positionSize > 0) {
                    String side = positionState.equals("LONG") ? "sell" : "buy";
                    broker.placeMarketOrder(getSymbol(), positionSize, side, getStrategyName());
                    positionState = "NO_POSITION";
                    positionSize = 0;
                }
                return;
            }
        }

        // 2. Fetch Data (Last 100 bars)
        List<Candle> bars = broker.getHistoricalBars(getSymbol(), "1Min", 100);
        if (bars.isEmpty()) {
            log.warn("[{}] No historical bars returned. Aborting evaluation.", getStrategyName());
            return;
        }

        Candle lastBar = bars.get(bars.size() - 1);
        this.currentPrice = lastBar.getClose();

        boolean isBacktest = "HISTORICAL_BACKTEST".equals(broker.getBrokerName());

        // 3. EOD close: detect day change in backtest mode
        ZoneId etZone = ZoneId.of("America/New_York");
        ZonedDateTime nowET = lastBar.getTimestamp().withZoneSameInstant(etZone);
        java.time.LocalDate currentBarDate = nowET.toLocalDate();

        if (isBacktest && lastBarDate != null && !currentBarDate.equals(lastBarDate)) {
            if (!positionState.equals("NO_POSITION") && positionSize > 0) {
                log.info("[{}] New trading day detected. Closing overnight {} position.", getStrategyName(), positionState);
                String side = positionState.equals("LONG") ? "sell" : "buy";
                BigDecimal closePrice = broker.getCurrentPrice(getSymbol());
                if (closePrice == null || closePrice.compareTo(BigDecimal.ZERO) == 0) closePrice = entryPrice;
                broker.placeMarketOrder(getSymbol(), positionSize, side, getStrategyName());
                saveTradeRecord(positionState, entryPrice, closePrice);
                positionState = "NO_POSITION";
                positionSize = 0;
            }
        }
        lastBarDate = currentBarDate;

        // Time Filter (RTH: 09:30 to 15:55 ET)
        if (!isBacktest) {
            LocalTime time = nowET.toLocalTime();
            LocalTime rthStart = LocalTime.of(9, 30);
            LocalTime rthEnd = LocalTime.of(15, 55);

            if (time.isBefore(rthStart) || time.isAfter(rthEnd)) {
                if (!positionState.equals("NO_POSITION") && positionSize > 0) {
                    log.info("[{}] Outside RTH. Closing open {} position.", getStrategyName(), positionState);
                    String side = positionState.equals("LONG") ? "sell" : "buy";

                    BigDecimal closePrice = broker.getCurrentPrice(getSymbol());
                    if (closePrice == null || closePrice.compareTo(BigDecimal.ZERO) == 0) closePrice = entryPrice;

                    broker.placeMarketOrder(getSymbol(), positionSize, side, getStrategyName());
                    saveTradeRecord(positionState, entryPrice, closePrice);
                    positionState = "NO_POSITION";
                    positionSize = 0;
                }
                return;
            }
        }

        // Compute intraday VWAP: same-day bars only
        BigDecimal cumVol = BigDecimal.ZERO;
        BigDecimal cumPv = BigDecimal.ZERO;

        for (Candle bar : bars) {
            ZonedDateTime barTime = bar.getTimestamp().withZoneSameInstant(etZone);
            boolean includeBar = barTime.toLocalDate().equals(currentBarDate);
            if (includeBar) {
                BigDecimal price = bar.getClose();
                BigDecimal vol = bar.getVolume();
                if (vol.compareTo(BigDecimal.ZERO) == 0) vol = BigDecimal.ONE;
                cumVol = cumVol.add(vol);
                cumPv = cumPv.add(price.multiply(vol));
            }
        }

        this.currentVwap = cumVol.compareTo(BigDecimal.ZERO) > 0 
                ? cumPv.divide(cumVol, 2, RoundingMode.HALF_UP) 
                : currentPrice;

        // Determine Bounds (VWAP +/- 0.5%)
        BigDecimal volatilityMultiplier = new BigDecimal("0.005"); 
        this.upperBand = currentVwap.multiply(BigDecimal.ONE.add(volatilityMultiplier));
        this.lowerBand = currentVwap.multiply(BigDecimal.ONE.subtract(volatilityMultiplier));


        // Execution Logic
        BigDecimal riskPercent = new BigDecimal("0.02"); // 2% risk

        BigDecimal maxPositionEquity = accountEquity.multiply(new BigDecimal("0.25"));
        int maxAffordableShares = currentPrice.compareTo(BigDecimal.ZERO) > 0
                ? maxPositionEquity.divide(currentPrice, 0, RoundingMode.DOWN).intValue()
                : 0;

        if (positionState.equals("NO_POSITION")) {
            if (currentPrice.compareTo(upperBand) > 0) {
                log.info("[{}] Breakout above Upper Band! Going LONG.", getStrategyName());
                BigDecimal calculatedStopLoss = currentPrice.multiply(new BigDecimal("0.995"));
                BigDecimal calculatedTakeProfit = currentPrice.multiply(new BigDecimal("1.010"));

                int shares = Math.min(
                        riskManager.calculatePositionSize(accountEquity, riskPercent, currentPrice, calculatedStopLoss),
                        maxAffordableShares);
                if (shares > 0 && broker.placeMarketOrder(getSymbol(), shares, "buy", getStrategyName()) != null) {
                    positionState = "LONG";
                    positionSize = shares;
                    entryPrice = currentPrice;
                    stopLoss = calculatedStopLoss;
                    takeProfit = calculatedTakeProfit;
                }
            } else if (currentPrice.compareTo(lowerBand) < 0) {
                log.info("[{}] Breakdown below Lower Band! Going SHORT.", getStrategyName());
                BigDecimal calculatedStopLoss = currentPrice.multiply(new BigDecimal("1.005"));
                BigDecimal calculatedTakeProfit = currentPrice.multiply(new BigDecimal("0.990"));

                int shares = Math.min(
                        riskManager.calculatePositionSize(accountEquity, riskPercent, calculatedStopLoss, currentPrice),
                        maxAffordableShares);
                if (shares > 0 && broker.placeMarketOrder(getSymbol(), shares, "sell", getStrategyName()) != null) {
                    positionState = "SHORT";
                    positionSize = shares;
                    entryPrice = currentPrice;
                    stopLoss = calculatedStopLoss;
                    takeProfit = calculatedTakeProfit;
                }
            }
        } else if (positionState.equals("LONG")) {
            if (currentPrice.compareTo(stopLoss) <= 0) {
                log.info("[{}] LONG Stop Loss hit at ${}. Closing.", getStrategyName(), currentPrice);
                broker.placeMarketOrder(getSymbol(), positionSize, "sell", getStrategyName());
                saveTradeRecord("LONG", entryPrice, currentPrice);
                positionState = "NO_POSITION";
                positionSize = 0;
            } else if (currentPrice.compareTo(takeProfit) >= 0) {
                log.info("[{}] LONG Take Profit hit at ${}. Closing.", getStrategyName(), currentPrice);
                broker.placeMarketOrder(getSymbol(), positionSize, "sell", getStrategyName());
                saveTradeRecord("LONG", entryPrice, currentPrice);
                positionState = "NO_POSITION";
                positionSize = 0;
            }
        } else if (positionState.equals("SHORT")) {
            if (currentPrice.compareTo(stopLoss) >= 0) {
                log.info("[{}] SHORT Stop Loss hit at ${}. Closing.", getStrategyName(), currentPrice);
                broker.placeMarketOrder(getSymbol(), positionSize, "buy", getStrategyName());
                saveTradeRecord("SHORT", entryPrice, currentPrice);
                positionState = "NO_POSITION";
                positionSize = 0;
            } else if (currentPrice.compareTo(takeProfit) <= 0) {
                log.info("[{}] SHORT Take Profit hit at ${}. Closing.", getStrategyName(), currentPrice);
                broker.placeMarketOrder(getSymbol(), positionSize, "buy", getStrategyName());
                saveTradeRecord("SHORT", entryPrice, currentPrice);
                positionState = "NO_POSITION";
                positionSize = 0;
            }
        }
    }
    
    private void saveTradeRecord(String direction, BigDecimal entry, BigDecimal exit) {
        BigDecimal pnl = direction.equals("LONG") ? exit.subtract(entry) : entry.subtract(exit);
        // Multiply by position size for total PnL
        BigDecimal totalPnl = pnl.multiply(new BigDecimal(positionSize));
        
        TradeRecord record = new TradeRecord();
        record.setStrategyName(getStrategyName());
        record.setSymbol(getSymbol());
        record.setDirection(direction);
        record.setEntryPrice(entry);
        record.setExitPrice(exit);
        record.setPnl(totalPnl);
        record.setExecutionTime(LocalDateTime.now());
        
        tradeRepository.save(record);
        log.info("[{}] Saved Trade to DB. Direction: {}, Shares: {}, Total PnL: ${}", getStrategyName(), direction, positionSize, totalPnl);
    }

    @Override
    public TradingStrategy createBacktestInstance() {
        return new IntradayMomentumStrategy(riskManager, tradeRepository);
    }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getCurrentVwap() { return currentVwap; }
    public BigDecimal getUpperBand() { return upperBand; }
    public BigDecimal getLowerBand() { return lowerBand; }
}
