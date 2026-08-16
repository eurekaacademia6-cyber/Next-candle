package com.nextcandle.ai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 41;

    private PreviewView previewView;
    private TextView statusText;
    private TextView predictionText;
    private TextView probabilityText;
    private TextView diagnosticsText;
    private TextView detailsText;

    private final ExecutorService analyzerExecutor = Executors.newSingleThreadExecutor();
    private final CandleDetector candleDetector = new CandleDetector();
    private final PredictionEngine predictionEngine = new PredictionEngine();

    private volatile long lastAnalysisMs = 0L;
    private volatile boolean analyzing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        statusText = findViewById(R.id.statusText);
        predictionText = findViewById(R.id.predictionText);
        probabilityText = findViewById(R.id.probabilityText);
        diagnosticsText = findViewById(R.id.diagnosticsText);
        detailsText = findViewById(R.id.detailsText);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        statusText.setText("LIVE CAMERA");
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(this);
        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider provider = providerFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                analysis.setAnalyzer(analyzerExecutor, this::analyzeFrame);

                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Exception e) {
                statusText.setText("CAMERA ERROR");
                detailsText.setText("Camera failed to start: " + e.getClass().getSimpleName());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeFrame(@NonNull ImageProxy image) {
        long now = System.currentTimeMillis();
        if (analyzing || now - lastAnalysisMs < 700L) {
            image.close();
            return;
        }
        analyzing = true;
        lastAnalysisMs = now;

        try {
            int[] gray = ImageTools.rgbaToGray(image);
            List<Candle> candles = candleDetector.detect(gray, image.getWidth(), image.getHeight());
            Prediction prediction = predictionEngine.analyze(candles);
            runOnUiThread(() -> render(prediction, candles.size()));
        } catch (RuntimeException e) {
            runOnUiThread(() -> {
                statusText.setText("ANALYZER ERROR");
                detailsText.setText("Analysis paused: " + e.getClass().getSimpleName());
            });
        } finally {
            image.close();
            analyzing = false;
        }
    }

    private void render(Prediction p, int candleCount) {
        predictionText.setText(p.label);
        probabilityText.setText(String.format(Locale.US, "UP %.1f%%    DOWN %.1f%%", p.up * 100.0, p.down * 100.0));
        diagnosticsText.setText(String.format(Locale.US,
                "Candles: %d | Quality: %.0f%% | Agreement: %.0f%%",
                candleCount, p.quality * 100.0, p.agreement * 100.0));

        StringBuilder text = new StringBuilder();
        text.append("Regime: ").append(p.regime.name()).append("\n");
        for (String reason : p.reasons) text.append("- ").append(reason).append("\n");
        detailsText.setText(text.toString());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            statusText.setText("CAMERA PERMISSION REQUIRED");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        analyzerExecutor.shutdownNow();
    }
}
