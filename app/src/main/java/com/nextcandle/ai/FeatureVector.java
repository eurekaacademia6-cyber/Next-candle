package com.nextcandle.ai;

public final class FeatureVector {
    public static final int SIZE = 32;
    public final double[] x = new double[SIZE];

    public void set(int i, double v) {
        if (i >= 0 && i < SIZE) x[i] = Maths.clamp(safe(v), -8.0, 8.0);
    }

    private static double safe(double v) {
        return (Double.isNaN(v) || Double.isInfinite(v)) ? 0.0 : v;
    }
}
