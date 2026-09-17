package com.quantplatform.core.strategy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantplatform.core.strategy.dto.ParsedStrategyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class StrategyParsingService {

    private static final Logger log = LoggerFactory.getLogger(StrategyParsingService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    private static final String SYSTEM_PROMPT = """
            You are a trading strategy parser. Given a natural language description of a trading strategy, \
            extract structured parameters as JSON.

            Return ONLY a valid JSON object with these fields:
            {
              "symbol": "Stock ticker (e.g. SPY, AAPL, TSLA). Default: SPY",
              "entryIndicator": "One of: RSI, MACD, SMA, EMA, VWAP, PRICE, BOLLINGER",
              "entryCondition": "One of: > , < , ==",
              "entryThreshold": numeric value,
              "takeProfitPercentage": decimal (e.g. 0.03 for 3%). Default: 0.05,
              "stopLossPercentage": decimal (e.g. 0.02 for 2%). Default: 0.02,
              "timeframe": "One of: 1m, 5m, 15m, 1h, 4h, 1d. Default: 1d",
              "description": "One-sentence summary of the strategy"
            }

            Mapping rules:
            - "drops below", "falls under", "is less than", "below" -> condition "<"
            - "goes above", "rises above", "is greater than", "above", "exceeds" -> condition ">"
            - "equals", "hits", "reaches exactly" -> condition "=="
            - RSI values are typically 0-100
            - Percentages like "3%" become 0.03, "1.5%" becomes 0.015
            - If take profit or stop loss not mentioned, use defaults (5% TP, 2% SL)
            - If symbol not mentioned, default to SPY
            - Return ONLY the JSON object, no markdown fences, no explanation
            """;

    public StrategyParsingService(
            @Value("${app.gemini.api-key:}") String apiKey
    ) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public ParsedStrategyConfig parse(String description) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key not configured. Set app.gemini.api-key in application.yml");
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                    ),
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", description))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "maxOutputTokens", 1024
                    )
            );

            String responseJson = restClient.post()
                    .uri("/v1beta/models/gemini-3.6-flash:generateContent?key={key}", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            String text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            String cleanJson = text.strip();
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            JsonNode parsed = objectMapper.readTree(cleanJson);

            return new ParsedStrategyConfig(
                    parsed.path("symbol").asText("SPY"),
                    parsed.path("entryIndicator").asText("RSI"),
                    parsed.path("entryCondition").asText("<"),
                    parsed.path("entryThreshold").asDouble(30.0),
                    parsed.path("takeProfitPercentage").asDouble(0.05),
                    parsed.path("stopLossPercentage").asDouble(0.02),
                    parsed.path("timeframe").asText("1d"),
                    parsed.path("description").asText(description)
            );

        } catch (Exception e) {
            log.error("Failed to parse strategy description: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse strategy description: " + e.getMessage(), e);
        }
    }
}
