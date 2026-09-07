package com.quantplatform.core.strategy.dto;

import java.time.Instant;
import java.util.UUID;

public record StrategyVersionResponse(
        UUID id,
        UUID strategyId,
        int versionNumber,
        String sourceCode,
        String parametersSchema,
        String changelogNote,
        UUID createdBy,
        Instant createdAt
) {}
