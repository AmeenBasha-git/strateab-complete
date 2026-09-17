package com.quantplatform.core.strategy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParseStrategyRequest(
        @NotBlank @Size(max = 5000) String description
) {}
