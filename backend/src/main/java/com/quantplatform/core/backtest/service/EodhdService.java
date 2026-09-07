package com.quantplatform.core.backtest.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantplatform.core.backtest.service.DatasetValidationService.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EodhdService {

    private static final Logger log = LoggerFactory.getLogger(EodhdService.class);
    private static final int BATCH_SIZE = 5000;
    private static final ZoneId ET_ZONE = ZoneId.of("America/New_York");

    private static final Map<String, String> INTERVAL_MAP = Map.of(
            "1Min", "1m",
            "5Min", "5m",
            "15Min", "15m",
            "1Hour", "1h"
    );

    private final String apiToken;
    private final HttpClient httpClient;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EodhdService(
            @Value("${app.data.eodhd.api-token:}") String apiToken,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        this.apiToken = apiToken;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ValidationResult fetchAndImport(String symbol, String exchange, String timeframe,
                                           LocalDate fromDate, LocalDate toDate) throws Exception {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("EODHD API token not configured in application.yml (app.data.eodhd.api-token)");
        }

        String interval = INTERVAL_MAP.getOrDefault(timeframe, "5m");
        long fromUnix = fromDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long toUnix = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toEpochSecond() - 1;

        String ticker = symbol.toUpperCase() + "." + exchange.toUpperCase();
        String url = String.format(
                "https://eodhd.com/api/intraday/%s?api_token=%s&interval=%s&fmt=json&from=%d&to=%d",
                ticker, apiToken, interval, fromUnix, toUnix);

        log.info("Fetching EODHD intraday data: {} interval={} from={} to={}", ticker, interval, fromDate, toDate);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("EODHD API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        List<Map<String, Object>> bars = objectMapper.readValue(
                response.body(), new TypeReference<List<Map<String, Object>>>() {});

        if (bars == null || bars.isEmpty()) {
            return new ValidationResult(false, 0, null, null,
                    List.of("EODHD returned no data for " + ticker + " in the specified date range"));
        }

        log.info("EODHD returned {} bars for {}", bars.size(), ticker);

        String insertSql = "INSERT INTO historical_bars (symbol, timestamp_et, open, high, low, close, volume, vwap_rth, vwap_eth) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (symbol, timestamp_et) DO NOTHING";

        String upperSymbol = symbol.toUpperCase();
        List<Object[]> batchArgs = new ArrayList<>(BATCH_SIZE);
        int totalInserted = 0;
        LocalDate startDate = null;
        LocalDate endDate = null;
        List<String> warnings = new ArrayList<>();

        for (Map<String, Object> bar : bars) {
            try {
                long timestamp = toLong(bar.get("timestamp"));
                ZonedDateTime zdt = Instant.ofEpochSecond(timestamp).atZone(ET_ZONE);

                BigDecimal open = toBigDecimal(bar.get("open"));
                BigDecimal high = toBigDecimal(bar.get("high"));
                BigDecimal low = toBigDecimal(bar.get("low"));
                BigDecimal close = toBigDecimal(bar.get("close"));
                BigDecimal volume = toBigDecimal(bar.get("volume"));

                if (high.compareTo(low) < 0) {
                    warnings.add("Bar at " + zdt + ": High < Low");
                }

                LocalDate barDate = zdt.toLocalDate();
                if (startDate == null) startDate = barDate;
                endDate = barDate;

                batchArgs.add(new Object[]{
                        upperSymbol,
                        java.sql.Timestamp.from(zdt.toInstant()),
                        open, high, low, close, volume,
                        null, null
                });

                if (batchArgs.size() == BATCH_SIZE) {
                    executeBatch(insertSql, batchArgs);
                    totalInserted += batchArgs.size();
                    batchArgs.clear();
                    log.info("EODHD import: {} rows inserted...", totalInserted);
                }
            } catch (Exception e) {
                log.warn("Skipping malformed EODHD bar: {}", e.getMessage());
            }
        }

        if (!batchArgs.isEmpty()) {
            executeBatch(insertSql, batchArgs);
            totalInserted += batchArgs.size();
        }

        log.info("EODHD import complete: {} bars for {} ({} to {})", totalInserted, ticker, startDate, endDate);

        boolean valid = totalInserted >= 2;
        return new ValidationResult(valid, totalInserted, startDate, endDate, warnings);
    }

    private void executeBatch(String sql, List<Object[]> batchArgs) {
        transactionTemplate.execute(status -> {
            jdbcTemplate.batchUpdate(sql, batchArgs);
            return null;
        });
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        String s = value.toString().trim();
        if (s.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(s);
    }
}
