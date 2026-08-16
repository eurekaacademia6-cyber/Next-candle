package com.nextcandle.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PredictionEngine {
    private final FeatureEngine featureEngine = new FeatureEngine();
    private final RegimeEngine regimeEngine = new RegimeEngine();
    private final RuleEnsemble ruleEnsemble = new RuleEnsemble();
    private final AdaptiveModel adaptiveModel = new AdaptiveModel();

    public Prediction analyze(List<Candle> candles) {
        if (candles == null || candles.size() < 10) {
            return new Prediction("NO EDGE", 0.5, 0.0, 0.0, RegimeEngine.Regime.UNKNOWN,
                    Collections.singletonList("Need at least 10 readable candles."));
        }

        FeatureVector features = featureEngine.extract(candles);
        RegimeEngine.Regime regime = regimeEngine.detect(features);
        List<RuleEnsemble.Component> components = ruleEnsemble.score(features, regime);
        double ruleProbability = ruleEnsemble.average(components);
        double adaptiveProbability = adaptiveModel.predict(features);

        double adaptiveWeight = Math.min(0.45, 0.10 + adaptiveModel.samples() / 2000.0);
        double probability = ruleProbability * (1.0 - adaptiveWeight) + adaptiveProbability * adaptiveWeight;

        double edge = Math.abs(probability - 0.5) * 2.0;
        double agreement = ruleEnsemble.agreement(components, probability);
        double quality = Maths.clamp(0.55 * edge + 0.30 * agreement + 0.15 * features.x[27], 0.0, 1.0);

        String label = "NO EDGE";
        if (quality >= 0.58 && Math.abs(probability - 0.5) >= 0.09 && agreement >= 0.57) {
            label = probability >= 0.5 ? "UP" : "DOWN";
        }

        List<String> reasons = new ArrayList<>();
        if (features.x[6] > 0.15) reasons.add("short-term momentum rising");
        if (features.x[6] < -0.15) reasons.add("short-term momentum falling");
        if (features.x[15] > 0.5) reasons.add("downside liquidity sweep proxy");
        if (features.x[16] > 0.5) reasons.add("upside liquidity sweep proxy");
        if (features.x[17] > 0.5) reasons.add("bullish engulfing-like geometry");
        if (features.x[17] < -0.5) reasons.add("bearish engulfing-like geometry");
        if (features.x[18] > 0.2) reasons.add("lower-wick rejection");
        if (features.x[18] < -0.2) reasons.add("upper-wick rejection");
        if (regime == RegimeEngine.Regime.TREND_UP) reasons.add("uptrend regime");
        if (regime == RegimeEngine.Regime.TREND_DOWN) reasons.add("downtrend regime");
        if (regime == RegimeEngine.Regime.COMPRESSION) reasons.add("compression regime");
        if (regime == RegimeEngine.Regime.EXPANSION) reasons.add("volatility expansion");
        if (reasons.isEmpty()) reasons.add("evidence is mixed or too weak");

        return new Prediction(label, probability, quality, agreement, regime, reasons);
    }

    public AdaptiveModel getAdaptiveModel() { return adaptiveModel; }
}
