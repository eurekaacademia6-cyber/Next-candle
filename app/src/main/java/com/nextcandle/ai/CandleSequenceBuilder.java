package com.nextcandle.ai;

import java.util.ArrayList;
import java.util.List;

public final class CandleSequenceBuilder {

    public static final int MAX_CANDLES = 15;

    public List<Candle> buildProxySequence(int[] gray, int width, int height) {
        List<Candle> result = new ArrayList<>();

        if (gray == null || width < 120 || height < 120) {
            return result;
        }

        int left = width / 12;
        int right = width * 11 / 12;
        int top = height / 8;
        int bottom = height * 7 / 8;

        int candleColumns = 15;
        int columnWidth = Math.max(4, (right - left) / candleColumns);

        double base = 100.0;

        for (int i = 0; i < candleColumns; i++) {
            int xStart = left + i * columnWidth;
            int xEnd = Math.min(right, xStart + columnWidth);

            int minY = bottom;
            int maxY = top;
            int hits = 0;

            for (int y = top; y < bottom; y++) {
                for (int x = xStart; x < xEnd; x++) {
                    int value = gray[y * width + x];

                    if (value > 180 || value < 70) {
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                        hits++;
                    }
                }
            }

            if (hits < 8 || maxY <= minY) {
                continue;
            }

            double range = Math.max(1.0, maxY - minY);
            double center = (maxY + minY) * 0.5;

            // Deterministic geometry proxy.
            // Real production accuracy requires a trained chart-vision model.
            double open = base + (center - minY) * 0.01;
            double close = base + (center - maxY) * 0.01;
            double high = Math.max(open, close) + range * 0.01;
            double low = Math.min(open, close) - range * 0.01;

            result.add(new Candle(
                    open,
                    high,
                    low,
                    close,
                    hits,
                    System.currentTimeMillis()));
        }

        return result;
    }
}
