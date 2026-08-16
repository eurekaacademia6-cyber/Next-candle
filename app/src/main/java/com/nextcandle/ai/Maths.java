package com.nextcandle.ai;

public final class Maths {
    private Maths() {}

    public static double mean(double[] values) {
        if (values.length == 0) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    public static double meanAbs(double[] values) {
        if (values.length == 0) return 0.0;
        double sum = 0.0;
        for (double v : values) sum += Math.abs(v);
        return sum / values.length;
    }

    public static double stdev(double[] values) {
        if (values.length < 2) return 0.0;
        double m = mean(values);
        double sum = 0.0;
        for (double v : values) {
            double d = v - m;
            sum += d * d;
        }
        return Math.sqrt(sum / (values.length - 1));
    }

    public static double sigmoid(double z) {
        if (z >= 0) {
            double e = Math.exp(-z);
            return 1.0 / (1.0 + e);
        }
        double e = Math.exp(z);
        return e / (1.0 + e);
    }

    public static double clamp(double v, double low, double high) {
        return Math.max(low, Math.min(high, v));
    }
}
