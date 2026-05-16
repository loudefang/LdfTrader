package ai.jzhu.trading.backtest.infrastructure.converter;

import ai.jzhu.strategy.domain.indicator.*;
import ai.jzhu.strategy.domain.model.KlineData;
import ai.jzhu.trading.common.dto.KlineResponse;
import ai.jzhu.trading.common.dto.indicator.IndicatorResponse;

import java.util.List;

public final class DataConverter {

    private DataConverter() {}

    public static List<KlineData> toKlineData(List<KlineResponse> responses) {
        return responses.stream()
                .map(r -> new KlineData(r.date(), r.open(), r.high(), r.low(), r.close(), r.volume()))
                .toList();
    }

    public static IndicatorData toIndicatorData(IndicatorResponse response) {
        MacdData macd = new MacdData(
                response.macd().difList(),
                response.macd().deaList(),
                response.macd().macdList()
        );
        MaData ma = new MaData(
                response.ma().ma5List(),
                response.ma().ma10List(),
                response.ma().ma20List(),
                response.ma().ma30List(),
                response.ma().ma60List()
        );
        RsiData rsi = new RsiData(
                response.rsi().rsi6List(),
                response.rsi().rsi12List(),
                response.rsi().rsi24List()
        );
        BollData boll = new BollData(
                response.boll().upperList(),
                response.boll().middleList(),
                response.boll().lowerList()
        );
        return new IndicatorData(macd, ma, rsi, boll);
    }
}
