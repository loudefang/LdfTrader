package ai.jzhu.trading.marketdata.application.usecase;

import ai.jzhu.trading.common.dto.KlineResponse;
import ai.jzhu.trading.marketdata.domain.model.Kline;
import ai.jzhu.trading.marketdata.domain.port.KlineRepository;
import ai.jzhu.trading.marketdata.domain.port.MarketDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetKlineUseCase {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final LocalDate DEFAULT_START = LocalDate.of(2000, 1, 1);

    private final KlineRepository klineRepository;
    private final MarketDataProvider marketDataProvider;

    public List<KlineResponse> execute(
            String symbol,
            String market,
            String period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        LocalDate effectiveStart = startDate != null ? startDate : DEFAULT_START;
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();

        List<Kline> cached = klineRepository.findBySymbolAndDateRange(
                symbol, market, period, effectiveStart, effectiveEnd
        );

        if (!cached.isEmpty()) {
            log.info("[CACHE HIT] {} {} {} {}~{} -> {} rows (from DB)",
                    symbol, market, period, effectiveStart, effectiveEnd, cached.size());
            return toResponse(cached);
        }

        log.info("[CACHE MISS] {} {} {} {}~{} -> fetching from FMP API",
                symbol, market, period, effectiveStart, effectiveEnd);

        List<Kline> fetched = marketDataProvider.fetchHistorical(
                symbol, market, period, effectiveStart, effectiveEnd
        );

        if (fetched.isEmpty()) {
            log.warn("[FMP EMPTY] {} {} returned no data", symbol, market);
            return List.of();
        }

        int saved = klineRepository.saveBatch(symbol, market, period, fetched);
        log.info("[CACHE STORE] {} {} {} -> saved {} of {} rows",
                symbol, market, period, saved, fetched.size());

        return toResponse(fetched);
    }

    private List<KlineResponse> toResponse(List<Kline> klines) {
        return klines.stream()
                .map(k -> new KlineResponse(
                        k.date().format(ISO_DATE),
                        k.open(),
                        k.high(),
                        k.low(),
                        k.close(),
                        k.volume()
                ))
                .toList();
    }
}
