package com.nextcandle.ai;

import java.util.Arrays;
import java.util.List;

public final class FeatureEngine {

    public FeatureVector extract(List<Candle> candles) {
        FeatureVector f = new FeatureVector();

        if (candles == null || candles.size() < 5) {
            return f;
        }

        int n = candles.size();
        double[] closes = new double[n];
        double[] ranges = new double[n];
        double[] bodies = new double[n];

        for (int i = 0; i < n; i++) {
            Candle c = candles.get(i);
            closes[i] = c.close;
            ranges[i] = c.range();
            bodies[i] = c.body();
        }

        Candle last = candles.get(n - 1);
        double meanRange = Math.max(1e-9, Maths.mean(ranges));
        double meanBody = Math.max(1e-9, Maths.meanAbs(bodies));

        f.set(0, last.body() / meanRange);
        f.set(1, last.upperWick() / meanRange);
        f.set(2, last.lowerWick() / meanRange);
        f.set(3, last.closePosition() * 2.0 - 1.0);
        f.set(4, last.range() / meanRange - 1.0);
        f.set(5, last.bodyAbs() / meanBody - 1.0);

        f.set(6, (last.close - closes[Math.max(0, n - 4)]) / meanRange);
        f.set(7, (last.close - closes[Math.max(0, n - 7)]) / meanRange);

        double mean5 = meanLast(closes, 5);
        double mean10 = meanLast(closes, 10);
        f.set(8, (last.close - mean5) / meanRange);
        f.set(9, (mean5 - mean10) / meanRange);

        f.set(10, slope(closes, Math.min(5, n)) / meanRange);
        f.set(11, slope(closes, Math.min(10, n)) / meanRange);

        double last5Range = meanLast(ranges, 5);
        double previous5Range = meanPrevious(ranges, 10, 5);
        f.set(12, previous5Range == 0.0 ? 0.0 : last5Range / previous5Range - 1.0);

        double recentHigh = Double.NEGATIVE_INFINITY;
        double recentLow = Double.POSITIVE_INFINITY;

        for (int i = 0; i < n - 1; i++) {
            recentHigh = Math.max(recentHigh, candles.get(i).high);
            recentLow = Math.min(recentLow, candles.get(i).low);
        }

        f.set(13, (last.close - recentHigh) / meanRange);
        f.set(14, (last.close - recentLow) / meanRange);

        boolean downsideSweep = last.low < recentLow && last.close > recentLow;
        boolean upsideSweep = last.high > recentHigh && last.close < recentHigh;
        f.set(15, downsideSweep ? 1.0 : 0.0);
        f.set(16, upsideSweep ? 1.0 : 0.0);

        if (n >= 2) {
            Candle previous = candles.get(n - 2);

            boolean bullishEngulfing =
                    last.bullish() &&
                    previous.bearish() &&
                    last.open <= previous.close &&
                    last.close >= previous.open;

            boolean bearishEngulfing =
                    last.bearish() &&
                    previous.bullish() &&
                    last.open >= previous.close &&
                    last.close <= previous.open;

            f.set(17, bullishEngulfing ? 1.0 : (bearishEngulfing ? -1.0 : 0.0));
        }

        f.set(18, (last.lowerWick() - last.upperWick()) / meanRange);

        int streak = 0;
        boolean direction = last.bullish();
        for (int i = n - 1; i >= 0; i--) {
            Candle c = candles.get(i);
            if ((c.bullish() == direction) && c.bodyAbs() > 0.0) {
                streak++;
            } else {
                break;
            }
        }
        f.set(19, direction ? streak / 5.0 : -streak / 5.0);

        f.set(20, (last.range() - last5Range) / meanRange);
        f.set(21, rsi(candles, 10) / 50.0 - 1.0);

        f.set(22, Maths.clamp(f.x[10] / 2.0, -1.0, 1.0));
        f.set(23, Maths.clamp(f.x[11] / 2.0, -1.0, 1.0));
        f.set(24, (last.close - mean10) / meanRange);

        double[] last5 = Arrays.copyOfRange(
                ranges, Math.max(0, ranges.length - 5), ranges.length);
        f.set(25, Maths.clamp(Maths.stdev(last5) / meanRange, -3.0, 3.0));

        if (n >= 3) {
            double firstMove = closes[n - 2] - closes[n - 3];
            double secondMove = closes[n - 1] - closes[n - 2];
            f.set(26, (secondMove - firstMove) / meanRange);
        }

        f.set(27, Math.min(1.0, n / 15.0));
        return f;
    }

    private double meanLast(double[] values, int count) {
        int start = Math.max(0, values.length - count);
        return Maths.mean(Arrays.copyOfRange(values, start, values.length));
    }

    private double meanPrevious(double[] values, int totalLookback, int count) {
        int end = Math.max(0, values.length - count);
        int start = Math.max(0, end - count);
        if (end <= start) return 0.0;
        return Maths.mean(Arrays.copyOfRange(values, start, end));
    }

    private double slope(double[] values, int count) {
        if (count < 2) return 0.0;
        int start = Math.max(0, values.length - count);

        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double sxy = 0.0;

        for (int i = 0; i < count; i++) {
            double x = i;
            double y = values[start + i];
            sx += x;
            sy += y;
            sxx += x * x;
            sxy += x * y;
        }

        double denominator = count * sxx - sx * sx;
        return denominator == 0.0 ? 0.0 :
                (count * sxy - sx * sy) / denominator;
    }

    private double rsi(List<Candle> candles, int period) {
        int start = Math.max(1, candles.size() - period);
        double gains = 0.0;
        double losses = 0.0;

        for (int i = start; i < candles.size(); i++) {
            double change = candles.get(i).close - candles.get(i - 1).close;
            if (change > 0) gains += change;
            else losses -= change;
        }

        if (losses == 0.0) return 100.0;
        double rs = gains / losses;
        return 100.0 - 100.0 / (1.0 + rs);
    }
}
