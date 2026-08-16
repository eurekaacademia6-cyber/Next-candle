package com.nextcandle.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PredictionEngine {

    private final FeatureEngine featureEngine = new FeatureEngine();
    private final RegimeEngine regimeEngine = new RegimeEngine();
    private final RuleEnsemble ensemble = new RuleEnsemble();
    private final AdaptiveModel adaptiveModel = new AdaptiveModel();

    public Prediction analyze(List<Candle> candles) {
        if (candles == null || candles.size() < 10) {
            return new Prediction(
                    "NO EDGE",
                    0.5,
                    0.0,
                    0.0,
                    RegimeEngine.Regime.UNKNOWN,
                    Collections.singletonList("Need at least 10 readable candles."));
        }

        FeatureVector f = featureEngine.extract(candles);
        RegimeEngine.Regime regime = regimeEngine.detect(f);

        List<RuleEnsemble.Component> components = ensemble.score(f, regime);
        double ruleProbability = ensemble.average(components);
        double adaptiveProbability = adaptiveModel.predict(f);

        double adaptiveWeight =
                Math.min(0.40, 0.10 + adaptiveModel.samples() / 2500.0);

        double probability =
                ruleProbability * (1.0 - adaptiveWeight) +
                adaptiveProbability * adaptiveWeight;

        double directionalEdge = Math.abs(probability - 0.5) * 2.0;
        double agreement = ensemble.agreement(components, probability);

        double quality = Maths.clamp(
                directionalEdge * 0.55 +
                agreement * 0.30 +
                f.x[27] * 0.15,
                0.0,
                1.0);

        String label = "NO EDGE";

        if (quality >= 0.58 &&
                Math.abs(probability - 0.5) >= 0.09 &&
                agreement >= 0.57) {

            label = probability >= 0.5 ? "UP" : "DOWN";
        }

        List<String> reasons = new ArrayList<>();

        if (f.x[6] > 0.15) reasons.add("short-term momentum rising");
        if (f.x[6] < -0.15) reasons.add("short-term momentum falling");
        if (f.x[15] > 0.5) reasons.add("downside liquidity sweep proxy");
        if (f.x[16] > 0.5) reasons.add("upside liquidity sweep proxy");
        if (f.x[17] > 0.5) reasons.add("bullish engulfing-like geometry");
        if (f.x[17] < -0.5) reasons.add("bearish engulfing-like geometry");
        if (f.x[18] > 0.20) reasons.add("lower-wick rejection");
        if (f.x[18] < -0.20) reasons.add("upper-wick rejection");

        if (regime == RegimeEngine.Regime.TRENDING_UP) {
            reasons.add("uptrend regime");
        } else if (regime == RegimeEngine.Regime.TRENDING_DOWN) {
            reasons.add("downtrend regime");
        } else if (regime == RegimeEngine.Regime.COMPRESSION) {
            reasons.add("compression regime");
        } else if (regime == RegimeEngine.Regime.EXPANSION) {
            reasons.add("volatility expansion");
        }

        if (reasons.isEmpty()) {
            reasons.add("evidence is not strongly directional");
        }

        return new Prediction(
                label,
                probability,
                quality,
                agreement,
                regime,
                reasons);
    }

    public AdaptiveModel getAdaptiveModel() {
        return adaptiveModel;
    }
}
