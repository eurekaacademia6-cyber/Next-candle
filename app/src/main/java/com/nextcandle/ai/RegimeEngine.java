package com.nextcandle.ai;

public final class RegimeEngine {

    public enum Regime {
        TRENDING_UP,
        TRENDING_DOWN,
        RANGE,
        COMPRESSION,
        EXPANSION,
        UNKNOWN
    }

    public Regime detect(FeatureVector f) {
        double shortTrend = f.x[22];
        double longTrend = f.x[23];
        double expansion = f.x[12];
        double volatility = f.x[25];

        if (Math.abs(shortTrend) < 0.12 && Math.abs(longTrend) < 0.12) {
            if (Math.abs(expansion) < 0.10) return Regime.COMPRESSION;
            return Regime.RANGE;
        }

        if (expansion > 0.25 || volatility > 0.35) {
            return Regime.EXPANSION;
        }

        if (shortTrend > 0.20 && longTrend > 0.10) {
            return Regime.TRENDING_UP;
        }

        if (shortTrend < -0.20 && longTrend < -0.10) {
            return Regime.TRENDING_DOWN;
        }

        return Regime.UNKNOWN;
    }
}
