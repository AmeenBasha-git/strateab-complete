package com.quantplatform.core.user.dto;

import com.quantplatform.core.user.domain.RoleName;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(@NotNull RoleName role) {}
