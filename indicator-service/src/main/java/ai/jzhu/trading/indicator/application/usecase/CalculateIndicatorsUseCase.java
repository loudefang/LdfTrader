package ai.jzhu.trading.indicator.application.usecase;

import ai.jzhu.trading.common.dto.indicator.IndicatorRequest;
import ai.jzhu.trading.common.dto.indicator.IndicatorResponse;
import ai.jzhu.trading.indicator.domain.calculator.BollCalculator;
import ai.jzhu.trading.indicator.domain.calculator.MacdCalculator;
import ai.jzhu.trading.indicator.domain.calculator.MaCalculator;
import ai.jzhu.trading.indicator.domain.calculator.RsiCalculator;
import ai.jzhu.trading.indicator.domain.model.IndicatorValues;
import ai.jzhu.trading.indicator.domain.port.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalculateIndicatorsUseCase {

    private final IndicatorRepository repository;
    private final MacdCalculator macdCalculator;
    private final MaCalculator maCalculator;
    private final RsiCalculator rsiCalculator;
    private final BollCalculator bollCalculator;

    public IndicatorResponse calculate(IndicatorRequest request) {
        var klines = request.klines();
        if (klines == null || klines.isEmpty()) {
            throw new IllegalArgumentException("klines must not be empty");
        }

        List<LocalDate> dates = klines.stream()
                .map(k -> LocalDate.parse(k.date()))
                .toList();
        LocalDate startDate = dates.get(0);
        LocalDate endDate = dates.get(dates.size() - 1);

        var cached = repository.findByCache(
                request.symbol(), request.market(), request.period(),
                startDate, endDate, klines.size());

        if (cached.isPresent()) {
            log.info("从缓存读取指标 symbol={} market={} period={}", request.symbol(), request.market(), request.period());
            return toResponse(cached.get());
        }

        log.info("重新计算指标 symbol={} market={} period={} count={}", request.symbol(), request.market(), request.period(), klines.size());
        double[] closes = klines.stream().mapToDouble(k -> k.close()).toArray();

        var values = new IndicatorValues(
                macdCalculator.calculate(closes),
                maCalculator.calculate(closes),
                rsiCalculator.calculate(closes),
                bollCalculator.calculate(closes)
        );

        repository.saveBatch(request.symbol(), request.market(), request.period(), dates, values);

        return toResponse(values);
    }

    private IndicatorResponse toResponse(IndicatorValues v) {
        return new IndicatorResponse(v.macd(), v.ma(), v.rsi(), v.boll());
    }
}
