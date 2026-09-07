package com.quantplatform.core.backtest.service;

import com.quantplatform.core.execution.broker.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates a parsed list of candles for data quality issues before
 * allowing a backtest to run against them.
 */
@Service
public class DatasetValidationService {

    private static final Logger log = LoggerFactory.getLogger(DatasetValidationService.class);

    public ValidationResult validate(List<Candle> candles) {
        List<String> warnings = new ArrayList<>();

        if (candles.isEmpty()) {
            return new ValidationResult(false, 0, null, null, List.of("Dataset contains no valid rows"));
        }

        int gapCount = 0;
        int zeroCount = 0;
        ZonedDateTime prevTimestamp = null;

        for (int i = 0; i < candles.size(); i++) {
            Candle c = candles.get(i);

            // Check for zero/null OHLCV values
            if (isZeroOrNull(c.getOpen()) || isZeroOrNull(c.getHigh()) ||
                isZeroOrNull(c.getLow()) || isZeroOrNull(c.getClose())) {
                zeroCount++;
            }

            // Check high >= low
            if (c.getHigh().compareTo(c.getLow()) < 0) {
                warnings.add("Row " + (i + 1) + ": High < Low (" + c.getHigh() + " < " + c.getLow() + ")");
            }

            // Check chronological ordering (already sorted by parser, but verify)
            if (prevTimestamp != null && !c.getTimestamp().isAfter(prevTimestamp)) {
                warnings.add("Row " + (i + 1) + ": Timestamp not strictly increasing");
                gapCount++;
            }

            // Check for future dates
            if (c.getTimestamp().toLocalDate().isAfter(LocalDate.now())) {
                warnings.add("Row " + (i + 1) + ": Future date detected (" + c.getTimestamp().toLocalDate() + ")");
            }

            prevTimestamp = c.getTimestamp();
        }

        if (zeroCount > 0) {
            warnings.add(zeroCount + " rows have zero/null OHLC values");
        }

        LocalDate startDate = candles.getFirst().getTimestamp().toLocalDate();
        LocalDate endDate = candles.getLast().getTimestamp().toLocalDate();

        // Consider valid if we have at least 2 bars and no critical errors
        boolean valid = candles.size() >= 2;

        log.info("Dataset validation: {} bars, {} to {}, {} warnings",
                candles.size(), startDate, endDate, warnings.size());

        return new ValidationResult(valid, candles.size(), startDate, endDate, warnings);
    }

    private boolean isZeroOrNull(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    public record ValidationResult(
            boolean valid,
            int totalBars,
            LocalDate startDate,
            LocalDate endDate,
            List<String> warnings
    ) {}
}
