package ai.jzhu.strategy.domain.strategy.impl;

import ai.jzhu.strategy.domain.indicator.IndicatorData;
import ai.jzhu.strategy.domain.model.Direction;
import ai.jzhu.strategy.domain.model.KlineData;
import ai.jzhu.strategy.domain.model.TradeSignal;
import ai.jzhu.strategy.domain.strategy.StrategyCalculator;
import ai.jzhu.strategy.domain.strategy.TradingStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BullishLongStrategy implements TradingStrategy {

    @Override
    public String getId() { return "bullishLong"; }

    @Override
    public String getName() { return "高位突破-做多"; }

    @Override
    public String getDescription() {
        return "MACD多头排列，价格突破250日最高价，回踩MA5，触发做多信号";
    }

    @Override
    public Optional<TradeSignal> checkOpenSignal(
            List<KlineData> klines, IndicatorData indicators, int currentIndex, boolean hasPosition) {

        if (hasPosition || currentIndex < 10) return Optional.empty();

        KlineData currK  = klines.get(currentIndex);
        KlineData lastK1 = klines.get(currentIndex - 1);

        // 过去250周期最高价，从 currentIndex-2 开始往前算
        double maxHigh = StrategyCalculator.getValidMaxCloseHighBetweenLastPeriod(
                klines, currentIndex - 2, 250, 5);
        if (Double.isNaN(maxHigh)) return Optional.empty();

        Double dif = indicators.macd().getDifAt(currentIndex);
        Double dea = indicators.macd().getDeaAt(currentIndex);
        Double ma5 = indicators.ma().getMa5At(currentIndex);
        if (dif == null || dea == null || ma5 == null) return Optional.empty();

        if (currK.close() <= maxHigh)           return Optional.empty(); // 未突破
        if (!currK.isBullishPillar())            return Optional.empty(); // 非阳线
        if (lastK1.high() > maxHigh)             return Optional.empty(); // 前一根已突破
        if (dif <= dea)                          return Optional.empty(); // MACD 非多头
        if (Math.min(lastK1.close(), currK.low()) > ma5) return Optional.empty(); // 未回踩MA5

        String reason = String.format("突破%.2f，MACD多头，回踩MA5=%.2f", maxHigh, ma5);
        return Optional.of(TradeSignal.openLong(currentIndex, currK.close(), reason));
    }

    @Override
    public Optional<TradeSignal> checkCloseSignal(
            List<KlineData> klines, IndicatorData indicators, int currentIndex, TradeSignal openSignal) {

        if (openSignal.direction() != Direction.LONG) return Optional.empty();
        if (currentIndex < 5) return Optional.empty();

        Double ma5Curr  = indicators.ma().getMa5At(currentIndex);
        Double ma10Curr = indicators.ma().getMa10At(currentIndex);
        Double ma5Prev  = indicators.ma().getMa5At(currentIndex - 1);
        Double ma10Prev = indicators.ma().getMa10At(currentIndex - 1);
        if (ma5Curr == null || ma10Curr == null || ma5Prev == null || ma10Prev == null) {
            return Optional.empty();
        }

        KlineData currK = klines.get(currentIndex);
        KlineData prevK = klines.get(currentIndex - 1);

        boolean currBreak = currK.close() < ma5Curr  && currK.close() < ma10Curr;
        boolean prevBreak = prevK.close() < ma5Prev  && prevK.close() < ma10Prev;
        if (!currBreak || !prevBreak) return Optional.empty();

        String reason = String.format("连续2根K线跌破MA5(%.2f)和MA10(%.2f)", ma5Curr, ma10Curr);
        return Optional.of(TradeSignal.closeLong(currentIndex, currK.close(), reason));
    }
}
