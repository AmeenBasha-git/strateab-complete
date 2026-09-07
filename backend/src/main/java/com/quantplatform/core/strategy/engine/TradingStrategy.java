package com.quantplatform.core.strategy.engine;

import com.quantplatform.core.execution.broker.BrokerAdapter;
import java.util.Map;

/**
 * Strategy interface. Each automated strategy must implement this interface.
 */
public interface TradingStrategy {

    /**
     * Unique name of the strategy (e.g., "VWAP_BREAKOUT", "SMA_CROSSOVER")
     */
    String getStrategyName();

    /**
     * The ticker symbol this strategy is monitoring (e.g., "AAPL", "BTC/USD")
     */
    String getSymbol();

    /**
     * A high-level description of what this algorithmic strategy does.
     */
    String getDescription();

    /**
     * A list of bullet points defining the quantitative entry criteria.
     */
    java.util.List<String> getEntryRules();

    /**
     * A list of bullet points defining the quantitative exit criteria (Take Profit, Stop Loss).
     */
    java.util.List<String> getExitRules();

    /**
     * Executes the strategy logic. This is called periodically by the Execution Manager.
     * @param broker The broker adapter to fetch data and place orders.
     */
    void evaluate(BrokerAdapter broker);

    /**
     * Returns true if the strategy is currently active (running).
     */
    boolean isActive();

    /**
     * Sets the active state of the strategy.
     */
    void setActive(boolean active);

    /**
     * Returns a map of live state variables (e.g. current positions, prices, dynamic metrics) for the UI dashboard.
     */
    Map<String, Object> getLiveState();

    /**
     * Creates a fresh, isolated instance of this strategy for backtesting.
     * The returned instance must have no shared mutable state with the live singleton.
     * Trade persistence and external data fetches may be no-ops in the backtest copy.
     */
    TradingStrategy createBacktestInstance();

    /**
     * Override the symbol this strategy operates on. Used by the backtest engine
     * to point the strategy at the dataset's symbol.
     */
    default void setSymbol(String symbol) {
        // no-op by default; strategies that support backtesting should override
    }
}
