package com.quantplatform.core.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A persisted, hashed refresh token. The token sent to the client has the
 * form {@code selector.verifier}. The {@code selector} is stored in plaintext
 * (indexed) for O(1) lookup, while the {@code verifier} is BCrypt-hashed as
 * {@code tokenHash} so a database leak never exposes usable credentials.
 *
 * <p>Rotation is tracked via {@code replacedByTokenId} to detect reuse of a
 * revoked token, which is a strong signal of token theft.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Plaintext, non-secret identifier for O(1) indexed lookup. */
    @Column(name = "selector", nullable = false, unique = true)
    private String selector;

    /** BCrypt hash of the secret verifier portion of the token. */
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public RefreshToken(UUID userId, String selector, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.selector = selector;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }

    public void markReplacedBy(UUID newTokenId) {
        this.replacedByTokenId = newTokenId;
    }
}