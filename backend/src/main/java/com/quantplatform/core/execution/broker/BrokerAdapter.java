package com.quantplatform.core.execution.broker;

import java.math.BigDecimal;

/**
 * Adapter interface for interacting with different brokerages (Alpaca, Zerodha, etc).
 */
public interface BrokerAdapter {
    
    /**
     * Unique name of the broker (e.g., "ALPACA", "ZERODHA")
     */
    String getBrokerName();

    /**
     * Checks if the market is currently open for trading.
     */
    boolean isMarketOpen();

    /**
     * Fetches the current live price for a given ticker symbol.
     * @param symbol The ticker symbol (e.g., "AAPL", "BTC/USD")
     * @return The current price.
     */
    BigDecimal getCurrentPrice(String symbol);

    /**
     * Places a market order for the specified symbol.
     * @param symbol The ticker symbol.
     * @param quantity Number of shares/coins.
     * @param side "buy" or "sell".
     * @return Order ID from the broker.
     */
    /**
     * Places a market order and tags it with the strategy name for virtual portfolio tracking.
     * @param symbol The ticker symbol.
     * @param quantity Number of shares.
     * @param side "buy" or "sell".
     * @param strategyName Name of the strategy placing the order.
     * @return Order ID from the broker.
     */
    String placeMarketOrder(String symbol, int quantity, String side, String strategyName);

    /**
     * Fetches historical candlestick bars for a symbol.
     * @param symbol The ticker symbol.
     * @param timeframe The timeframe (e.g., "1Min", "5Min", "1Day").
     * @param limit Number of bars to return.
     * @return A list of Candle objects.
     */
    java.util.List<Candle> getHistoricalBars(String symbol, String timeframe, int limit);

    /**
     * Gets the total current equity of the account for risk calculations.
     * @return Total account equity.
     */
    BigDecimal getAccountEquity();

    /**
     * Gets a list of symbols for currently open positions.
     * @return List of ticker symbols currently held.
     */
    java.util.List<String> getOpenPositions();

    /**
     * Closes the entire position for a specific symbol (Liquidate).
     * @param symbol The ticker symbol.
     */
    void closePosition(String symbol);
}
