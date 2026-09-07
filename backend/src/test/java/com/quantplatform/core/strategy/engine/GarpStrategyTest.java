package com.quantplatform.core.strategy.engine;

import com.quantplatform.core.data.fundamental.FundamentalDataAdapter;
import com.quantplatform.core.data.fundamental.FundamentalMetrics;
import com.quantplatform.core.execution.broker.BrokerAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

public class GarpStrategyTest {

    @Test
    public void testGarpStrategyEvaluation() {
        FundamentalDataAdapter dataAdapter = mock(FundamentalDataAdapter.class);
        BrokerAdapter broker = mock(BrokerAdapter.class);

        // Mock Broker
        when(broker.isMarketOpen()).thenReturn(true);
        when(broker.getAccountEquity()).thenReturn(new BigDecimal("100000"));
        when(broker.getOpenPositions()).thenReturn(Arrays.asList());
        when(broker.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("150"));
        when(broker.getCurrentPrice("MSFT")).thenReturn(new BigDecimal("300"));

        // Mock Fundamental Data
        List<String> universe = Arrays.asList("AAPL", "MSFT", "TSLA", "AMZN");
        when(dataAdapter.getLiquidUniverse(anyDouble(), anyInt())).thenReturn(universe);

        Map<String, FundamentalMetrics> metricsMap = new HashMap<>();
        
        // AAPL: High ROIC, Low EV/EBITDA (Great GARP target)
        metricsMap.put("AAPL", new FundamentalMetrics("AAPL", new BigDecimal("0.25"), new BigDecimal("10.5")));
        // MSFT: Good ROIC, Low EV/EBITDA (Great GARP target)
        metricsMap.put("MSFT", new FundamentalMetrics("MSFT", new BigDecimal("0.20"), new BigDecimal("12.0")));
        // TSLA: OK ROIC, Very High EV/EBITDA (Overpriced)
        metricsMap.put("TSLA", new FundamentalMetrics("TSLA", new BigDecimal("0.15"), new BigDecimal("80.0")));
        // AMZN: Low ROIC, OK EV/EBITDA
        metricsMap.put("AMZN", new FundamentalMetrics("AMZN", new BigDecimal("0.05"), new BigDecimal("15.0")));
        // NVDA: Moderate ROIC, Moderate EV/EBITDA
        metricsMap.put("NVDA", new FundamentalMetrics("NVDA", new BigDecimal("0.10"), new BigDecimal("30.0")));
        // Fill out list with bad GARP candidates to reach 10 stocks (so 20% = 2 stocks)
        metricsMap.put("B1", new FundamentalMetrics("B1", new BigDecimal("0.01"), new BigDecimal("50.0")));
        metricsMap.put("B2", new FundamentalMetrics("B2", new BigDecimal("0.01"), new BigDecimal("50.0")));
        metricsMap.put("B3", new FundamentalMetrics("B3", new BigDecimal("0.01"), new BigDecimal("50.0")));
        metricsMap.put("B4", new FundamentalMetrics("B4", new BigDecimal("0.01"), new BigDecimal("50.0")));
        metricsMap.put("B5", new FundamentalMetrics("B5", new BigDecimal("0.01"), new BigDecimal("50.0")));
        
        when(dataAdapter.getFundamentalMetrics(anyList())).thenReturn(metricsMap);

        GarpStrategy garpStrategy = new GarpStrategy(dataAdapter);
        garpStrategy.setActive(true);
        System.out.println("Executing GARP Strategy...");
        garpStrategy.evaluate(broker);

        // Verify that it bought AAPL and MSFT
        verify(broker).placeMarketOrder(eq("AAPL"), anyInt(), eq("buy"), anyString());
        verify(broker).placeMarketOrder(eq("MSFT"), anyInt(), eq("buy"), anyString());
        
        System.out.println("GARP Strategy successfully analyzed universe and submitted target orders!");
    }
}
