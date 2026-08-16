package com.nextcandle.ai;

public final class FeatureVector {
    public static final int SIZE = 28;
    public final double[] x = new double[SIZE];

    public FeatureVector set(int index, double value) {
        if (index >= 0 && index < SIZE) {
            x[index] = sanitize(value);
        }
        return this;
    }

    private static double sanitize(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0;
        }
        return Math.max(-8.0, Math.min(8.0, v));
    }
}
