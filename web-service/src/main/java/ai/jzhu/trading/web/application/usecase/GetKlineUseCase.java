package ai.jzhu.trading.web.application.usecase;

import ai.jzhu.trading.common.dto.KlineResponse;
import ai.jzhu.trading.common.dto.KlineWithIndicatorsResponse;
import ai.jzhu.trading.web.domain.port.IndicatorPort;
import ai.jzhu.trading.web.domain.port.MarketDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetKlineUseCase {

    private final MarketDataPort marketDataPort;
    private final IndicatorPort indicatorPort;

    public KlineWithIndicatorsResponse execute(
            String symbol,
            String market,
            String period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        log.info("[BFF] kline+indicators request symbol={} market={} period={} {}~{}",
                symbol, market, period, startDate, endDate);

        List<KlineResponse> klines = marketDataPort.getKline(symbol, market, period, startDate, endDate);

        var indicators = indicatorPort.calculate(klines, symbol, market, period);

        return new KlineWithIndicatorsResponse(klines, indicators, klines.size());
    }
}
