package com.nextcandle.ai;

import java.util.List;

public final class FeatureEngine {
    public FeatureVector extract(List<Candle> c) {
        FeatureVector f = new FeatureVector();
        int n = c.size();
        if (n < 5) return f;

        double[] closes = new double[n];
        double[] ranges = new double[n];
        double[] bodies = new double[n];
        for (int i = 0; i < n; i++) {
            Candle k = c.get(i);
            closes[i] = k.close;
            ranges[i] = k.range();
            bodies[i] = k.body();
        }

        int s5 = Math.max(0, n - 5);
        int s10 = Math.max(0, n - 10);
        int s15 = Math.max(0, n - 15);
        double avgRange = Math.max(1e-9, mean(ranges, s15, n));
        double avgBody = Math.max(1e-9, meanAbs(bodies, s15, n));
        Candle last = c.get(n - 1);

        f.set(0, last.body() / avgRange);
        f.set(1, last.upperWick() / avgRange);
        f.set(2, last.lowerWick() / avgRange);
        f.set(3, 2.0 * last.closePosition() - 1.0);
        f.set(4, last.range() / avgRange - 1.0);
        f.set(5, last.bodyAbs() / avgBody - 1.0);
        f.set(6, (last.close - closes[Math.max(0, n - 4)]) / avgRange);
        f.set(7, (last.close - closes[Math.max(0, n - 7)]) / avgRange);

        double m5 = mean(closes, s5, n);
        double m10 = mean(closes, s10, n);
        f.set(8, (last.close - m5) / avgRange);
        f.set(9, (m5 - m10) / avgRange);
        f.set(10, slope(closes, s5, n) / avgRange);
        f.set(11, slope(closes, s10, n) / avgRange);
        f.set(12, mean(ranges, s5, n) / Math.max(1e-9, mean(ranges, s10, s5)) - 1.0);

        double hh = Double.NEGATIVE_INFINITY;
        double ll = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n - 1; i++) {
            hh = Math.max(hh, c.get(i).high);
            ll = Math.min(ll, c.get(i).low);
        }
        f.set(13, (last.close - hh) / avgRange);
        f.set(14, (last.close - ll) / avgRange);
        f.set(15, last.low < ll && last.close > ll ? 1.0 : 0.0);
        f.set(16, last.high > hh && last.close < hh ? 1.0 : 0.0);

        if (n >= 2) {
            Candle prev = c.get(n - 2);
            boolean bullEngulf = last.bullish() && prev.bearish() && last.open <= prev.close && last.close >= prev.open;
            boolean bearEngulf = last.bearish() && prev.bullish() && last.open >= prev.close && last.close <= prev.open;
            f.set(17, bullEngulf ? 1.0 : (bearEngulf ? -1.0 : 0.0));
        }

        f.set(18, (last.lowerWick() - last.upperWick()) / avgRange);
        f.set(19, directionalStreak(c));
        f.set(20, (last.range() - mean(ranges, s5, n)) / avgRange);
        f.set(21, rsiLike(closes, Math.min(10, n)) / 50.0 - 1.0);
        f.set(22, Math.signum(slope(closes, s5, n)) * Math.min(1.0, Math.abs(slope(closes, s5, n)) / avgRange));
        f.set(23, Maths.clamp(slope(closes, s10, n) / avgRange, -1.0, 1.0));
        f.set(24, (last.close - m10) / avgRange);
        f.set(25, stdev(ranges, s5, n) / avgRange);
        if (n >= 3) {
            double a1 = closes[n - 1] - closes[n - 2];
            double a2 = closes[n - 2] - closes[n - 3];
            f.set(26, (a1 - a2) / avgRange);
        }
        f.set(27, Math.min(1.0, n / 15.0));
        f.set(28, Math.abs(slope(closes, s5, n)) / avgRange);
        f.set(29, (last.close - hh) / Math.max(1e-9, hh - ll));
        f.set(30, (last.close - ll) / Math.max(1e-9, hh - ll));
        f.set(31, n >= 10 ? 1.0 : 0.5);
        return f;
    }

    private double directionalStreak(List<Candle> c) {
        Candle last = c.get(c.size() - 1);
        boolean dir = last.bullish();
        int streak = 0;
        for (int i = c.size() - 1; i >= 0; i--) {
            Candle k = c.get(i);
            if ((k.bullish() == dir) && k.bodyAbs() > 0.0) streak++; else break;
        }
        double signed = dir ? streak : -streak;
        return signed / 5.0;
    }

    private static double mean(double[] a, int start, int end) {
        if (end <= start) return 0.0;
        double s = 0.0;
        for (int i = start; i < end; i++) s += a[i];
        return s / (end - start);
    }

    private static double meanAbs(double[] a, int start, int end) {
        if (end <= start) return 0.0;
        double s = 0.0;
        for (int i = start; i < end; i++) s += Math.abs(a[i]);
        return s / (end - start);
    }

    private static double stdev(double[] a, int start, int end) {
        if (end - start < 2) return 0.0;
        double m = mean(a, start, end);
        double s = 0.0;
        for (int i = start; i < end; i++) { double d = a[i] - m; s += d * d; }
        return Math.sqrt(s / (end - start - 1));
    }

    private static double slope(double[] a, int start, int end) {
        int n = end - start;
        if (n < 2) return 0.0;
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (int i = 0; i < n; i++) {
            double x = i, y = a[start + i];
            sx += x; sy += y; sxx += x*x; sxy += x*y;
        }
        double den = n * sxx - sx * sx;
        return den == 0 ? 0.0 : (n * sxy - sx * sy) / den;
    }

    private static double rsiLike(double[] closes, int period) {
        if (closes.length < 2) return 50.0;
        int start = Math.max(1, closes.length - period);
        double gain = 0.0, loss = 0.0;
        for (int i = start; i < closes.length; i++) {
            double d = closes[i] - closes[i - 1];
            if (d > 0) gain += d; else loss -= d;
        }
        if (loss == 0.0) return 100.0;
        double rs = gain / loss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
}
