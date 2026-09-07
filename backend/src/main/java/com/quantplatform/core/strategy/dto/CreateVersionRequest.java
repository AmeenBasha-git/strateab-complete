package com.quantplatform.core.strategy.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVersionRequest(
        @NotBlank String sourceCode,
        String parametersSchema,
        String changelogNote
) {}
