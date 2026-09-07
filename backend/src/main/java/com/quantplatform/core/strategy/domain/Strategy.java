package com.quantplatform.core.strategy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Stable identity of a strategy. Holds metadata only — the actual runnable
 * code lives in {@link StrategyVersion}. This entity rarely changes; every
 * code edit produces a new version instead of mutating this row.
 */
@Entity
@Table(name = "strategies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Null for PLATFORM strategies — they belong to the platform, not a user. */
    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false)
    private StrategyType strategyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StrategyVisibility visibility;

    @Column(name = "active_version_id")
    private UUID activeVersionId;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    public static Strategy createUserStrategy(UUID ownerId, String name, String description) {
        Strategy strategy = new Strategy();
        strategy.ownerId = ownerId;
        strategy.name = name;
        strategy.description = description;
        strategy.strategyType = StrategyType.USER;
        strategy.visibility = StrategyVisibility.PRIVATE;
        return strategy;
    }

    public static Strategy createPlatformStrategy(String name, String description) {
        Strategy strategy = new Strategy();
        strategy.name = name;
        strategy.description = description;
        strategy.strategyType = StrategyType.PLATFORM;
        strategy.visibility = StrategyVisibility.PUBLIC;
        return strategy;
    }

    public void updateMetadata(String name, String description, StrategyVisibility visibility) {
        this.name = name;
        this.description = description;
        this.visibility = visibility;
    }

    public void setActiveVersion(UUID versionId) {
        this.activeVersionId = versionId;
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public boolean isPlatformStrategy() {
        return strategyType == StrategyType.PLATFORM;
    }
}
