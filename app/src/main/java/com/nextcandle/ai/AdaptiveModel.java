package com.nextcandle.ai;

public final class AdaptiveModel {
    private final double[] weights = new double[FeatureVector.SIZE];
    private double bias = 0.0;
    private int samples = 0;

    public AdaptiveModel() {
        weights[0] = 0.65;
        weights[6] = 0.50;
        weights[7] = 0.30;
        weights[10] = 0.50;
        weights[11] = 0.30;
        weights[15] = 0.65;
        weights[16] = -0.65;
        weights[17] = 0.40;
        weights[18] = 0.20;
        weights[21] = 0.20;
    }

    public double predict(FeatureVector features) {
        double z = bias;
        for (int i = 0; i < weights.length; i++) {
            z += weights[i] * features.x[i];
        }
        return Maths.sigmoid(z);
    }

    public void update(FeatureVector features, boolean actualUp, double learningRate) {
        double prediction = predict(features);
        double target = actualUp ? 1.0 : 0.0;
        double error = target - prediction;

        for (int i = 0; i < weights.length; i++) {
            weights[i] += learningRate * error * features.x[i];
        }

        bias += learningRate * error;
        samples++;
    }

    public int samples() {
        return samples;
    }
}
