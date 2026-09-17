package com.quantplatform.core.strategy.engine;

import com.quantplatform.core.data.fundamental.FundamentalDataAdapter;
import com.quantplatform.core.data.fundamental.FundamentalMetrics;
import com.quantplatform.core.execution.broker.BrokerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GarpStrategy implements TradingStrategy {

    private static final Logger log = LoggerFactory.getLogger(GarpStrategy.class);

    private final FundamentalDataAdapter fundamentalData;
    
    private boolean active = false;
    
    // State variables to replicate QuantConnect quarterly rebalance
    private LocalDate lastRebalanceDate = null;
    private List<String> portfolioTargets = new ArrayList<>();

    public GarpStrategy(FundamentalDataAdapter fundamentalData) {
        this.fundamentalData = fundamentalData;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public Map<String, Object> getLiveState() {
        Map<String, Object> state = new HashMap<>();
        state.put("position", portfolioTargets.isEmpty() ? "NO_POSITION" : "LONG");
        state.put("isKillSwitchTriggered", false);
        state.put("portfolioTargets", portfolioTargets);
        state.put("lastRebalanceDate", lastRebalanceDate);
        return state;
    }

    @Override
    public String getStrategyName() {
        return "GARP_STRATEGY";
    }

    @Override
    public String getSymbol() {
        return "MULTIPLE"; // Operates on a universe of stocks
    }

    @Override
    public String getDescription() {
        return "Growth at a Reasonable Price (GARP). Filters 1000 liquid US stocks, selects top 20% by ROIC, and buys the top 2 cheapest by EV/EBITDA. Rebalances quarterly.";
    }

    @Override
    public List<String> getEntryRules() {
        return List.of(
            "Price > $5 and highly liquid",
            "Top 20% highest Return on Invested Capital (ROIC)",
            "Top 2 lowest Enterprise Value to EBITDA (EV/EBITDA)"
        );
    }

    @Override
    public List<String> getExitRules() {
        return List.of(
            "Rebalance Quarterly: Liquidate if stock falls out of top 2 GARP targets"
        );
    }

    @Override
    public void evaluate(BrokerAdapter broker) {
        if (!active) {
            return;
        }

        if (!broker.isMarketOpen()) {
            return;
        }

        LocalDate today = LocalDate.now();

        // Check if we need to rebalance (First run, or 3 months have passed since last rebalance)
        if (lastRebalanceDate == null || today.isAfter(lastRebalanceDate.plusMonths(3).minusDays(1))) {
            log.info("Starting Quarterly Rebalance for GARP Strategy...");
            
            // 1. Coarse Selection: Get Top 1000 Liquid US Stocks over $5
            List<String> universe = fundamentalData.getLiquidUniverse(5.0, 1000);
            if (universe.isEmpty()) {
                log.warn("Failed to fetch universe. Skipping rebalance.");
                return;
            }

            // 2. Fine Selection: Get Fundamental Metrics
            Map<String, FundamentalMetrics> metricsMap = fundamentalData.getFundamentalMetrics(universe);
            
            // Filter out companies missing ROIC or EV/EBITDA
            List<FundamentalMetrics> validMetrics = metricsMap.values().stream()
                    .filter(m -> m.getRoic() != null && m.getEvToEbitda() != null)
                    .collect(Collectors.toList());

            // STEP 1: THE QUALITY CHECK (Sort by ROIC Highest to Lowest)
            validMetrics.sort((a, b) -> b.getRoic().compareTo(a.getRoic()));
            
            int top20PercentCount = (int) (validMetrics.size() * 0.2);
            if (top20PercentCount == 0) {
                log.warn("Not enough valid companies for GARP strategy.");
                return;
            }
            
            List<FundamentalMetrics> topQuality = validMetrics.subList(0, top20PercentCount);

            // STEP 2: THE PRICE CHECK (Sort by EV/EBITDA Lowest to Highest)
            topQuality.sort(Comparator.comparing(FundamentalMetrics::getEvToEbitda));
            
            // Take absolute cheapest 2 stocks
            int targetCount = Math.min(2, topQuality.size());
            portfolioTargets = topQuality.subList(0, targetCount).stream()
                    .map(FundamentalMetrics::getSymbol)
                    .collect(Collectors.toList());
                    
            log.info("New GARP Target Portfolio: {}", portfolioTargets);

            // 3. Execution - Liquidate stocks no longer in target
            List<String> currentPositions = broker.getOpenPositions();
            for (String heldSymbol : currentPositions) {
                if (!portfolioTargets.contains(heldSymbol)) {
                    broker.closePosition(heldSymbol);
                }
            }

            // 4. Execution - Buy target stocks (10% allocation per stock for backtesting safety)
            BigDecimal totalEquity = broker.getAccountEquity();
            if (totalEquity.compareTo(BigDecimal.ZERO) > 0 && !portfolioTargets.isEmpty()) {
                BigDecimal allocationPerStock = totalEquity.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
                
                for (String targetSymbol : portfolioTargets) {
                    if (!currentPositions.contains(targetSymbol)) {
                        BigDecimal currentPrice = broker.getCurrentPrice(targetSymbol);
                        if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                            // Calculate quantity: (10% Equity) / Current Price
                            int qty = allocationPerStock.divide(currentPrice, 0, RoundingMode.DOWN).intValue();
                            if (qty > 0) {
                                broker.placeMarketOrder(targetSymbol, qty, "buy", getStrategyName());
                            }
                        }
                    }
                }
            }
            
            lastRebalanceDate = today;
            log.info("Quarterly Rebalance Complete.");
        }
    }

    @Override
    public TradingStrategy createBacktestInstance() {
        return new GarpStrategy(fundamentalData);
    }
}
