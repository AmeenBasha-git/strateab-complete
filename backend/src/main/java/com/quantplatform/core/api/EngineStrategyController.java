package com.quantplatform.core.api;

import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.engine.TradingStrategy;
import com.quantplatform.core.strategy.engine.StrategyManager;
import com.quantplatform.core.strategy.analytics.PerformanceMetricsService;
import com.quantplatform.core.strategy.repository.StrategyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/strategies")
@CrossOrigin(origins = "http://localhost:5173")
public class EngineStrategyController {

    private final StrategyManager strategyManager;
    private final PerformanceMetricsService metricsService;
    private final StrategyRepository strategyRepository;

    public EngineStrategyController(StrategyManager strategyManager,
                                    PerformanceMetricsService metricsService,
                                    StrategyRepository strategyRepository) {
        this.strategyManager = strategyManager;
        this.metricsService = metricsService;
        this.strategyRepository = strategyRepository;
    }

    @GetMapping("/library")
    public List<Map<String, Object>> getStrategyLibrary(@AuthenticationPrincipal UUID userId) {
        List<Map<String, Object>> library = new ArrayList<>();
        for (TradingStrategy strategy : strategyManager.getAllStrategies()) {
            library.add(buildStrategyProfile(strategy));
        }

        if (userId != null) {
            List<Strategy> userStrategies = strategyRepository.findByOwnerIdAndDeletedFalse(userId);
            for (Strategy s : userStrategies) {
                Map<String, Object> profile = new HashMap<>();
                profile.put("strategyName", s.getName());
                profile.put("symbol", "");
                profile.put("description", s.getDescription() != null ? s.getDescription() : "");
                profile.put("entryRules", List.of());
                profile.put("exitRules", List.of());
                profile.put("status", "OFFLINE");
                profile.put("type", "User Strategy");
                profile.put("version", "v1.0.0");
                profile.put("visibility", "Private");
                profile.put("createdDate", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
                profile.put("strategyType", "USER");
                library.add(profile);
            }
        }

        return library;
    }

    @GetMapping("/{name}")
    public ResponseEntity<Map<String, Object>> getStrategyDetails(@PathVariable String name) {
        for (TradingStrategy strategy : strategyManager.getAllStrategies()) {
            if (strategy.getStrategyName().equals(name)) {
                Map<String, Object> profile = buildStrategyProfile(strategy);
                
                // If this is an active strategy, append live execution state and metrics
                if (strategy.isActive()) {
                    profile.put("metrics", metricsService.calculateMetrics(strategy.getStrategyName()));
                    
                    Map<String, Object> liveState = strategy.getLiveState();
                    // Map generic keys to what frontend expects for state object
                    if (!liveState.containsKey("isMarketOpen")) {
                        liveState.put("isMarketOpen", true); // Mocked for simplicity
                    }
                    profile.put("state", liveState);
                    
                    // Live Math values (pulled directly into profile to support legacy UI mappings)
                    if (liveState.containsKey("currentPrice")) profile.put("currentPrice", liveState.get("currentPrice"));
                    if (liveState.containsKey("vwap")) profile.put("vwap", liveState.get("vwap"));
                    if (liveState.containsKey("upperBand")) profile.put("upperBand", liveState.get("upperBand"));
                    if (liveState.containsKey("lowerBand")) profile.put("lowerBand", liveState.get("lowerBand"));
                }
                
                return ResponseEntity.ok(profile);
            }
        }
        return ResponseEntity.notFound().build();
    }

    private Map<String, Object> buildStrategyProfile(TradingStrategy strategy) {
        Map<String, Object> profile = new HashMap<>();
        
        profile.put("strategyName", strategy.getStrategyName());
        profile.put("symbol", strategy.getSymbol());
        profile.put("description", strategy.getDescription());
        profile.put("entryRules", strategy.getEntryRules());
        profile.put("exitRules", strategy.getExitRules());
        
        profile.put("status", strategy.isActive() ? "ACTIVE" : "OFFLINE");
        
        profile.put("type", strategy.getStrategyName().contains("MOMENTUM") ? "Momentum" : "Fundamental GARP");
        profile.put("version", "v1.2.0");
        profile.put("visibility", "Private");
        profile.put("createdDate", LocalDate.now().minusMonths(2).toString());
        
        return profile;
    }
}
