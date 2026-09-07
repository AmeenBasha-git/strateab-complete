package com.quantplatform.core.strategy.dto;

import com.quantplatform.core.strategy.domain.StrategyType;
import com.quantplatform.core.strategy.domain.StrategyVisibility;

import java.time.Instant;
import java.util.UUID;

/** Lightweight listing shape — omits source code to keep list responses cheap. */
public record StrategySummaryResponse(
        UUID id,
        String name,
        StrategyType strategyType,
        StrategyVisibility visibility,
        Instant updatedAt
) {}
