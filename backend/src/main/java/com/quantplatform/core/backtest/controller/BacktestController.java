package com.quantplatform.core.backtest.controller;

import com.quantplatform.core.backtest.domain.BacktestDataset;
import com.quantplatform.core.backtest.domain.BacktestRun;
import com.quantplatform.core.backtest.dto.RunBacktestRequest;
import com.quantplatform.core.backtest.repository.BacktestDatasetRepository;
import com.quantplatform.core.backtest.repository.BacktestRunRepository;
import com.quantplatform.core.backtest.service.BacktestManager;
import com.quantplatform.core.backtest.service.CsvParserService;
import com.quantplatform.core.backtest.service.DatasetValidationService;
import com.quantplatform.core.common.exception.ResourceNotFoundException;
import com.quantplatform.core.common.response.ApiResponse;
import com.quantplatform.core.execution.broker.Candle;
import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.engine.StrategyManager;
import com.quantplatform.core.strategy.engine.TradingStrategy;
import com.quantplatform.core.strategy.repository.StrategyRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@RequestMapping("/api/v1/backtests")
@CrossOrigin(origins = "*")
@Tag(name = "Backtesting", description = "Dataset upload, backtest execution, and results")
public class BacktestController {

    private static final Logger log = LoggerFactory.getLogger(BacktestController.class);

    private final BacktestDatasetRepository datasetRepository;
    private final BacktestRunRepository runRepository;
    private final CsvParserService csvParserService;
    private final DatasetValidationService validationService;
    private final BacktestManager backtestManager;
    private final StrategyManager strategyManager;
    private final StrategyRepository strategyRepository;
    private final com.quantplatform.core.backtest.service.KaggleDatasetService kaggleDatasetService;
    private final com.quantplatform.core.backtest.service.EodhdService eodhdService;
    private final Path uploadDir;

    public BacktestController(BacktestDatasetRepository datasetRepository,
                              BacktestRunRepository runRepository,
                              CsvParserService csvParserService,
                              DatasetValidationService validationService,
                              BacktestManager backtestManager,
                              StrategyManager strategyManager,
                              StrategyRepository strategyRepository,
                              com.quantplatform.core.backtest.service.KaggleDatasetService kaggleDatasetService,
                              com.quantplatform.core.backtest.service.EodhdService eodhdService,
                              @Value("${app.backtest.upload-dir:./backtest-data}") String uploadDir) {
        this.datasetRepository = datasetRepository;
        this.runRepository = runRepository;
        this.csvParserService = csvParserService;
        this.validationService = validationService;
        this.backtestManager = backtestManager;
        this.strategyManager = strategyManager;
        this.strategyRepository = strategyRepository;
        this.kaggleDatasetService = kaggleDatasetService;
        this.eodhdService = eodhdService;
        this.uploadDir = Path.of(uploadDir);
    }

