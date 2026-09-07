package com.quantplatform.core.backtest.broker;

import com.quantplatform.core.execution.broker.BrokerAdapter;
import com.quantplatform.core.execution.broker.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A {@link BrokerAdapter} that replays historical candle data bar-by-bar.
 *
 * <p>Supports both long and short positions. Position quantity is signed:
 * positive = long, negative = short. Round-trip P&L is computed when a
 * position is reduced or closed, regardless of direction.
 *
 * <p>Instances are NOT Spring beans — they are created per-backtest-run by
 * {@link com.quantplatform.core.backtest.service.BacktestManager}.
 */
public class HistoricalBrokerAdapter implements BrokerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HistoricalBrokerAdapter.class);

    private final String symbol;
    private final List<Candle> historicalWindow = new ArrayList<>();
    private static final int MAX_WINDOW_SIZE = 1000;

    private int currentBarIndex = -1;
    private int totalBarsProcessed = 0;

    private BigDecimal accountEquity;
    private BigDecimal peakEquity;
    private BigDecimal maxDrawdown = BigDecimal.ZERO;

    // Signed quantity: positive = long, negative = short
    private final Map<String, Integer> positions = new HashMap<>();
    private final Map<String, BigDecimal> entryPrices = new HashMap<>();
    private final List<TradeEntry> executedTrades = new ArrayList<>();

    private final BigDecimal slippageBps;
    private final BigDecimal commissionPerTrade;

    private BigDecimal totalCommission = BigDecimal.ZERO;
    private final Map<LocalDate, BigDecimal> dailyEquity = new LinkedHashMap<>();
    private LocalDate lastSnapshotDate = null;

    public HistoricalBrokerAdapter(String symbol, BigDecimal startingEquity) {
        this(symbol, startingEquity, new BigDecimal("5"), new BigDecimal("1.00"));
    }

    public HistoricalBrokerAdapter(String symbol, BigDecimal startingEquity,
                                   BigDecimal slippageBps, BigDecimal commissionPerTrade) {
        this.symbol = symbol;
        this.accountEquity = startingEquity;
        this.peakEquity = startingEquity;
        this.slippageBps = slippageBps;
        this.commissionPerTrade = commissionPerTrade;
    }

    public void addBar(Candle candle) {
        if (historicalWindow.size() >= MAX_WINDOW_SIZE) {
            historicalWindow.remove(0);
        }
        historicalWindow.add(candle);
        currentBarIndex = historicalWindow.size() - 1;
        totalBarsProcessed++;
        updateDrawdown();

        LocalDate barDate = candle.getTimestamp().withZoneSameInstant(ZoneId.of("America/New_York")).toLocalDate();
        if (lastSnapshotDate == null || !barDate.equals(lastSnapshotDate)) {
            if (lastSnapshotDate != null) {
                dailyEquity.put(lastSnapshotDate, getAccountEquity());
            }
            lastSnapshotDate = barDate;
        }
    }

    public void finalizeEquityCurve() {
        if (lastSnapshotDate != null) {
            dailyEquity.put(lastSnapshotDate, getAccountEquity());
        }
    }

    public int getCurrentBarIndex() {
        return totalBarsProcessed - 1;
    }

    public int getTotalBars() {
        return totalBarsProcessed;
    }

    @Override
    public String getBrokerName() {
        return "HISTORICAL_BACKTEST";
    }

    @Override
    public boolean isMarketOpen() {
        return true;
    }

    @Override
    public BigDecimal getCurrentPrice(String symbol) {
        if (currentBarIndex < 0 || currentBarIndex >= historicalWindow.size()) {
            return BigDecimal.ZERO;
        }
        return historicalWindow.get(currentBarIndex).getClose();
    }

    @Override
    public String placeMarketOrder(String symbol, int quantity, String side, String strategyName) {
        BigDecimal rawPrice = getCurrentPrice(symbol);
        if (rawPrice.compareTo(BigDecimal.ZERO) == 0) return null;

        BigDecimal slippageMultiplier = slippageBps.divide(new BigDecimal("10000"), 6, RoundingMode.HALF_UP);
        BigDecimal fillPrice;
        if ("buy".equalsIgnoreCase(side)) {
            fillPrice = rawPrice.multiply(BigDecimal.ONE.add(slippageMultiplier));
        } else {
            fillPrice = rawPrice.multiply(BigDecimal.ONE.subtract(slippageMultiplier));
        }
        fillPrice = fillPrice.setScale(4, RoundingMode.HALF_UP);

        String orderId = "BT-" + UUID.randomUUID().toString().substring(0, 8);
        int currentQty = positions.getOrDefault(symbol, 0);
        boolean isBuy = "buy".equalsIgnoreCase(side);
        int orderSign = isBuy ? 1 : -1;
        int orderQty = quantity * orderSign; // positive for buy, negative for sell
        int newQty = currentQty + orderQty;

        BigDecimal tradeValue = fillPrice.multiply(BigDecimal.valueOf(quantity));

        // Determine if this trade closes (fully or partially) an existing position
        boolean isClosing = (currentQty > 0 && !isBuy) || (currentQty < 0 && isBuy);

        if (isClosing) {
            int closingQty = Math.min(quantity, Math.abs(currentQty));
            BigDecimal entryPrice = entryPrices.getOrDefault(symbol, fillPrice);

            BigDecimal pnl;
            if (currentQty > 0) {
                pnl = fillPrice.subtract(entryPrice).multiply(BigDecimal.valueOf(closingQty));
            } else {
                pnl = entryPrice.subtract(fillPrice).multiply(BigDecimal.valueOf(closingQty));
            }
            pnl = pnl.subtract(commissionPerTrade.multiply(new BigDecimal("2")));

            if (currentQty > 0) {
                accountEquity = accountEquity.add(fillPrice.multiply(BigDecimal.valueOf(closingQty)))
                        .subtract(commissionPerTrade);
            } else {
                BigDecimal marginReturn = entryPrice.multiply(BigDecimal.valueOf(closingQty));
                BigDecimal shortProfit = entryPrice.subtract(fillPrice).multiply(BigDecimal.valueOf(closingQty));
                accountEquity = accountEquity.add(marginReturn).add(shortProfit)
                        .subtract(commissionPerTrade);
            }
            totalCommission = totalCommission.add(commissionPerTrade);

            String closeSide = isBuy ? "BUY_TO_COVER" : "SELL";
            executedTrades.add(new TradeEntry(strategyName, symbol, closeSide, closingQty, fillPrice,
                    historicalWindow.get(currentBarIndex).getTimestamp(), pnl, true));

            // Handle any excess quantity that opens a new position in the opposite direction
            int excessQty = quantity - closingQty;
            if (excessQty > 0) {
                BigDecimal excessValue = fillPrice.multiply(BigDecimal.valueOf(excessQty));
                if (isBuy) {
                    accountEquity = accountEquity.subtract(excessValue).subtract(commissionPerTrade);
                } else {
                    accountEquity = accountEquity.subtract(excessValue).subtract(commissionPerTrade);
                }
                totalCommission = totalCommission.add(commissionPerTrade);
                entryPrices.put(symbol, fillPrice);
                String openSide = isBuy ? "BUY" : "SELL_SHORT";
                executedTrades.add(new TradeEntry(strategyName, symbol, openSide, excessQty, fillPrice,
                        historicalWindow.get(currentBarIndex).getTimestamp()));
            }

            if (newQty == 0) {
                positions.remove(symbol);
                entryPrices.remove(symbol);
            } else {
                positions.put(symbol, newQty);
                if (excessQty > 0) {
                    entryPrices.put(symbol, fillPrice);
                }
            }

        } else {
            // Opening or adding to a position
            BigDecimal totalCost = tradeValue.add(commissionPerTrade);
            if (accountEquity.compareTo(totalCost) < 0) {
                log.warn("[Backtest] Insufficient equity: need {} have {}", totalCost, accountEquity);
                return null;
            }
            accountEquity = accountEquity.subtract(totalCost);
            totalCommission = totalCommission.add(commissionPerTrade);

            if (currentQty == 0) {
                entryPrices.put(symbol, fillPrice);
            }
            positions.put(symbol, newQty);

            String openSide = isBuy ? "BUY" : "SELL_SHORT";
            executedTrades.add(new TradeEntry(strategyName, symbol, openSide, quantity, fillPrice,
                    historicalWindow.get(currentBarIndex).getTimestamp()));
        }

        updateDrawdown();
        return orderId;
    }

    @Override
    public List<Candle> getHistoricalBars(String symbol, String timeframe, int limit) {
        if (currentBarIndex < 0) return List.of();

        int start = Math.max(0, currentBarIndex - limit + 1);
        int end = currentBarIndex + 1;
        return new ArrayList<>(historicalWindow.subList(start, end));
    }

    @Override
    public BigDecimal getAccountEquity() {
        BigDecimal positionValue = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> pos : positions.entrySet()) {
            BigDecimal currentPrice = getCurrentPrice(pos.getKey());
            BigDecimal entryPrice = entryPrices.getOrDefault(pos.getKey(), currentPrice);
            int qty = pos.getValue();
            if (qty > 0) {
                positionValue = positionValue.add(
                        currentPrice.multiply(BigDecimal.valueOf(qty)));
            } else {
                int absQty = Math.abs(qty);
                positionValue = positionValue.add(
                        entryPrice.multiply(BigDecimal.valueOf(absQty))
                                .add(entryPrice.subtract(currentPrice).multiply(BigDecimal.valueOf(absQty))));
            }
        }
        return accountEquity.add(positionValue);
    }

    @Override
    public List<String> getOpenPositions() {
        return new ArrayList<>(positions.keySet());
    }

    @Override
    public void closePosition(String symbol) {
        int qty = positions.getOrDefault(symbol, 0);
        if (qty > 0) {
            placeMarketOrder(symbol, qty, "sell", "BACKTEST_CLOSE");
        } else if (qty < 0) {
            placeMarketOrder(symbol, Math.abs(qty), "buy", "BACKTEST_CLOSE");
        }
    }

    public List<TradeEntry> getExecutedTrades() {
        return executedTrades;
    }

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    public BigDecimal getTotalCommission() {
        return totalCommission;
    }

    public Map<LocalDate, BigDecimal> getDailyEquity() {
        return dailyEquity;
    }

    private void updateDrawdown() {
        BigDecimal equity = getAccountEquity();
        if (equity.compareTo(peakEquity) > 0) {
            peakEquity = equity;
        }
        if (peakEquity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal drawdown = peakEquity.subtract(equity)
                    .divide(peakEquity, 4, RoundingMode.HALF_UP);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }
    }

    public record TradeEntry(
            String strategyName,
            String symbol,
            String side,
            int quantity,
            BigDecimal price,
            java.time.ZonedDateTime timestamp,
            BigDecimal pnl,
            boolean isClosing
    ) {
        public TradeEntry(String strategyName, String symbol, String side,
                          int quantity, BigDecimal price, java.time.ZonedDateTime timestamp) {
            this(strategyName, symbol, side, quantity, price, timestamp, BigDecimal.ZERO, false);
        }

        public TradeEntry(String strategyName, String symbol, String side,
                          int quantity, BigDecimal price, java.time.ZonedDateTime timestamp, BigDecimal pnl) {
            this(strategyName, symbol, side, quantity, price, timestamp, pnl, true);
        }
    }
}
