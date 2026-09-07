package com.quantplatform.core.backtest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Metadata for an uploaded historical dataset. The raw CSV data is stored
 * on the filesystem; this entity only tracks what was uploaded and its
 * validated properties (symbol, date range, bar count).
 */
@Entity
@Table(name = "backtest_datasets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BacktestDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String timeframe;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_bars", nullable = false)
    private int totalBars;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public BacktestDataset(String name, String symbol, String timeframe,
                           LocalDate startDate, LocalDate endDate,
                           int totalBars, String filePath, UUID uploadedBy) {
        this.name = name;
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalBars = totalBars;
        this.filePath = filePath;
        this.uploadedBy = uploadedBy;
    }
}
