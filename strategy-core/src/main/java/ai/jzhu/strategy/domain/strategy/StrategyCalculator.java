package ai.jzhu.strategy.domain.strategy;

import ai.jzhu.strategy.domain.model.KlineData;

import java.util.List;

public final class StrategyCalculator {

    private StrategyCalculator() {}

    /**
     * 从 endIndex 往前取最多 period 根 K 线，返回其中最高价的最大值。
     * 若实际可用数据不足 minPeriod 根，返回 Double.NaN。
     */
    public static double getValidMaxCloseHighBetweenLastPeriod(
            List<KlineData> klines, int endIndex, int period, int minPeriod) {

        int startIndex = Math.max(0, endIndex - period + 1);
        int actualCount = endIndex - startIndex + 1;
        if (actualCount < minPeriod) return Double.NaN;

        double max = Double.NEGATIVE_INFINITY;
        for (int i = startIndex; i <= endIndex; i++) {
            max = Math.max(max, klines.get(i).high());
        }
        return max;
    }
}
