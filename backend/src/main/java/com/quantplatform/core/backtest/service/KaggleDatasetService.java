package com.quantplatform.core.backtest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class KaggleDatasetService {

    private static final Logger log = LoggerFactory.getLogger(KaggleDatasetService.class);
    private final String username;
    private final String key;
    private final String token;
    private final HttpClient httpClient;

    public KaggleDatasetService(
            @Value("${app.data.kaggle.username:}") String username,
            @Value("${app.data.kaggle.key:}") String key,
            @Value("${app.data.kaggle.token:}") String token) {
        this.username = username;
        this.key = key;
        this.token = token;
        // Follow normal redirects (e.g., 302 to Google Cloud Storage)
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private String extractDatasetId(String input) {
        if (input == null) return "";
        // If they pasted the python script snippet
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("kagglehub\\.dataset_download\\([\"']([^\"']+)[\"']\\)").matcher(input);
        if (m.find()) {
            return m.group(1);
        }
        // If they pasted the full kaggle URL
        m = java.util.regex.Pattern.compile("kaggle\\.com/datasets/([^/]+/[^/?#\\s]+)").matcher(input);
        if (m.find()) {
            return m.group(1);
        }
        // Otherwise, assume it's already a clean ID, just trim any accidental whitespace
        return input.trim();
    }

    public Path downloadAndExtractCsv(String rawDatasetId, Path targetDir, String targetFilenamePrefix) throws Exception {
        String datasetId = extractDatasetId(rawDatasetId);
        boolean hasBasicAuth = !username.isBlank() && !key.isBlank() && !"your_kaggle_username".equals(username);
        boolean hasTokenAuth = token != null && !token.isBlank();

        if (!hasBasicAuth && !hasTokenAuth) {
            throw new IllegalStateException("Kaggle credentials not configured in application.yml");
        }

        // Kaggle API format: GET https://www.kaggle.com/api/v1/datasets/download/{owner}/{dataset-name}
        URI uri = URI.create("https://www.kaggle.com/api/v1/datasets/download/" + datasetId);
        
        String authHeader;
        if (hasTokenAuth) {
            authHeader = "Bearer " + token;
        } else {
            authHeader = "Basic " + Base64.getEncoder().encodeToString((username + ":" + key).getBytes(StandardCharsets.UTF_8));
        }

        log.info("Downloading Kaggle dataset {} from {}", datasetId, uri);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", authHeader)
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Failed to download from Kaggle. HTTP " + response.statusCode() + ": " + body);
        }

        log.info("Download successful, extracting ZIP...");

        // Stream the ZIP and extract the first CSV file found
        try (ZipInputStream zis = new ZipInputStream(response.body())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".csv")) {
                    log.info("Found CSV entry in archive: {}", entry.getName());
                    
                    // Sanitize the entry name to prevent directory traversal just in case
                    String safeName = Path.of(entry.getName()).getFileName().toString();
                    Path targetFile = targetDir.resolve(targetFilenamePrefix + "_" + safeName);
                    
                    Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Extracted CSV to: {}", targetFile);
                    return targetFile; // We only extract the first CSV found
                }
            }
        }

        throw new RuntimeException("No .csv file found in the Kaggle dataset archive.");
    }
}
