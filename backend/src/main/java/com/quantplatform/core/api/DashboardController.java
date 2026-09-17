package com.quantplatform.core.api;

import com.quantplatform.core.execution.broker.BrokerAdapter;
import com.quantplatform.core.strategy.engine.StrategyManager;
import com.quantplatform.core.strategy.engine.TradingStrategy;
import com.quantplatform.core.strategy.analytics.PerformanceMetricsService;
import com.quantplatform.core.execution.model.TradeRecord;
import com.quantplatform.core.execution.repository.TradeRecordRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:5173") // Allow React Frontend to connect
public class DashboardController {

    private final BrokerAdapter brokerAdapter;
    private final StrategyManager strategyManager;
    private final PerformanceMetricsService metricsService;
    private final TradeRecordRepository tradeRepository;

    public DashboardController(BrokerAdapter brokerAdapter, StrategyManager strategyManager, 
                               PerformanceMetricsService metricsService, TradeRecordRepository tradeRepository) {
        this.brokerAdapter = brokerAdapter;
        this.strategyManager = strategyManager;
        this.metricsService = metricsService;
        this.tradeRepository = tradeRepository;
    }

    @GetMapping("/trades")
    public List<TradeRecord> getRecentTrades() {
        return tradeRepository.findAll(Sort.by(Sort.Direction.DESC, "executionTime"));
    }

    @GetMapping("/status")
    public List<Map<String, Object>> getDashboardStatus() {
        List<Map<String, Object>> statuses = new ArrayList<>();
        
        BigDecimal accountEquity = BigDecimal.ZERO;
        try {
            accountEquity = brokerAdapter.getAccountEquity();
        } catch (Exception e) {
            // Fallback to zero
        }
        
        List<TradingStrategy> allStrategies = strategyManager.getAllStrategies();

        for (TradingStrategy strategy : allStrategies) {
            Map<String, Object> status = new HashMap<>();
            
            // 1. Account Data
            status.put("accountEquity", accountEquity);

            // 2. Strategy Data
            status.put("strategyName", strategy.getStrategyName());
            status.put("symbol", strategy.getSymbol());
            status.put("isActive", strategy.isActive());
            status.put("description", strategy.getDescription());
            
            // Include dynamic state directly from the strategy
            Map<String, Object> liveState = strategy.getLiveState();
            status.putAll(liveState);
            
            // Backwards compatibility for frontend mappings if not in liveState
            if (!status.containsKey("positionState") && status.containsKey("position")) {
                status.put("positionState", status.get("position"));
            }
            
            // Fetch live price from broker directly so it updates even when market is closed
            if (!strategy.getSymbol().equals("MULTIPLE")) {
                try {
                    BigDecimal livePrice = brokerAdapter.getCurrentPrice(strategy.getSymbol());
                    if (livePrice != null && livePrice.compareTo(BigDecimal.ZERO) > 0) {
                        status.put("currentPrice", livePrice);
                    }
                } catch (Exception e) {
                    // Broker unavailable — keep whatever price the strategy already has
                }
            }

            // 3. Performance Metrics
            Map<String, Object> metrics = metricsService.calculateMetrics(strategy.getStrategyName());
            status.put("metrics", metrics);
            
            statuses.add(status);
        }

        return statuses;
    }
}
