package com.nextcandle.ai;

public final class AdaptiveModel {
    private final double[] weights = new double[FeatureVector.SIZE];
    private double bias;
    private int samples;

    public AdaptiveModel() {
        weights[0] = 0.80;
        weights[6] = 0.55;
        weights[7] = 0.30;
        weights[10] = 0.55;
        weights[11] = 0.35;
        weights[15] = 0.65;
        weights[16] = -0.65;
        weights[17] = 0.40;
        weights[18] = 0.20;
        weights[21] = 0.25;
    }

    public double predict(FeatureVector f) {
        double z = bias;
        for (int i = 0; i < weights.length; i++) z += weights[i] * f.x[i];
        return Maths.sigmoid(z);
    }

    public void update(FeatureVector f, boolean actualUp, double learningRate) {
        double p = predict(f);
        double y = actualUp ? 1.0 : 0.0;
        double error = y - p;
        for (int i = 0; i < weights.length; i++) weights[i] += learningRate * error * f.x[i];
        bias += learningRate * error;
        samples++;
    }

    public int samples() { return samples; }
}
