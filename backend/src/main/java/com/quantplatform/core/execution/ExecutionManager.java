package com.quantplatform.core.execution;

import com.quantplatform.core.execution.broker.BrokerAdapter;
import com.quantplatform.core.strategy.engine.TradingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The core engine that orchestrates running multiple strategies simultaneously.
 */
@Service
public class ExecutionManager {

    private static final Logger log = LoggerFactory.getLogger(ExecutionManager.class);

    private final List<TradingStrategy> strategies;
    private final Map<String, BrokerAdapter> brokers;

    public ExecutionManager(List<TradingStrategy> strategies, List<BrokerAdapter> brokerAdapters) {
        this.strategies = strategies;
        // Group brokers by their unique name (e.g. "ALPACA", "ZERODHA")
        this.brokers = brokerAdapters.stream()
                .collect(Collectors.toMap(BrokerAdapter::getBrokerName, broker -> broker));
    }

    /**
     * This job runs every 1 minute.
     * It checks if the market is open and then evaluates all active strategies.
     */
    @Scheduled(fixedRateString = "${app.execution.interval-ms:60000}")
    public void runStrategies() {
        log.info("Execution Engine: Waking up to evaluate {} strategies...", strategies.size());

        // For now, default to Alpaca paper broker for testing
        BrokerAdapter defaultBroker = brokers.get("ALPACA");
        if (defaultBroker == null) {
            log.warn("Alpaca broker adapter not found! Skipping execution.");
            return;
        }

        if (!defaultBroker.isMarketOpen()) {
            log.info("Market is currently closed. Skipping evaluation.");
            // In a real scenario, we might still want to evaluate crypto strategies 24/7
            // return; // Commented out for testing purposes
        }

        for (TradingStrategy strategy : strategies) {
            log.info("Evaluating Strategy: {} for {}", strategy.getStrategyName(), strategy.getSymbol());
            try {
                // Here we pass the broker to the strategy so it can fetch data and trade
                strategy.evaluate(defaultBroker);
            } catch (Exception e) {
                log.error("Error evaluating strategy {}: {}", strategy.getStrategyName(), e.getMessage());
            }
        }
        
        log.info("Execution Engine: Evaluation cycle complete.");
    }
}
