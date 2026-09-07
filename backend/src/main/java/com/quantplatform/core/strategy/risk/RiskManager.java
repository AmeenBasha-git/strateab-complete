package com.quantplatform.core.strategy.risk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared utility service to calculate exact position sizes based on a strategy's risk profile.
 */
@Service
public class RiskManager {

    private static final Logger log = LoggerFactory.getLogger(RiskManager.class);

    /**
     * Calculates the number of shares to buy without exceeding the maximum risk per trade.
     *
     * @param accountEquity The total cash/equity in the account (e.g. $100,000)
     * @param maxRiskPercent The maximum % of the account to risk on this trade (e.g. 0.02 for 2%)
     * @param entryPrice The price you are buying the asset at (e.g. $150.00)
     * @param stopLossPrice The price you will sell if the trade goes against you (e.g. $145.00)
     * @return The integer number of shares to buy.
     */
    public int calculatePositionSize(BigDecimal accountEquity, BigDecimal maxRiskPercent, BigDecimal entryPrice, BigDecimal stopLossPrice) {
        if (entryPrice.compareTo(stopLossPrice) <= 0) {
            log.error("RiskManager: Entry price must be strictly greater than Stop Loss price for long positions.");
            return 0;
        }

        // 1. Calculate max dollar amount we are allowed to lose (e.g. $100,000 * 0.02 = $2,000)
        BigDecimal maxRiskDollars = accountEquity.multiply(maxRiskPercent);

        // 2. Calculate the dollar risk per single share (e.g. $150 - $145 = $5)
        BigDecimal riskPerShare = entryPrice.subtract(stopLossPrice);

        // 3. Divide max loss by risk per share (e.g. $2,000 / $5 = 400 shares)
        BigDecimal numShares = maxRiskDollars.divide(riskPerShare, 0, RoundingMode.DOWN);

        log.info("RiskManager calculation: Equity=${}, Risk=${}, Entry=${}, SL=${}. Recommended Shares: {}", 
                accountEquity, maxRiskDollars, entryPrice, stopLossPrice, numShares);

        return numShares.intValue();
    }
}
