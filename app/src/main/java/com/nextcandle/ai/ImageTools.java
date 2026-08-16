package com.nextcandle.ai;

import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public final class ImageTools {
    private ImageTools() {}

    public static int[] rgbaToGray(ImageProxy image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] out = new int[width * height];
        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        byte[] row = new byte[rowStride];

        for (int y = 0; y < height; y++) {
            int rowBytes = Math.min(rowStride, buffer.remaining());
            buffer.get(row, 0, rowBytes);
            for (int x = 0; x < width; x++) {
                int i = x * pixelStride;
                if (i + 2 >= rowBytes) break;
                int r = row[i] & 0xFF;
                int g = row[i + 1] & 0xFF;
                int b = row[i + 2] & 0xFF;
                out[y * width + x] = (r * 299 + g * 587 + b * 114) / 1000;
            }
        }
        return out;
    }
}
