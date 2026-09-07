package com.quantplatform.core.strategy.mapper;

import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.dto.StrategyResponse;
import com.quantplatform.core.strategy.dto.StrategySummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class StrategyMapper {

    public StrategyResponse toResponse(Strategy strategy) {
        return new StrategyResponse(
                strategy.getId(),
                strategy.getOwnerId(),
                strategy.getName(),
                strategy.getDescription(),
                strategy.getStrategyType(),
                strategy.getVisibility(),
                strategy.getActiveVersionId(),
                strategy.getCreatedAt(),
                strategy.getUpdatedAt()
        );
    }

    public StrategySummaryResponse toSummary(Strategy strategy) {
        return new StrategySummaryResponse(
                strategy.getId(),
                strategy.getName(),
                strategy.getStrategyType(),
                strategy.getVisibility(),
                strategy.getUpdatedAt()
        );
    }
}
