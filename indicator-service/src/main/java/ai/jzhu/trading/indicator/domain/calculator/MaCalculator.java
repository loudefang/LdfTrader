package ai.jzhu.trading.indicator.domain.calculator;

import ai.jzhu.trading.common.dto.indicator.MaResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class MaCalculator {

    public MaResult calculate(double[] closes) {
        int n = closes.length;
        return new MaResult(
                calcMa(closes, n, 5),
                calcMa(closes, n, 10),
                calcMa(closes, n, 20),
                calcMa(closes, n, 30),
                calcMa(closes, n, 60)
        );
    }

    private List<Double> calcMa(double[] closes, int n, int period) {
        List<Double> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (i < period - 1) {
                result.add(null);
            } else {
                double sum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += closes[j];
                }
                result.add(round(sum / period));
            }
        }
        return result;
    }

    private static Double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
