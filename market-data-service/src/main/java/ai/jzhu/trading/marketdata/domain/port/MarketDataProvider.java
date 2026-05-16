package ai.jzhu.trading.marketdata.domain.port;

import ai.jzhu.trading.marketdata.domain.model.Kline;

import java.time.LocalDate;
import java.util.List;

public interface MarketDataProvider {

    List<Kline> fetchHistorical(
            String symbol,
            String market,
            String period,
            LocalDate startDate,
            LocalDate endDate
    );
}
