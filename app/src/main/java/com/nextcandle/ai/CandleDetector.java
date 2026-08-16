package com.nextcandle.ai;

import java.util.ArrayList;
import java.util.List;

public final class CandleDetector {
    public List<Candle> detect(int[] gray, int width, int height) {
        List<Candle> result = new ArrayList<>();
        if (gray == null || width < 160 || height < 120) return result;

        int left = width / 12;
        int right = width * 11 / 12;
        int top = height / 10;
        int bottom = height * 9 / 10;
        int columns = 15;
        int slice = Math.max(4, (right - left) / columns);

        for (int col = 0; col < columns; col++) {
            int x0 = left + col * slice;
            int x1 = Math.min(right, x0 + slice);
            int minY = bottom;
            int maxY = top;
            long dark = 0;
            long bright = 0;
            long total = 0;
            for (int y = top; y < bottom; y++) {
                for (int x = x0; x < x1; x++) {
                    int g = gray[y * width + x];
                    if (g < 80) dark++;
                    if (g > 175) bright++;
                    total++;
                    if (g < 55 || g > 210) {
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }
            if (maxY <= minY || total == 0) continue;
            double contrast = (double) (dark + bright) / total;
            if (contrast < 0.015) continue;

            double range = Math.max(1.0, maxY - minY);
            double center = (maxY + minY) / 2.0;
            double bias = ((dark - bright) / (double) total) * range * 0.35;
            double open = 100.0 + (center + bias) * 0.01;
            double close = 100.0 + (center - bias) * 0.01;
            double high = 100.0 + minY * 0.01 - 0.02;
            double low = 100.0 + maxY * 0.01 + 0.02;
            result.add(new Candle(open, high, low, close, System.currentTimeMillis()));
        }
        return result;
    }
}
