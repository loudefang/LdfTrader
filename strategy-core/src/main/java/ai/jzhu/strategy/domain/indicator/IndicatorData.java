package ai.jzhu.strategy.domain.indicator;

public record IndicatorData(
        MacdData macd,
        MaData ma,
        RsiData rsi,
        BollData boll
) {}
