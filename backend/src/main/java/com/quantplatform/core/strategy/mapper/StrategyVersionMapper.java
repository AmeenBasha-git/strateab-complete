package com.quantplatform.core.strategy.mapper;

import com.quantplatform.core.strategy.domain.StrategyVersion;
import com.quantplatform.core.strategy.dto.StrategyVersionResponse;
import org.springframework.stereotype.Component;

@Component
public class StrategyVersionMapper {

    public StrategyVersionResponse toResponse(StrategyVersion version) {
        return new StrategyVersionResponse(
                version.getId(),
                version.getStrategyId(),
                version.getVersionNumber(),
                version.getSourceCode(),
                version.getParametersSchema(),
                version.getChangelogNote(),
                version.getCreatedBy(),
                version.getCreatedAt()
        );
    }
}
