package com.quantplatform.core.strategy.dto;

import com.quantplatform.core.strategy.domain.StrategyVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStrategyRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @NotNull StrategyVisibility visibility
) {}
