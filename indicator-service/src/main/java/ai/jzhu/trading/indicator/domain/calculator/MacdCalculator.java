package ai.jzhu.trading.indicator.domain.calculator;

import ai.jzhu.trading.common.dto.indicator.MacdResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * EMA seed = first close price; all positions are computable (no leading nulls).
 * MACD柱 = (DIF - DEA) × 2
 */
@Component
public class MacdCalculator {

    public MacdResult calculate(double[] closes) {
        int n = closes.length;
        double[] ema12 = calcEma(closes, 12);
        double[] ema26 = calcEma(closes, 26);

        double[] dif = new double[n];
        for (int i = 0; i < n; i++) {
            dif[i] = ema12[i] - ema26[i];
        }
        double[] dea = calcEma(dif, 9);

        List<Double> difList = new ArrayList<>(n);
        List<Double> deaList = new ArrayList<>(n);
        List<Double> macdList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            difList.add(round(dif[i]));
            deaList.add(round(dea[i]));
            macdList.add(round((dif[i] - dea[i]) * 2));
        }
        return new MacdResult(difList, deaList, macdList);
    }

    private double[] calcEma(double[] data, int period) {
        int n = data.length;
        double[] ema = new double[n];
        if (n == 0) return ema;
        double k = 2.0 / (period + 1);
        ema[0] = data[0];
        for (int i = 1; i < n; i++) {
            ema[i] = data[i] * k + ema[i - 1] * (1 - k);
        }
        return ema;
    }

    private static Double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
