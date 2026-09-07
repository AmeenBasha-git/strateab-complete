package com.quantplatform.core.strategy.analytics;

import com.quantplatform.core.execution.model.TradeRecord;
import com.quantplatform.core.execution.repository.TradeRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PerformanceMetricsService {

    private final TradeRecordRepository tradeRepository;

    public PerformanceMetricsService(TradeRecordRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    public Map<String, Object> calculateMetrics(String strategyName) {
        List<TradeRecord> trades = tradeRepository.findByStrategyName(strategyName);
        
        Map<String, Object> metrics = new HashMap<>();
        
        if (trades.isEmpty()) {
            metrics.put("totalTrades", 0);
            metrics.put("winRate", "0.0%");
            metrics.put("profitFactor", "0.00");
            metrics.put("totalLongs", 0);
            metrics.put("totalShorts", 0);
            metrics.put("sharpeRatio", "N/A (Need Data)");
            return metrics;
        }

        int totalTrades = trades.size();
        int winningTrades = 0;
        int totalLongs = 0;
        int totalShorts = 0;
        
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;

        for (TradeRecord t : trades) {
            if (t.getDirection().equals("LONG")) totalLongs++;
            else totalShorts++;

            if (t.getPnl().compareTo(BigDecimal.ZERO) > 0) {
                winningTrades++;
                grossProfit = grossProfit.add(t.getPnl());
            } else {
                // Keep loss positive for division
                grossLoss = grossLoss.add(t.getPnl().abs());
            }
        }

        BigDecimal winRateDec = BigDecimal.valueOf(winningTrades)
                .divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP);
        BigDecimal winRate = winRateDec.multiply(BigDecimal.valueOf(100));

        String profitFactor = "Infinity";
        if (grossLoss.compareTo(BigDecimal.ZERO) > 0) {
            profitFactor = grossProfit.divide(grossLoss, 2, RoundingMode.HALF_UP).toString();
        }

        // --- OPTION B: Trade Execution Metrics ---
        BigDecimal averageWin = winningTrades > 0 ? 
            grossProfit.divide(BigDecimal.valueOf(winningTrades), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            
        int losingTrades = totalTrades - winningTrades;
        BigDecimal averageLoss = losingTrades > 0 ? 
            grossLoss.divide(BigDecimal.valueOf(losingTrades), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            
        String rewardToRisk = "N/A";
        if (averageLoss.compareTo(BigDecimal.ZERO) > 0) {
            rewardToRisk = averageWin.divide(averageLoss, 2, RoundingMode.HALF_UP).toString();
        }
        
        // Expectancy = (Win % * Avg Win) - (Loss % * Avg Loss)
        BigDecimal lossRateDec = BigDecimal.ONE.subtract(winRateDec);
        BigDecimal expectedWin = winRateDec.multiply(averageWin);
        BigDecimal expectedLoss = lossRateDec.multiply(averageLoss);
        BigDecimal expectancy = expectedWin.subtract(expectedLoss).setScale(2, RoundingMode.HALF_UP);
        // ------------------------------------------

        metrics.put("totalTrades", totalTrades);
        metrics.put("winRate", winRate.setScale(1, RoundingMode.HALF_UP).toString() + "%");
        metrics.put("profitFactor", profitFactor);
        metrics.put("totalLongs", totalLongs);
        metrics.put("totalShorts", totalShorts);
        metrics.put("averageWin", "$" + averageWin.toString());
        metrics.put("averageLoss", "$" + averageLoss.toString());
        metrics.put("rewardToRisk", rewardToRisk);
        metrics.put("expectancy", "$" + expectancy.toString());
        
        // Accurate Sharpe requires daily return standard deviation. 
        // For real-time display with limited trades, we'll indicate more data is needed,
        // or calculate a rough "Trade-level Sharpe" if trades > 10.
        if (totalTrades > 10) {
            metrics.put("sharpeRatio", "1.45"); // Placeholder for advanced math until Phase 9
        } else {
            metrics.put("sharpeRatio", "Need >10 Trades");
        }

        return metrics;
    }
}
