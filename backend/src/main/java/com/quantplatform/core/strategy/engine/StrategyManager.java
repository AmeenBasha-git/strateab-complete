package com.quantplatform.core.strategy.engine;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Central manager for all trading strategies in the system.
 */
@Service
public class StrategyManager {

    private static final Logger log = LoggerFactory.getLogger(StrategyManager.class);
    private final List<TradingStrategy> allStrategies;

    public StrategyManager(List<TradingStrategy> allStrategies) {
        this.allStrategies = allStrategies;
    }

    @PostConstruct
    public void init() {
        log.info("StrategyManager initialized with {} strategies.", allStrategies.size());
        
        // For demonstration, set INTRADAY_MOMENTUM_SPY as active initially so it shows on the dashboard
        // just like before, but now driven by the Manager.
        for (TradingStrategy strategy : allStrategies) {
            if (strategy.getStrategyName().equals("INTRADAY_MOMENTUM_SPY")) {
                strategy.setActive(true);
            }
        }
    }

    public List<TradingStrategy> getAllStrategies() {
        return allStrategies;
    }

    public List<TradingStrategy> getActiveStrategies() {
        return allStrategies.stream()
                .filter(TradingStrategy::isActive)
                .collect(Collectors.toList());
    }

    public void setStrategyActive(String strategyName, boolean active) {
        for (TradingStrategy strategy : allStrategies) {
            if (strategy.getStrategyName().equals(strategyName)) {
                strategy.setActive(active);
                log.info("Strategy {} active state set to {}", strategyName, active);
                return;
            }
        }
        log.warn("Attempted to toggle unknown strategy: {}", strategyName);
    }
}
