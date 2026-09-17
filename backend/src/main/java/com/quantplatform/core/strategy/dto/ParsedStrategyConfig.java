package com.quantplatform.core.strategy.dto;

public record ParsedStrategyConfig(
        String symbol,
        String entryIndicator,
        String entryCondition,
        double entryThreshold,
        double takeProfitPercentage,
        double stopLossPercentage,
        String timeframe,
        String description
) {}
