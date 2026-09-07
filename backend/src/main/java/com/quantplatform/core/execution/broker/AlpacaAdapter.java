package com.quantplatform.core.execution.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Connects to the Alpaca Paper Trading API using Spring's RestClient.
 */
@Service
public class AlpacaAdapter implements BrokerAdapter {

    private static final Logger log = LoggerFactory.getLogger(AlpacaAdapter.class);

    private final RestClient restClient;
    private final RestClient dataRestClient;

    public AlpacaAdapter(
            @Value("${app.broker.alpaca.key-id:}") String keyId,
            @Value("${app.broker.alpaca.secret-key:}") String secretKey,
            @Value("${app.broker.alpaca.base-url:https://paper-api.alpaca.markets}") String baseUrl) {
        
        this.restClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory())
                .baseUrl(baseUrl)
                .defaultHeader("APCA-API-KEY-ID", keyId)
                .defaultHeader("APCA-API-SECRET-KEY", secretKey)
                .build();
                
        this.dataRestClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory())
                .baseUrl("https://data.alpaca.markets")
                .defaultHeader("APCA-API-KEY-ID", keyId)
                .defaultHeader("APCA-API-SECRET-KEY", secretKey)
                .build();
    }

    @Override
    public String getBrokerName() {
        return "ALPACA";
    }

    @Override
    public boolean isMarketOpen() {
        try {
            // Alpaca Clock API: GET /v2/clock
            Map<String, Object> clock = restClient.get()
                    .uri("/v2/clock")
                    .retrieve()
                    .body(Map.class);
            
            if (clock != null && clock.containsKey("is_open")) {
                return (Boolean) clock.get("is_open");
            }
        } catch (Exception e) {
            log.error("Failed to check if Alpaca market is open: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public BigDecimal getCurrentPrice(String symbol) {
        try {
            log.info("Fetching real-time price for {} from Alpaca...", symbol);
            
            Map<String, Object> response = dataRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/trades/latest")
                        .queryParam("symbols", symbol)
                        .build())
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("trades")) {
                Map<String, Map<String, Object>> trades = (Map<String, Map<String, Object>>) response.get("trades");
                Map<String, Object> symbolTrade = trades.get(symbol);
                if (symbolTrade != null && symbolTrade.containsKey("p")) {
                    return new BigDecimal(symbolTrade.get("p").toString());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching price for {}: {}", symbol, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String placeMarketOrder(String symbol, int quantity, String side, String strategyName) {
        try {
            log.info("Placing {} market order for {} shares of {} on Alpaca (Strategy: {})", side.toUpperCase(), quantity, symbol, strategyName);
            
            // Limit client_order_id to 48 chars (Alpaca requirement)
            String clientOrderId = strategyName + "_" + System.currentTimeMillis();
            if (clientOrderId.length() > 48) {
                clientOrderId = clientOrderId.substring(0, 48);
            }
            
            Map<String, Object> request = Map.of(
                    "symbol", symbol,
                    "qty", quantity,
                    "side", side.toLowerCase(),
                    "type", "market",
                    "time_in_force", "day",
                    "client_order_id", clientOrderId
            );

            Map<String, Object> response = restClient.post()
                    .uri("/v2/orders")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("id")) {
                String orderId = (String) response.get("id");
                log.info("Order placed successfully! Order ID: {}", orderId);
                return orderId;
            }
        } catch (Exception e) {
            log.error("Failed to place order on Alpaca: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public java.util.List<Candle> getHistoricalBars(String symbol, String timeframe, int limit) {
        try {
            log.info("Fetching {} bars for {} from Alpaca...", limit, symbol);
            
            Map<String, Object> response = dataRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/v2/stocks/bars")
                        .queryParam("symbols", symbol)
                        .queryParam("timeframe", timeframe)
                        .queryParam("limit", limit)
                        .build())
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("bars")) {
                Map<String, java.util.List<Map<String, Object>>> barsMap = (Map<String, java.util.List<Map<String, Object>>>) response.get("bars");
                java.util.List<Map<String, Object>> symbolBars = barsMap.get(symbol);
                
                if (symbolBars != null) {
                    return symbolBars.stream()
                            .map(bar -> {
                                Candle candle = new Candle();
                                candle.setTimestamp(java.time.ZonedDateTime.parse(bar.get("t").toString()));
                                candle.setOpen(new BigDecimal(bar.get("o").toString()));
                                candle.setHigh(new BigDecimal(bar.get("h").toString()));
                                candle.setLow(new BigDecimal(bar.get("l").toString()));
                                candle.setClose(new BigDecimal(bar.get("c").toString()));
                                candle.setVolume(new BigDecimal(bar.get("v").toString()));
                                return candle;
                            })
                            .toList();
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch historical bars: {}", e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public BigDecimal getAccountEquity() {
        try {
            Map<String, Object> account = restClient.get()
                    .uri("/v2/account")
                    .retrieve()
                    .body(Map.class);
            
            if (account != null && account.containsKey("equity")) {
                return new BigDecimal(account.get("equity").toString());
            }
        } catch (Exception e) {
            log.error("Failed to fetch account equity: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public java.util.List<String> getOpenPositions() {
        try {
            java.util.List<Map<String, Object>> positions = restClient.get()
                    .uri("/v2/positions")
                    .retrieve()
                    .body(java.util.List.class);

            if (positions != null) {
                return positions.stream()
                        .map(p -> (String) p.get("symbol"))
                        .filter(java.util.Objects::nonNull)
                        .toList();
            }
        } catch (Exception e) {
            log.error("Failed to fetch open positions: {}", e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public void closePosition(String symbol) {
        try {
            log.info("Liquidating position for {}", symbol);
            restClient.delete()
                    .uri("/v2/positions/{symbol}", symbol)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully liquidated {}", symbol);
        } catch (Exception e) {
            log.error("Failed to close position for {}: {}", symbol, e.getMessage());
        }
    }
}
