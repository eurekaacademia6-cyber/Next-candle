package com.nextcandle.ai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1001;

    private PreviewView previewView;
    private TextView stateText;
    private TextView predictionText;
    private TextView probabilityText;
    private TextView diagnosticsText;
    private TextView reasonsText;

    private final ExecutorService cameraExecutor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final ChartVisionEngine visionEngine =
            new ChartVisionEngine();

    private final PredictionEngine predictionEngine =
            new PredictionEngine();

    private volatile boolean analyzing = false;
    private volatile long lastAnalysisMs = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        stateText = findViewById(R.id.stateText);
        predictionText = findViewById(R.id.predictionText);
        probabilityText = findViewById(R.id.probabilityText);
        diagnosticsText = findViewById(R.id.diagnosticsText);
        reasonsText = findViewById(R.id.reasonsText);

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        stateText.setText("LIVE CAMERA");

        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(this);

        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider provider = providerFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider());

                ImageAnalysis analysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                analysis.setOutputImageFormat(
                ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888);
        analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                provider.unbindAll();

                provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis);

            } catch (Exception e) {
                stateText.setText("CAMERA ERROR");
                reasonsText.setText(
                        "Camera initialization failed: " + e.getClass().getSimpleName());
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeFrame(ImageProxy image) {
        long now = System.currentTimeMillis();

        if (analyzing || now - lastAnalysisMs < 700L) {
            image.close();
            return;
        }

        analyzing = true;
        lastAnalysisMs = now;

        try {
            Bitmap bitmap = proxyToBitmap(image);

            if (bitmap != null) {
                List<Candle> candles = visionEngine.analyze(bitmap);
                Prediction prediction = predictionEngine.analyze(candles);

                mainHandler.post(() ->
                        renderPrediction(prediction, candles.size()));
            }

        } finally {
            image.close();
            analyzing = false;
        }
    }

    private void renderPrediction(Prediction prediction, int candleCount) {
        predictionText.setText(prediction.label);

        probabilityText.setText(
                String.format(
                        Locale.US,
                        "UP %.1f%%    DOWN %.1f%%",
                        prediction.upProbability * 100.0,
                        prediction.downProbability * 100.0));

        diagnosticsText.setText(
                String.format(
                        Locale.US,
                        "Candles: %d | Quality: %.0f%% | Agreement: %.0f%%",
                        candleCount,
                        prediction.confidence * 100.0,
                        prediction.agreement * 100.0));

        StringBuilder reasons = new StringBuilder();
        reasons.append("Regime: ")
                .append(prediction.regime.name())
                .append("
");

        for (String reason : prediction.reasons) {
            reasons.append("- ")
                    .append(reason)
                    .append("
");
        }

        reasonsText.setText(reasons.toString());
    }

    private Bitmap proxyToBitmap(ImageProxy image) {
        if (image.getPlanes().length == 0) {
            return null;
        }

        ImageProxy.PlaneProxy plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();

        if (pixelStride < 4) {
            return null;
        }

        byte[] row = new byte[rowStride];
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            buffer.position(y * rowStride);
            int bytesToRead = Math.min(rowStride, buffer.remaining());
            buffer.get(row, 0, bytesToRead);

            for (int x = 0; x < width; x++) {
                int offset = x * pixelStride;

                if (offset + 3 >= bytesToRead) {
                    continue;
                }

                int r = row[offset] & 0xFF;
                int g = row[offset + 1] & 0xFF;
                int b = row[offset + 2] & 0xFF;
                int a = row[offset + 3] & 0xFF;

                pixels[y * width + x] =
                        (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        return Bitmap.createBitmap(
                pixels,
                width,
                height,
                Bitmap.Config.ARGB_8888);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode == CAMERA_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {
            stateText.setText("CAMERA DENIED");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdownNow();
    }
}
