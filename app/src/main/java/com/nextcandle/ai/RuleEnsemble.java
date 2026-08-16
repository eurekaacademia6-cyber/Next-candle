package com.nextcandle.ai;

import java.util.ArrayList;
import java.util.List;

public final class RuleEnsemble {
    public static final class Component {
        public final String name;
        public final double pUp;
        public Component(String name, double pUp) { this.name = name; this.pUp = pUp; }
    }

    public List<Component> score(FeatureVector f, RegimeEngine.Regime r) {
        List<Component> out = new ArrayList<>();
        double regime = r == RegimeEngine.Regime.TREND_UP ? 0.70 : r == RegimeEngine.Regime.TREND_DOWN ? -0.70 : 0.0;
        out.add(new Component("Candle", Maths.sigmoid(1.5*f.x[0] - 0.8*f.x[1] + 0.9*f.x[2])));
        out.add(new Component("Momentum", Maths.sigmoid(1.4*f.x[6] + 0.8*f.x[7] + 0.5*f.x[21])));
        out.add(new Component("Trajectory", Maths.sigmoid(1.8*f.x[10] + 0.9*f.x[11] + 0.5*f.x[26])));
        out.add(new Component("Structure", Maths.sigmoid(1.2*f.x[13] - 1.2*f.x[14] + 0.7*f.x[22])));
        out.add(new Component("Liquidity", Maths.sigmoid(2.0*f.x[15] - 2.0*f.x[16] + 0.7*f.x[18])));
        out.add(new Component("Pattern", Maths.sigmoid(1.6*f.x[17] + 0.6*f.x[18])));
        out.add(new Component("Regime", Maths.sigmoid(1.6*regime)));
        return out;
    }

    public double average(List<Component> items) {
        double s = 0.0;
        for (Component c : items) s += c.pUp;
        return items.isEmpty() ? 0.5 : s / items.size();
    }

    public double agreement(List<Component> items, double pUp) {
        if (items.isEmpty()) return 0.0;
        boolean up = pUp >= 0.5;
        int agree = 0;
        for (Component c : items) if ((c.pUp >= 0.5) == up) agree++;
        return (double) agree / items.size();
    }
}
