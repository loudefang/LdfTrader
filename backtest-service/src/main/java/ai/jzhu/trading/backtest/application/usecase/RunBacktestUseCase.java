package ai.jzhu.trading.backtest.application.usecase;

import ai.jzhu.strategy.domain.indicator.IndicatorData;
import ai.jzhu.strategy.domain.model.KlineData;
import ai.jzhu.strategy.domain.strategy.TradingStrategy;
import ai.jzhu.trading.backtest.domain.port.IndicatorPort;
import ai.jzhu.trading.backtest.domain.port.MarketDataPort;
import ai.jzhu.trading.backtest.domain.service.BacktestEngine;
import ai.jzhu.trading.backtest.infrastructure.converter.DataConverter;
import ai.jzhu.trading.common.dto.KlineResponse;
import ai.jzhu.trading.common.dto.backtest.BacktestRequest;
import ai.jzhu.trading.common.dto.backtest.BacktestTradeDetail;
import ai.jzhu.trading.common.dto.backtest.SimpleBacktestResponse;
import ai.jzhu.trading.common.dto.indicator.IndicatorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunBacktestUseCase {

    private final MarketDataPort marketDataPort;
    private final IndicatorPort indicatorPort;
    private final BacktestEngine backtestEngine;
    private final List<TradingStrategy> strategies;

    public SimpleBacktestResponse execute(BacktestRequest req) {
        TradingStrategy strategy = strategies.stream()
                .filter(s -> s.getId().equals(req.strategyId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown strategyId: " + req.strategyId()));

        String market = req.market() != null ? req.market() : "us";
        String period = req.period() != null ? req.period() : "daily";
        LocalDate startDate = req.startDate() != null ? LocalDate.parse(req.startDate()) : null;
        LocalDate endDate   = req.endDate()   != null ? LocalDate.parse(req.endDate())   : null;

        log.info("[backtest] symbol={} market={} period={} {}~{} strategy={}",
                req.symbol(), market, period, startDate, endDate, req.strategyId());

        List<KlineResponse> klineResponses = marketDataPort.getKline(
                req.symbol(), market, period, startDate, endDate);

        if (klineResponses.isEmpty()) {
            return new SimpleBacktestResponse(req.symbol(), req.strategyId(),
                    strategy.getName(), 0, List.of());
        }

        IndicatorResponse indicatorResponse = indicatorPort.calculate(
                klineResponses, req.symbol(), market, period);

        List<KlineData> klines     = DataConverter.toKlineData(klineResponses);
        IndicatorData   indicators = DataConverter.toIndicatorData(indicatorResponse);

        List<BacktestTradeDetail> trades = backtestEngine.run(klines, indicators, strategy);

        return new SimpleBacktestResponse(
                req.symbol(), req.strategyId(), strategy.getName(),
                trades.size(), trades);
    }
}
