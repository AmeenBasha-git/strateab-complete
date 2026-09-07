package com.quantplatform.core.strategy.model;

/**
 * Entity representing a user-defined strategy configuration from the No-Code Builder.
 * In a real database, this would be an @Entity with @Id.
 */
public class UserStrategyConfig {

    private String id;
    private String strategyName;
    private String symbol;
    private String entryIndicator; // e.g. "RSI"
    private String entryCondition; // e.g. ">"
    private double entryThreshold; // e.g. 70.0
    private double takeProfitPercentage; // e.g. 0.02 (2%)
    private double stopLossPercentage; // e.g. 0.01 (1%)

    public UserStrategyConfig() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getEntryIndicator() {
        return entryIndicator;
    }

    public void setEntryIndicator(String entryIndicator) {
        this.entryIndicator = entryIndicator;
    }

    public String getEntryCondition() {
        return entryCondition;
    }

    public void setEntryCondition(String entryCondition) {
        this.entryCondition = entryCondition;
    }

    public double getEntryThreshold() {
        return entryThreshold;
    }

    public void setEntryThreshold(double entryThreshold) {
        this.entryThreshold = entryThreshold;
    }

    public double getTakeProfitPercentage() {
        return takeProfitPercentage;
    }

    public void setTakeProfitPercentage(double takeProfitPercentage) {
        this.takeProfitPercentage = takeProfitPercentage;
    }

    public double getStopLossPercentage() {
        return stopLossPercentage;
    }

    public void setStopLossPercentage(double stopLossPercentage) {
        this.stopLossPercentage = stopLossPercentage;
    }
}
