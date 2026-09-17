package com.quantplatform.core.strategy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFromParsedRequest(
        @NotBlank @Size(max = 150) String name,
        String originalDescription,
        @NotBlank String symbol,
        @NotBlank String entryIndicator,
        @NotBlank String entryCondition,
        double entryThreshold,
        double takeProfitPercentage,
        double stopLossPercentage,
        String timeframe
) {}
