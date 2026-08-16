import com.nextcandle.ai.Candle;
import com.nextcandle.ai.Prediction;
import com.nextcandle.ai.PredictionEngine;
import java.util.ArrayList;
import java.util.List;

public class SmokeTest {
    public static void main(String[] args) {
        List<Candle> candles = new ArrayList<>();
        double price = 100.0;

        for (int i = 0; i < 15; i++) {
            double open = price;
            double close = price + 0.08 + (i % 3) * 0.02;
            candles.add(new Candle(
                open,
                close + 0.04,
                open - 0.03,
                close,
                100.0,
                i
            ));
            price = close;
        }

        Prediction prediction =
            new PredictionEngine().analyze(candles);

        System.out.println(prediction.label);
        System.out.println(prediction.upProbability);
        System.out.println(prediction.confidence);
    }
}
