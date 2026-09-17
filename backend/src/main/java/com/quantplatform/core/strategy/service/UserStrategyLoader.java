package com.quantplatform.core.strategy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.domain.StrategyVersion;
import com.quantplatform.core.strategy.engine.DynamicRulesStrategy;
import com.quantplatform.core.strategy.engine.TradingStrategy;
import com.quantplatform.core.strategy.model.UserStrategyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserStrategyLoader {

    private static final Logger log = LoggerFactory.getLogger(UserStrategyLoader.class);

    private final StrategyVersionService versionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserStrategyLoader(StrategyVersionService versionService) {
        this.versionService = versionService;
    }

    public TradingStrategy load(Strategy strategy) {
        if (strategy.getActiveVersionId() == null) {
            throw new IllegalStateException("Strategy has no active version: " + strategy.getName());
        }

        StrategyVersion version = versionService.getById(strategy.getActiveVersionId());
        String configJson = version.getParametersSchema();

        if (configJson == null || configJson.isBlank()) {
            throw new IllegalStateException("Strategy version has no parameters config: " + strategy.getName());
        }

        try {
            JsonNode node = objectMapper.readTree(configJson);

            UserStrategyConfig config = new UserStrategyConfig();
            config.setId(strategy.getId().toString());
            config.setStrategyName(strategy.getName());
            config.setSymbol(node.path("symbol").asText("SPY"));
            config.setEntryIndicator(node.path("entryIndicator").asText("RSI"));
            config.setEntryCondition(node.path("entryCondition").asText("<"));
            config.setEntryThreshold(node.path("entryThreshold").asDouble(30.0));
            config.setTakeProfitPercentage(node.path("takeProfitPercentage").asDouble(0.05));
            config.setStopLossPercentage(node.path("stopLossPercentage").asDouble(0.02));

            return new DynamicRulesStrategy(config);

        } catch (Exception e) {
            log.error("Failed to load user strategy '{}': {}", strategy.getName(), e.getMessage());
            throw new RuntimeException("Failed to load strategy config: " + e.getMessage(), e);
        }
    }
}
