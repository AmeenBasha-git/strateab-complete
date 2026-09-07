package com.quantplatform.core.execution.broker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A simulated Broker for Sandbox Backtesting.
 * It does not send orders to the real market. It uses fake money and historical data.
 */
public class MockBrokerAdapter implements BrokerAdapter {

    private final String brokerName = "SANDBOX_MOCK_BROKER";
    private BigDecimal accountEquity = new BigDecimal("100000.00"); // Start with 100k fake money
    
    // Key: Symbol, Value: Quantity
    private final Map<String, Integer> openPositions = new HashMap<>();
    
    // Key: Symbol, Value: Current Mocked Price
    private final Map<String, BigDecimal> mockPrices = new HashMap<>();

    @Override
    public String getBrokerName() {
        return brokerName;
    }

    @Override
    public boolean isMarketOpen() {
        return true; // Always open in a backtest sandbox unless time rules apply
    }

    @Override
    public BigDecimal getCurrentPrice(String symbol) {
        return mockPrices.getOrDefault(symbol, BigDecimal.ZERO);
    }
    
    // Helper for the engine to step through time
    public void setMockPrice(String symbol, BigDecimal price) {
        this.mockPrices.put(symbol, price);
    }

    @Override
    public String placeMarketOrder(String symbol, int quantity, String side, String strategyName) {
        BigDecimal currentPrice = getCurrentPrice(symbol);
        
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Cannot place order. Mock price is zero for " + symbol);
        }

        BigDecimal tradeValue = currentPrice.multiply(BigDecimal.valueOf(quantity));
        
        if (side.equalsIgnoreCase("buy")) {
            if (accountEquity.compareTo(tradeValue) < 0) {
                // Insufficient funds in sandbox
                return null;
            }
            accountEquity = accountEquity.subtract(tradeValue);
            openPositions.put(symbol, openPositions.getOrDefault(symbol, 0) + quantity);
        } else if (side.equalsIgnoreCase("sell")) {
            int currentQty = openPositions.getOrDefault(symbol, 0);
            if (currentQty < quantity) {
                // Cannot short sell more than we own in this simple mock
                quantity = currentQty; 
                tradeValue = currentPrice.multiply(BigDecimal.valueOf(quantity));
            }
            
            accountEquity = accountEquity.add(tradeValue);
            
            if (currentQty - quantity == 0) {
                openPositions.remove(symbol);
            } else {
                openPositions.put(symbol, currentQty - quantity);
            }
        }
        
        return "MOCK-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public List<Candle> getHistoricalBars(String symbol, String timeframe, int limit) {
        // In a real backtest, the engine would feed historical data.
        // For simplicity, we just return an empty list or mock data.
        return new ArrayList<>();
    }

    @Override
    public BigDecimal getAccountEquity() {
        return accountEquity;
    }

    @Override
    public List<String> getOpenPositions() {
        return new ArrayList<>(openPositions.keySet());
    }

    @Override
    public void closePosition(String symbol) {
        int qty = openPositions.getOrDefault(symbol, 0);
        if (qty > 0) {
            placeMarketOrder(symbol, qty, "sell", "SYSTEM");
        }
    }
}
