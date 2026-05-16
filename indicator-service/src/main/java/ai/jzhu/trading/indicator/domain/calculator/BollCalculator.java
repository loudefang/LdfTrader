package ai.jzhu.trading.indicator.domain.calculator;

import ai.jzhu.trading.common.dto.indicator.BollResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class BollCalculator {

    private static final int PERIOD = 20;

    public BollResult calculate(double[] closes) {
        int n = closes.length;
        List<Double> upperList = new ArrayList<>(n);
        List<Double> middleList = new ArrayList<>(n);
        List<Double> lowerList = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            if (i < PERIOD - 1) {
                upperList.add(null);
                middleList.add(null);
                lowerList.add(null);
            } else {
                double sum = 0;
                for (int j = i - PERIOD + 1; j <= i; j++) sum += closes[j];
                double mean = sum / PERIOD;

                // Population standard deviation
                double variance = 0;
                for (int j = i - PERIOD + 1; j <= i; j++) {
                    double d = closes[j] - mean;
                    variance += d * d;
                }
                double stdDev = Math.sqrt(variance / PERIOD);

                upperList.add(round(mean + 2 * stdDev));
                middleList.add(round(mean));
                lowerList.add(round(mean - 2 * stdDev));
            }
        }
        return new BollResult(upperList, middleList, lowerList);
    }

    private static Double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
