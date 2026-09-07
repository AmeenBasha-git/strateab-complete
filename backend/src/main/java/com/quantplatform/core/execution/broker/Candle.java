package com.quantplatform.core.execution.broker;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Represents a single period of price action.
 */
public class Candle {
    private ZonedDateTime timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private BigDecimal vwapRth;
    private BigDecimal vwapEth;

    public Candle() {
    }

    public Candle(ZonedDateTime timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume) {
        this(timestamp, open, high, low, close, volume, null, null);
    }

    public Candle(ZonedDateTime timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume, BigDecimal vwapRth, BigDecimal vwapEth) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.vwapRth = vwapRth;
        this.vwapEth = vwapEth;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getVwapRth() {
        return vwapRth;
    }

    public void setVwapRth(BigDecimal vwapRth) {
        this.vwapRth = vwapRth;
    }

    public BigDecimal getVwapEth() {
        return vwapEth;
    }

    public void setVwapEth(BigDecimal vwapEth) {
        this.vwapEth = vwapEth;
    }

    @Override
    public String toString() {
        return "Candle{" +
                "timestamp=" + timestamp +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                ", vwapRth=" + vwapRth +
                ", vwapEth=" + vwapEth +
                '}';
    }
}
