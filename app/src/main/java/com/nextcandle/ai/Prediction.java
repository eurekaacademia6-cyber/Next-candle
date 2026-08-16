package com.nextcandle.ai;

import java.util.List;

public final class Prediction {
    public final String label;
    public final double up;
    public final double down;
    public final double quality;
    public final double agreement;
    public final RegimeEngine.Regime regime;
    public final List<String> reasons;

    public Prediction(String label, double up, double quality, double agreement,
                      RegimeEngine.Regime regime, List<String> reasons) {
        this.label = label;
        this.up = up;
        this.down = 1.0 - up;
        this.quality = quality;
        this.agreement = agreement;
        this.regime = regime;
        this.reasons = reasons;
    }
}
