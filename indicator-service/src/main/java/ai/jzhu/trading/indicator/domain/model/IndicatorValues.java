package ai.jzhu.trading.indicator.domain.model;

import ai.jzhu.trading.common.dto.indicator.BollResult;
import ai.jzhu.trading.common.dto.indicator.MacdResult;
import ai.jzhu.trading.common.dto.indicator.MaResult;
import ai.jzhu.trading.common.dto.indicator.RsiResult;

public record IndicatorValues(
        MacdResult macd,
        MaResult ma,
        RsiResult rsi,
        BollResult boll
) {}
