package ai.jzhu.trading.indicator.domain.port;

import ai.jzhu.trading.indicator.domain.model.IndicatorValues;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IndicatorRepository {

    Optional<IndicatorValues> findByCache(String symbol, String market, String period,
                                          LocalDate startDate, LocalDate endDate, int expectedCount);

    void saveBatch(String symbol, String market, String period,
                   List<LocalDate> dates, IndicatorValues values);
}
