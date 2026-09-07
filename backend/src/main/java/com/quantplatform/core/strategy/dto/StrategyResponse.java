package com.quantplatform.core.strategy.dto;

import com.quantplatform.core.strategy.domain.StrategyType;
import com.quantplatform.core.strategy.domain.StrategyVisibility;

import java.time.Instant;
import java.util.UUID;

public record StrategyResponse(
        UUID id,
        UUID ownerId,
        String name,
        String description,
        StrategyType strategyType,
        StrategyVisibility visibility,
        UUID activeVersionId,
        Instant createdAt,
        Instant updatedAt
) {}
