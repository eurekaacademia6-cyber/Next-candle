package com.nextcandle.ai;

import java.util.List;

public final class Prediction {
    public final String label;
    public final double upProbability;
    public final double downProbability;
    public final double confidence;
    public final double agreement;
    public final RegimeEngine.Regime regime;
    public final List<String> reasons;

    public Prediction(
            String label,
            double upProbability,
            double confidence,
            double agreement,
            RegimeEngine.Regime regime,
            List<String> reasons) {

        this.label = label;
        this.upProbability = upProbability;
        this.downProbability = 1.0 - upProbability;
        this.confidence = confidence;
        this.agreement = agreement;
        this.regime = regime;
        this.reasons = reasons;
    }
}
