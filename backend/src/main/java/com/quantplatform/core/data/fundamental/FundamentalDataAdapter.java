package com.quantplatform.core.data.fundamental;

import java.util.List;
import java.util.Map;

/**
 * Interface for fetching fundamental data like ROIC and EV/EBITDA,
 * as well as screening the universe of tradeable symbols.
 */
public interface FundamentalDataAdapter {

    /**
     * Gets a list of liquid symbols matching criteria.
     * @param minPrice Minimum stock price (e.g., $5).
     * @param limit Maximum number of symbols to return (sorted by volume).
     * @return List of ticker symbols.
     */
    List<String> getLiquidUniverse(double minPrice, int limit);

    /**
     * Fetches fundamental metrics for a list of symbols.
     * @param symbols List of symbols.
     * @return Map of symbol to FundamentalMetrics.
     */
    Map<String, FundamentalMetrics> getFundamentalMetrics(List<String> symbols);
}
