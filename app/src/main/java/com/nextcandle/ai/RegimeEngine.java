package com.nextcandle.ai;

public final class RegimeEngine {
    public enum Regime { TREND_UP, TREND_DOWN, RANGE, COMPRESSION, EXPANSION, UNKNOWN }

    public Regime detect(FeatureVector f) {
        double shortTrend = f.x[22];
        double longTrend = f.x[23];
        double expansion = f.x[12];
        double vol = f.x[25];
        if (Math.abs(shortTrend) < 0.15 && Math.abs(longTrend) < 0.15) {
            return Math.abs(expansion) < 0.10 ? Regime.COMPRESSION : Regime.RANGE;
        }
        if (expansion > 0.25 || vol > 0.35) return Regime.EXPANSION;
        if (shortTrend > 0.25 && longTrend > 0.10) return Regime.TREND_UP;
        if (shortTrend < -0.25 && longTrend < -0.10) return Regime.TREND_DOWN;
        return Regime.UNKNOWN;
    }
}
