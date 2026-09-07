package com.quantplatform.core.data.fundamental;

import java.math.BigDecimal;

public class FundamentalMetrics {
    private String symbol;
    private BigDecimal roic;
    private BigDecimal evToEbitda;

    public FundamentalMetrics() {}

    public FundamentalMetrics(String symbol, BigDecimal roic, BigDecimal evToEbitda) {
        this.symbol = symbol;
        this.roic = roic;
        this.evToEbitda = evToEbitda;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getRoic() {
        return roic;
    }

    public void setRoic(BigDecimal roic) {
        this.roic = roic;
    }

    public BigDecimal getEvToEbitda() {
        return evToEbitda;
    }

    public void setEvToEbitda(BigDecimal evToEbitda) {
        this.evToEbitda = evToEbitda;
    }
}
