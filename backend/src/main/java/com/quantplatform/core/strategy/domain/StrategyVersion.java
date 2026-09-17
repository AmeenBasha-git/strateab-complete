package com.quantplatform.core.strategy.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable snapshot of a strategy's code and parameters at a point in
 * time. Never updated after creation — this is what makes past backtests
 * and deployments reproducible, since they reference a specific version id
 * rather than "whatever the strategy looks like today".
 */
@Entity
@Table(name = "strategy_versions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"strategy_id", "version_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StrategyVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "strategy_id", nullable = false)
    private UUID strategyId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Lob
    @Column(name = "source_code", nullable = false)
    private String sourceCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters_schema", columnDefinition = "jsonb")
    private String parametersSchema;

    @Column(name = "changelog_note", length = 1000)
    private String changelogNote;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public StrategyVersion(
            UUID strategyId,
            int versionNumber,
            String sourceCode,
            String parametersSchema,
            String changelogNote,
            UUID createdBy
    ) {
        this.strategyId = strategyId;
        this.versionNumber = versionNumber;
        this.sourceCode = sourceCode;
        this.parametersSchema = parametersSchema;
        this.changelogNote = changelogNote;
        this.createdBy = createdBy;
    }
}
