package com.nextcandle.ai;

public final class Candle {
    public final double open;
    public final double high;
    public final double low;
    public final double close;
    public final long timeMs;

    public Candle(double open, double high, double low, double close, long timeMs) {
        this.open = open;
        this.high = Math.max(high, Math.max(open, close));
        this.low = Math.min(low, Math.min(open, close));
        this.close = close;
        this.timeMs = timeMs;
    }

    public double range() { return Math.max(1e-9, high - low); }
    public double body() { return close - open; }
    public double bodyAbs() { return Math.abs(body()); }
    public double upperWick() { return Math.max(0.0, high - Math.max(open, close)); }
    public double lowerWick() { return Math.max(0.0, Math.min(open, close) - low); }
    public double bodyRatio() { return bodyAbs() / range(); }
    public double closePosition() { return (close - low) / range(); }
    public boolean bullish() { return close > open; }
    public boolean bearish() { return close < open; }
}
