package com.nextcandle.ai;

import android.graphics.Bitmap;
import java.util.List;

public final class ChartVisionEngine {

    private final CandleSequenceBuilder sequenceBuilder =
            new CandleSequenceBuilder();

    public List<Candle> analyze(Bitmap bitmap) {
        if (bitmap == null) {
            return java.util.Collections.emptyList();
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int[] gray = new int[pixels.length];

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];

            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            gray[i] = (int) (0.299 * r + 0.587 * g + 0.114 * b);
        }

        return sequenceBuilder.buildProxySequence(gray, width, height);
    }
}
