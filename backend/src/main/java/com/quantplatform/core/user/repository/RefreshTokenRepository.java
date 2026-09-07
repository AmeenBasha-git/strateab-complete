package com.quantplatform.core.user.repository;

import com.quantplatform.core.user.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findBySelector(String selector);

    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);
}