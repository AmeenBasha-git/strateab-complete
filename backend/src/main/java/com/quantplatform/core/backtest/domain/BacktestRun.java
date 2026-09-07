package com.quantplatform.core.backtest.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records the result of a single backtest execution. Created in PENDING state,
 * updated to COMPLETED or FAILED once the backtest loop finishes. All P&L
 * metrics are computed from the trades generated during the run.
 */
@Entity
@Table(name = "backtest_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BacktestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    @Column(name = "strategy_name", nullable = false)
    private String strategyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BacktestStatus status;

    @Column(name = "starting_equity", nullable = false, precision = 19, scale = 4)
    private BigDecimal startingEquity;

    @Column(name = "final_equity", precision = 19, scale = 4)
    private BigDecimal finalEquity;

    @Column(name = "total_trades")
    private int totalTrades;

    @Column(name = "win_rate", precision = 5, scale = 2)
    private BigDecimal winRate;

    @Column(name = "profit_factor", precision = 10, scale = 4)
    private BigDecimal profitFactor;

    @Column(name = "total_pnl", precision = 19, scale = 4)
    private BigDecimal totalPnl;

    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown;

    // Risk-adjusted ratios
    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "sortino_ratio", precision = 10, scale = 4)
    private BigDecimal sortinoRatio;

    @Column(name = "calmar_ratio", precision = 10, scale = 4)
    private BigDecimal calmarRatio;

    // Return metrics
    @Column(name = "annualized_return", precision = 10, scale = 4)
    private BigDecimal annualizedReturn;

    @Column(name = "annualized_volatility", precision = 10, scale = 4)
    private BigDecimal annualizedVolatility;

    // Trade statistics
    @Column(name = "avg_win", precision = 19, scale = 4)
    private BigDecimal avgWin;

    @Column(name = "avg_loss", precision = 19, scale = 4)
    private BigDecimal avgLoss;

    @Column(name = "largest_win", precision = 19, scale = 4)
    private BigDecimal largestWin;

    @Column(name = "largest_loss", precision = 19, scale = 4)
    private BigDecimal largestLoss;

    @Column(name = "expectancy", precision = 19, scale = 4)
    private BigDecimal expectancy;

    @Column(name = "risk_reward_ratio", precision = 10, scale = 4)
    private BigDecimal riskRewardRatio;

    @Column(name = "max_consecutive_wins")
    private Integer maxConsecutiveWins;

    @Column(name = "max_consecutive_losses")
    private Integer maxConsecutiveLosses;

    // Directional breakdown
    @Column(name = "long_trades")
    private Integer longTrades;

    @Column(name = "short_trades")
    private Integer shortTrades;

    @Column(name = "long_win_rate", precision = 5, scale = 2)
    private BigDecimal longWinRate;

    @Column(name = "short_win_rate", precision = 5, scale = 2)
    private BigDecimal shortWinRate;

    // Cost tracking
    @Column(name = "total_commission", precision = 19, scale = 4)
    private BigDecimal totalCommission;

    // Equity curve for charting
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "equity_curve", columnDefinition = "jsonb")
    private List<Map<String, Object>> equityCurve;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "run_by", nullable = false)
    private UUID runBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static BacktestRun create(UUID datasetId, String strategyName,
                                     BigDecimal startingEquity, UUID runBy) {
        BacktestRun run = new BacktestRun();
        run.datasetId = datasetId;
        run.strategyName = strategyName;
        run.startingEquity = startingEquity;
        run.status = BacktestStatus.PENDING;
        run.runBy = runBy;
        return run;
    }

    public void markCompleted(BigDecimal finalEquity, int totalTrades,
                              BigDecimal winRate, BigDecimal profitFactor,
                              BigDecimal totalPnl, BigDecimal maxDrawdown) {
        this.status = BacktestStatus.COMPLETED;
        this.finalEquity = finalEquity;
        this.totalTrades = totalTrades;
        this.winRate = winRate;
        this.profitFactor = profitFactor;
        this.totalPnl = totalPnl;
        this.maxDrawdown = maxDrawdown;
        this.completedAt = Instant.now();
    }

    public void setExtendedMetrics(BigDecimal sharpeRatio, BigDecimal sortinoRatio, BigDecimal calmarRatio,
                                   BigDecimal annualizedReturn, BigDecimal annualizedVolatility,
                                   BigDecimal avgWin, BigDecimal avgLoss,
                                   BigDecimal largestWin, BigDecimal largestLoss,
                                   BigDecimal expectancy, BigDecimal riskRewardRatio,
                                   int maxConsecutiveWins, int maxConsecutiveLosses,
                                   int longTrades, int shortTrades,
                                   BigDecimal longWinRate, BigDecimal shortWinRate,
                                   BigDecimal totalCommission,
                                   List<Map<String, Object>> equityCurve) {
        this.sharpeRatio = sharpeRatio;
        this.sortinoRatio = sortinoRatio;
        this.calmarRatio = calmarRatio;
        this.annualizedReturn = annualizedReturn;
        this.annualizedVolatility = annualizedVolatility;
        this.avgWin = avgWin;
        this.avgLoss = avgLoss;
        this.largestWin = largestWin;
        this.largestLoss = largestLoss;
        this.expectancy = expectancy;
        this.riskRewardRatio = riskRewardRatio;
        this.maxConsecutiveWins = maxConsecutiveWins;
        this.maxConsecutiveLosses = maxConsecutiveLosses;
        this.longTrades = longTrades;
        this.shortTrades = shortTrades;
        this.longWinRate = longWinRate;
        this.shortWinRate = shortWinRate;
        this.totalCommission = totalCommission;
        this.equityCurve = equityCurve;
    }

    public void markFailed(String errorMessage) {
        this.status = BacktestStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }
}
