package com.nextcandle.ai;

import java.util.ArrayList;
import java.util.List;

public final class RuleEnsemble {

    public static final class Component {
        public final String name;
        public final double upProbability;

        public Component(String name, double upProbability) {
            this.name = name;
            this.upProbability = upProbability;
        }
    }

    public List<Component> score(FeatureVector f, RegimeEngine.Regime regime) {
        List<Component> result = new ArrayList<>();

        result.add(new Component(
                "Candle geometry",
                Maths.sigmoid(1.5 * f.x[0] - 0.8 * f.x[1] + 0.9 * f.x[2])));

        result.add(new Component(
                "Momentum",
                Maths.sigmoid(1.4 * f.x[6] + 0.8 * f.x[7] + 0.4 * f.x[21])));

        result.add(new Component(
                "Trajectory",
                Maths.sigmoid(1.7 * f.x[10] + 1.0 * f.x[11] + 0.6 * f.x[26])));

        result.add(new Component(
                "Location",
                Maths.sigmoid(-0.8 * f.x[24] + 0.4 * f.x[13] - 0.4 * f.x[14])));

        result.add(new Component(
                "Liquidity",
                Maths.sigmoid(1.8 * f.x[15] - 1.8 * f.x[16] + 0.7 * f.x[18])));

        result.add(new Component(
                "Pattern",
                Maths.sigmoid(1.4 * f.x[17] + 0.6 * f.x[18])));

        double regimeBias =
                regime == RegimeEngine.Regime.TRENDING_UP ? 0.7 :
                regime == RegimeEngine.Regime.TRENDING_DOWN ? -0.7 : 0.0;

        result.add(new Component(
                "Regime",
                Maths.sigmoid(1.2 * regimeBias)));

        return result;
    }

    public double average(List<Component> components) {
        if (components.isEmpty()) return 0.5;

        double sum = 0.0;
        for (Component c : components) {
            sum += c.upProbability;
        }
        return sum / components.size();
    }

    public double agreement(List<Component> components, double probability) {
        if (components.isEmpty()) return 0.0;

        int agreeing = 0;
        boolean up = probability >= 0.5;

        for (Component c : components) {
            if ((c.upProbability >= 0.5) == up) {
                agreeing++;
            }
        }

        return (double) agreeing / components.size();
    }
}
