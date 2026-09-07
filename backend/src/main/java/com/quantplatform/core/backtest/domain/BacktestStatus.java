package com.quantplatform.core.backtest.domain;

/**
 * Lifecycle state of a backtest execution.
 */
public enum BacktestStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
