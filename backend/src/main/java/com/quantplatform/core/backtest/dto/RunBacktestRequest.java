package com.quantplatform.core.backtest.dto;

import java.math.BigDecimal;

public record RunBacktestRequest(
        java.util.UUID datasetId,
        String strategyName,
        BigDecimal startingEquity,
        BigDecimal slippageBps,
        BigDecimal commissionPerTrade
) {
    public BigDecimal slippageBps() {
        return slippageBps != null ? slippageBps : new BigDecimal("5");
    }

    public BigDecimal commissionPerTrade() {
        return commissionPerTrade != null ? commissionPerTrade : new BigDecimal("1.00");
    }
}
