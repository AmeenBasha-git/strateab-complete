package com.quantplatform.core.backtest.dto;

public record ImportDatasetRequest(
        String name,
        String symbol,
        String timeframe,
        String sourceType,
        String sourceUri,
        String exchange,
        String fromDate,
        String toDate
) {}
