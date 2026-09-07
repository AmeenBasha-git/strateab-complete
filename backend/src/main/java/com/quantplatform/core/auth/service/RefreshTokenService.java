package com.quantplatform.core.auth.service;

import com.quantplatform.core.common.exception.InvalidTokenException;
import com.quantplatform.core.user.domain.RefreshToken;
import com.quantplatform.core.user.repository.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Manages refresh-token lifecycle using a selector/verifier split.
 *
 * <p>The raw token sent to the client has the form {@code selector.verifier}.
 * The selector is stored in plaintext (unique-indexed) for O(1) DB lookup;
 * the verifier is BCrypt-hashed so a database leak never exposes credentials.
 * This eliminates the previous O(n) full-table-scan BCrypt-match approach.
 */
@Service
public class RefreshTokenService {

    private static final long REFRESH_TTL_DAYS = 30;
    private static final int SELECTOR_BYTES = 16;
    private static final int VERIFIER_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder tokenHasher;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, PasswordEncoder tokenHasher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public String issue(UUID userId) {
        String selector = generateRandomBase64(SELECTOR_BYTES);
        String verifier = generateRandomBase64(VERIFIER_BYTES);

        RefreshToken entity = new RefreshToken(
                userId,
                selector,
                tokenHasher.encode(verifier),
                Instant.now().plusSeconds(REFRESH_TTL_DAYS * 86400)
        );
        refreshTokenRepository.save(entity);

        return selector + "." + verifier;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        SelectorVerifier sv = splitToken(rawToken);

        RefreshToken existing = refreshTokenRepository.findBySelector(sv.selector())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not recognized"));

        if (!tokenHasher.matches(sv.verifier(), existing.getTokenHash())) {
            throw new InvalidTokenException("Refresh token not recognized");
        }

        if (!existing.isUsable()) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }

        existing.revoke();
        String newRawToken = issue(existing.getUserId());
        refreshTokenRepository.save(existing);

        return new RotationResult(existing.getUserId(), newRawToken);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .forEach(RefreshToken::revoke);
    }

    private SelectorVerifier splitToken(String rawToken) {
        if (rawToken == null || !rawToken.contains(".")) {
            throw new InvalidTokenException("Malformed refresh token");
        }
        int dot = rawToken.indexOf('.');
        return new SelectorVerifier(rawToken.substring(0, dot), rawToken.substring(dot + 1));
    }

    private String generateRandomBase64(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record SelectorVerifier(String selector, String verifier) {}

    public record RotationResult(UUID userId, String newRefreshToken) {}
}

