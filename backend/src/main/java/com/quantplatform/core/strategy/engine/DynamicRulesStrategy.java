package com.quantplatform.core.strategy.engine;

import com.quantplatform.core.execution.broker.BrokerAdapter;
import com.quantplatform.core.strategy.model.UserStrategyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A strategy that executes user-defined rules dynamically rather than hardcoded logic.
 */
public class DynamicRulesStrategy implements TradingStrategy {

    private static final Logger log = LoggerFactory.getLogger(DynamicRulesStrategy.class);

    private final UserStrategyConfig config;
    private boolean active = false;

    // Track entry details for exits
    private BigDecimal entryPrice = BigDecimal.ZERO;
    private int positionSize = 0;

    // For mocking indicator values during backtest
    private double currentMockedIndicatorValue = 0.0;

    public DynamicRulesStrategy(UserStrategyConfig config) {
           this.config = config;
    }

    // Helper for backtest engine to inject mocked RSI etc
    public void setCurrentMockedIndicatorValue(double value) {
        this.currentMockedIndicatorValue = value;
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
        state.put("position", positionSize > 0 ? "LONG" : "NO_POSITION");
        state.put("killSwitchTriggered", false);
        return state;
    }

    @Override
    public String getStrategyName() {
        return config.getStrategyName() != null ? config.getStrategyName() : "DYNAMIC_USER_STRAT";
    }

    @Override
    public String getSymbol() {
        return config.getSymbol() != null ? config.getSymbol() : "UNKNOWN";
    }

    @Override
    public String getDescription() {
        return "User defined custom rules engine strategy.";
    }

    @Override
    public List<String> getEntryRules() {
        return List.of("Dynamic Rule: " + config.getEntryIndicator() + " " + config.getEntryCondition() + " " + config.getEntryThreshold());
    }

    @Override
    public List<String> getExitRules() {
        return List.of(
            "Take Profit: " + (config.getTakeProfitPercentage() * 100) + "%",
            "Stop Loss: " + (config.getStopLossPercentage() * 100) + "%"
        );
    }

    @Override
    public void evaluate(BrokerAdapter broker) {
        if (!active) {
            return;
        }

        BigDecimal currentPrice = broker.getCurrentPrice(getSymbol());
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) return;

        // Position Management (Exit rules)
        List<String> openPositions = broker.getOpenPositions();
        if (openPositions.contains(getSymbol()) && positionSize > 0 && entryPrice.compareTo(BigDecimal.ZERO) > 0) {
            
            BigDecimal takeProfitPrice = entryPrice.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(config.getTakeProfitPercentage())));
            BigDecimal stopLossPrice = entryPrice.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(config.getStopLossPercentage())));

            if (currentPrice.compareTo(takeProfitPrice) >= 0) {
                log.info("Take Profit Triggered! Selling.");
                broker.placeMarketOrder(getSymbol(), positionSize, "sell", getStrategyName());
                positionSize = 0;
                entryPrice = BigDecimal.ZERO;
            } else if (currentPrice.compareTo(stopLossPrice) <= 0) {
                log.info("Stop Loss Triggered! Selling.");
                broker.placeMarketOrder(getSymbol(), positionSize, "sell", getStrategyName());
                positionSize = 0;
                entryPrice = BigDecimal.ZERO;
            }
            return; // We are in a position and haven't exited, so don't try to enter again
        }

        // Entry Rules Engine
        boolean conditionMet = false;
        
        if (">".equals(config.getEntryCondition())) {
            conditionMet = currentMockedIndicatorValue > config.getEntryThreshold();
        } else if ("<".equals(config.getEntryCondition())) {
            conditionMet = currentMockedIndicatorValue < config.getEntryThreshold();
        } else if ("==".equals(config.getEntryCondition())) {
            conditionMet = currentMockedIndicatorValue == config.getEntryThreshold();
        }

        if (conditionMet && !openPositions.contains(getSymbol())) {
            log.info("Entry Rule Met! Buying.");
            
            BigDecimal equity = broker.getAccountEquity();
            BigDecimal allocation = equity.multiply(new BigDecimal("0.10")); // 10% sizing
            
            int qtyToBuy = allocation.divide(currentPrice, 0, RoundingMode.DOWN).intValue();
            
            if (qtyToBuy > 0) {
                broker.placeMarketOrder(getSymbol(), qtyToBuy, "buy", getStrategyName());
                positionSize = qtyToBuy;
                entryPrice = currentPrice;
            }
        }
    }

    @Override
    public TradingStrategy createBacktestInstance() {
        return new DynamicRulesStrategy(config);
    }
}
