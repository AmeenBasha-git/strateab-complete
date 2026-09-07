package com.quantplatform.core.strategy.engine;

import com.quantplatform.core.execution.broker.BrokerAdapter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A dummy offline strategy to demonstrate the Strategy Library.
 */
@Component
public class MeanReversionStrategy implements TradingStrategy {

    private boolean active = false;

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
        return Collections.emptyMap(); // Offline strategy, no live state
    }

    @Override
    public String getStrategyName() {
        return "MEAN_REVERSION_QQQ";
    }

    @Override
    public String getSymbol() {
        return "QQQ";
    }

    @Override
    public String getDescription() {
        return "A statistical mean-reversion algorithm designed to buy heavily oversold conditions and short heavily overbought conditions on the Nasdaq 100.";
    }

    @Override
    public List<String> getEntryRules() {
        return List.of(
            "Calculates the Relative Strength Index (RSI) using a 14-period lookback.",
            "Calculates Bollinger Bands (20-period SMA, 2 Standard Deviations).",
            "Goes LONG if RSI < 30 and price closes below the lower Bollinger Band.",
            "Goes SHORT if RSI > 70 and price closes above the upper Bollinger Band."
        );
    }

    @Override
    public List<String> getExitRules() {
        return List.of(
            "Take Profit: When price crosses the 20-period SMA (mean reversion target).",
            "Stop Loss: 1.5% hard stop from entry price to protect against trending moves.",
            "Time Stop: Closes all positions if held for more than 5 trading days."
        );
    }

    @Override
    public void evaluate(BrokerAdapter broker) {
        // This is an offline strategy. It is not registered with the ExecutionManager.
        // So this logic never runs.
    }

    @Override
    public TradingStrategy createBacktestInstance() {
        return new MeanReversionStrategy();
    }
}
