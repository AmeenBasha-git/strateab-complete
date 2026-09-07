package com.quantplatform.core.execution.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trade_records")
public class TradeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String strategyName;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String direction; // "LONG" or "SHORT"

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal entryPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal exitPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal pnl; // Profit & Loss amount

    @Column(nullable = false)
    private LocalDateTime executionTime;
}