    @PostMapping("/datasets/import-url")
    public ApiResponse<Map<String, Object>> importDatasetFromUrl(
            @RequestBody com.quantplatform.core.backtest.dto.ImportDatasetRequest request,
            @AuthenticationPrincipal UUID authUserId
    ) {
        UUID userId = authUserId != null ? authUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        try {
            // Create upload directory if needed
            Files.createDirectories(uploadDir);

            Path filePath;

            DatasetValidationService.ValidationResult validation;

            if ("EODHD".equalsIgnoreCase(request.sourceType())) {
                String exchange = request.exchange() != null ? request.exchange() : "US";
                if (request.fromDate() == null || request.toDate() == null) {
                    return ApiResponse.error("fromDate and toDate are required for EODHD imports", List.of());
                }
                java.time.LocalDate from = java.time.LocalDate.parse(request.fromDate());
                java.time.LocalDate to = java.time.LocalDate.parse(request.toDate());
                validation = eodhdService.fetchAndImport(request.symbol(), exchange, request.timeframe(), from, to);
                filePath = null;
            } else {
                if ("LOCAL".equalsIgnoreCase(request.sourceType())) {
                    filePath = java.nio.file.Path.of(request.sourceUri());
                    if (!Files.exists(filePath)) {
                        return ApiResponse.error("File not found: " + request.sourceUri(), List.of());
                    }
                } else if ("KAGGLE".equalsIgnoreCase(request.sourceType())) {
                    String prefix = UUID.randomUUID() + "_" + request.symbol() + "_" + request.timeframe();
                    filePath = kaggleDatasetService.downloadAndExtractCsv(request.sourceUri(), uploadDir, prefix);
                } else {
                    String filename = UUID.randomUUID() + "_" + request.symbol() + "_" + request.timeframe() + ".csv";
                    filePath = uploadDir.resolve(filename);
                    log.info("Starting download from URL: {}", request.sourceUri());
                    try (java.io.InputStream in = java.net.URI.create(request.sourceUri()).toURL().openStream()) {
                        Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    log.info("Download completed to: {}", filePath);
                }
                validation = csvParserService.parseAndImport(filePath, request.symbol().toUpperCase());
            }

            if (!validation.valid()) {
                if (filePath != null) Files.deleteIfExists(filePath);
                return ApiResponse.error("Dataset validation failed", validation.warnings());
            }

            // Save metadata
            BacktestDataset dataset = new BacktestDataset(
                    request.name(), request.symbol().toUpperCase(), request.timeframe(),
                    validation.startDate(), validation.endDate(),
                    validation.totalBars(), filePath != null ? filePath.toString() : "eodhd:" + request.symbol(), userId
            );
            datasetRepository.save(dataset);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", dataset.getId());
            result.put("name", dataset.getName());
            result.put("symbol", dataset.getSymbol());
            result.put("totalBars", validation.totalBars());
            result.put("startDate", validation.startDate());
            result.put("endDate", validation.endDate());
            result.put("warnings", validation.warnings());

            return ApiResponse.success(result, "Dataset downloaded and validated");

        } catch (Exception e) {
            log.error("Failed to import dataset from URL: {}", e.getMessage());
            return ApiResponse.error("Import failed: " + e.getMessage(), List.of(e.getMessage()));
        }
    }

    @GetMapping("/datasets")
    public ApiResponse<List<BacktestDataset>> listDatasets(@AuthenticationPrincipal UUID authUserId) {
        UUID userId = authUserId != null ? authUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        return ApiResponse.success(
                datasetRepository.findByUploadedByOrderByCreatedAtDesc(userId),
                "OK"
        );
    }

    @PostMapping("/run")
    public ApiResponse<BacktestRun> runBacktest(
            @RequestBody RunBacktestRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        UUID resolvedUserId = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");

        BacktestRun run = backtestManager.execute(
                request.datasetId(), request.strategyName(),
                request.startingEquity(), resolvedUserId,
                request.slippageBps(), request.commissionPerTrade()
        );

        String message = switch (run.getStatus()) {
            case COMPLETED -> "Backtest completed successfully";
            case FAILED -> "Backtest failed: " + run.getErrorMessage();
            default -> "Backtest status: " + run.getStatus();
        };

        return ApiResponse.success(run, message);
    }

    @GetMapping("/runs")
    public ApiResponse<List<BacktestRun>> listRuns(@AuthenticationPrincipal UUID authUserId) {
        UUID userId = authUserId != null ? authUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        return ApiResponse.success(
                runRepository.findByRunByOrderByCreatedAtDesc(userId),
                "OK"
        );
    }

    @GetMapping("/runs/{id}")
    public ApiResponse<BacktestRun> getRunDetail(@PathVariable UUID id) {
        BacktestRun run = runRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Backtest run not found: " + id));
        return ApiResponse.success(run, "OK");
    }

    @GetMapping("/runs/count")
    public ApiResponse<Long> getRunCount(@AuthenticationPrincipal UUID authUserId) {
        UUID userId = authUserId != null ? authUserId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        return ApiResponse.success(runRepository.countByRunBy(userId), "OK");
    }

    @GetMapping("/strategies")
    public ApiResponse<List<Map<String, String>>> availableStrategies(
            @AuthenticationPrincipal UUID userId
    ) {
        List<Map<String, String>> strategies = new ArrayList<>(strategyManager.getAllStrategies().stream()
                .map(s -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("name", s.getStrategyName());
                    entry.put("symbol", s.getSymbol());
                    entry.put("description", s.getDescription());
                    entry.put("type", "PLATFORM");
                    return entry;
                })
                .toList());

        if (userId != null) {
            List<Strategy> userStrategies = strategyRepository.findByOwnerIdAndDeletedFalse(userId);
            for (Strategy s : userStrategies) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("name", s.getName());
                entry.put("symbol", "USER");
                entry.put("description", s.getDescription() != null ? s.getDescription() : "User strategy");
                entry.put("type", "USER");
                strategies.add(entry);
            }
        }

        return ApiResponse.success(strategies, "OK");
    }
}
