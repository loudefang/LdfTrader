package ai.jzhu.trading.indicator.domain.calculator;

import ai.jzhu.trading.common.dto.indicator.RsiResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class RsiCalculator {

    public RsiResult calculate(double[] closes) {
        return new RsiResult(
                calcRsi(closes, 6),
                calcRsi(closes, 12),
                calcRsi(closes, 24)
        );
    }

    private List<Double> calcRsi(double[] closes, int period) {
        int n = closes.length;
        List<Double> result = new ArrayList<>(n);

        // First `period` positions cannot be computed (need `period` price diffs to seed)
        for (int i = 0; i < period; i++) {
            result.add(null);
        }
        if (n <= period) return result;

        // Seed: average gain/loss over closes[0..period] (period diffs: closes[1]-closes[0] … closes[period]-closes[period-1])
        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            double diff = closes[i] - closes[i - 1];
            if (diff > 0) avgGain += diff;
            else avgLoss += -diff;
        }
        avgGain /= period;
        avgLoss /= period;
        result.add(rsiValue(avgGain, avgLoss));

        // Wilder smoothing
        for (int i = period + 1; i < n; i++) {
            double diff = closes[i] - closes[i - 1];
            double gain = diff > 0 ? diff : 0;
            double loss = diff < 0 ? -diff : 0;
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
            result.add(rsiValue(avgGain, avgLoss));
        }
        return result;
    }

    private static Double rsiValue(double avgGain, double avgLoss) {
        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        return round(100.0 - 100.0 / (1 + rs));
    }

    private static Double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
