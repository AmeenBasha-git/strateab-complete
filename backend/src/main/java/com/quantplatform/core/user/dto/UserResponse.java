package com.quantplatform.core.user.dto;

import com.quantplatform.core.user.domain.RoleName;
import com.quantplatform.core.user.domain.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        UserStatus status,
        Set<RoleName> roles,
        Instant createdAt
) {}
