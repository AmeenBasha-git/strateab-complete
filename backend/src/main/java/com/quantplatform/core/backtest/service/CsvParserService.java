package com.quantplatform.core.backtest.service;

import com.quantplatform.core.backtest.service.DatasetValidationService.ValidationResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CsvParserService {

    private static final Logger log = LoggerFactory.getLogger(CsvParserService.class);
    private static final int BATCH_SIZE = 5000;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/New_York");

    private static final Map<String, Set<String>> COLUMN_ALIASES = Map.of(
            "timestamp", Set.of("timestamp", "datetime", "timestamp et", "timestamp_et", "date/time"),
            "date_only", Set.of("date"),
            "time_only", Set.of("time"),
            "open",      Set.of("open", "o"),
            "high",      Set.of("high", "h"),
            "low",       Set.of("low", "l"),
            "close",     Set.of("close", "c", "adj close", "adj_close", "adjclose"),
            "volume",    Set.of("volume", "vol", "v"),
            "vwap_rth",  Set.of("vwap_rth", "vwap rth"),
            "vwap_eth",  Set.of("vwap_eth", "vwap eth")
    );

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSXXX"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm"),
            DateTimeFormatter.ofPattern("M/d/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yy H:mm"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
    };

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public CsvParserService(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ValidationResult parseAndImport(Path filePath, String symbol) throws IOException {
        List<String> warnings = new ArrayList<>();
        int lineNum = 1;
        int totalInserted = 0;
        int gapCount = 0;
        int zeroCount = 0;
        ZonedDateTime prevTimestamp = null;
        LocalDate startDate = null;
        LocalDate endDate = null;

        String insertSql = "INSERT INTO historical_bars (symbol, timestamp_et, open, high, low, close, volume, vwap_rth, vwap_eth) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (symbol, timestamp_et) DO NOTHING";

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .setAllowMissingColumnNames(true)
                .build();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            // Strip BOM if present
            reader.mark(1);
            int firstChar = reader.read();
            if (firstChar != '﻿' && firstChar != -1) {
                reader.reset();
            }

            CSVParser parser = new CSVParser(reader, format);
            Map<String, Integer> headerMap = parser.getHeaderMap();

            int tsIdx = resolveColumnOptional(headerMap, "timestamp");
            int dateIdx = resolveColumnOptional(headerMap, "date_only");
            int timeIdx = resolveColumnOptional(headerMap, "time_only");
            boolean splitDateTime = tsIdx < 0 && dateIdx >= 0;
            if (tsIdx < 0 && dateIdx < 0) {
                throw new IllegalArgumentException("Missing timestamp column (looked for: timestamp, datetime, date, date/time; found headers: " + headerMap.keySet() + ")");
            }
            if (tsIdx < 0) {
                tsIdx = dateIdx;
            }
            int openIdx = resolveColumn(headerMap, "open");
            int highIdx = resolveColumn(headerMap, "high");
            int lowIdx = resolveColumn(headerMap, "low");
            int closeIdx = resolveColumn(headerMap, "close");
            int volIdx = resolveColumnOptional(headerMap, "volume");
            int vwapRthIdx = resolveColumnOptional(headerMap, "vwap_rth");
            int vwapEthIdx = resolveColumnOptional(headerMap, "vwap_eth");

            List<Object[]> batchArgs = new ArrayList<>(BATCH_SIZE);

            for (CSVRecord record : parser) {
                lineNum++;

                try {
                    String rawTs = record.get(tsIdx);
                    if (splitDateTime && timeIdx >= 0) {
                        rawTs = rawTs.trim() + " " + record.get(timeIdx).trim();
                    }
                    ZonedDateTime timestamp = parseTimestamp(rawTs);
                    BigDecimal open = new BigDecimal(record.get(openIdx));
                    BigDecimal high = new BigDecimal(record.get(highIdx));
                    BigDecimal low = new BigDecimal(record.get(lowIdx));
                    BigDecimal close = new BigDecimal(record.get(closeIdx));
                    BigDecimal volume = volIdx >= 0 ? parseBigDecimalSafe(record.get(volIdx)) : BigDecimal.ZERO;
                    BigDecimal vwapRth = vwapRthIdx >= 0 ? parseBigDecimalOrNull(record.get(vwapRthIdx)) : null;
                    BigDecimal vwapEth = vwapEthIdx >= 0 ? parseBigDecimalOrNull(record.get(vwapEthIdx)) : null;

                    if (isZeroOrNull(open) || isZeroOrNull(high) || isZeroOrNull(low) || isZeroOrNull(close)) {
                        zeroCount++;
                    }
                    if (high.compareTo(low) < 0) {
                        warnings.add("Row " + lineNum + ": High < Low");
                    }
                    if (prevTimestamp != null && !timestamp.isAfter(prevTimestamp)) {
                        gapCount++;
                    }
                    if (timestamp.toLocalDate().isAfter(LocalDate.now())) {
                        warnings.add("Row " + lineNum + ": Future date detected");
                    }

                    if (startDate == null) startDate = timestamp.toLocalDate();
                    endDate = timestamp.toLocalDate();
                    prevTimestamp = timestamp;

                    batchArgs.add(new Object[]{symbol, java.sql.Timestamp.from(timestamp.toInstant()), open, high, low, close, volume, vwapRth, vwapEth});

                    if (batchArgs.size() == BATCH_SIZE) {
                        executeBatch(insertSql, batchArgs);
                        totalInserted += batchArgs.size();
                        batchArgs.clear();
                        log.info("Imported {} rows...", totalInserted);
                    }

                } catch (Exception e) {
                    log.warn("Skipping malformed row {} in {}: {}", lineNum, filePath.getFileName(), e.getMessage());
                }
            }

            if (!batchArgs.isEmpty()) {
                executeBatch(insertSql, batchArgs);
                totalInserted += batchArgs.size();
                log.info("Imported {} rows total.", totalInserted);
            }
        }

        if (zeroCount > 0) {
            warnings.add(zeroCount + " rows have zero/null OHLC values");
        }
        if (gapCount > 0) {
            warnings.add(gapCount + " rows are not chronologically ordered correctly");
        }

        boolean valid = totalInserted >= 2;
        log.info("Dataset import complete: {} bars inserted, {} to {}, {} warnings", totalInserted, startDate, endDate, warnings.size());

        return new ValidationResult(valid, totalInserted, startDate, endDate, warnings);
    }

    private int resolveColumn(Map<String, Integer> headerMap, String canonicalName) {
        Set<String> aliases = COLUMN_ALIASES.getOrDefault(canonicalName, Set.of(canonicalName));
        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
            String headerNormalized = entry.getKey().trim().toLowerCase().replace("\"", "");
            if (aliases.contains(headerNormalized)) {
                return entry.getValue();
            }
        }
        if (canonicalName.equals("volume")) return -1;
        throw new IllegalArgumentException("Missing required column: " + canonicalName +
                " (looked for: " + aliases + ", found headers: " + headerMap.keySet() + ")");
    }

    private int resolveColumnOptional(Map<String, Integer> headerMap, String canonicalName) {
        Set<String> aliases = COLUMN_ALIASES.getOrDefault(canonicalName, Set.of(canonicalName));
        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
            String headerNormalized = entry.getKey().trim().toLowerCase().replace("\"", "");
            if (aliases.contains(headerNormalized)) {
                return entry.getValue();
            }
        }
        return -1;
    }

    private void executeBatch(String sql, List<Object[]> batchArgs) {
        transactionTemplate.execute(status -> {
            jdbcTemplate.batchUpdate(sql, batchArgs);
            return null;
        });
    }

    private boolean isZeroOrNull(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private BigDecimal parseBigDecimalSafe(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseBigDecimalOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ZonedDateTime parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Empty timestamp");
        }
        raw = raw.trim();

        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return ZonedDateTime.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {}

            try {
                LocalDateTime ldt = LocalDateTime.parse(raw, fmt);
                return ldt.atZone(DEFAULT_ZONE);
            } catch (DateTimeParseException ignored) {}

            try {
                LocalDate ld = LocalDate.parse(raw, fmt);
                return ld.atStartOfDay(DEFAULT_ZONE);
            } catch (DateTimeParseException ignored) {}
        }

        // Unix epoch seconds or millis
        try {
            long epoch = Long.parseLong(raw);
            if (epoch > 1_000_000_000_000L) {
                return java.time.Instant.ofEpochMilli(epoch).atZone(DEFAULT_ZONE);
            } else {
                return java.time.Instant.ofEpochSecond(epoch).atZone(DEFAULT_ZONE);
            }
        } catch (NumberFormatException ignored) {}

        throw new IllegalArgumentException("Unparseable timestamp: " + raw);
    }
}
