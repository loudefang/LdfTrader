package ai.jzhu.trading.marketdata.infrastructure.external;

import ai.jzhu.trading.marketdata.domain.model.Kline;
import ai.jzhu.trading.marketdata.domain.port.MarketDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 按 market 把请求分发到正确的 {@link MarketDataProvider} 实现:
 * <ul>
 *   <li>{@code us} → {@link FmpMarketDataProvider} (financialmodelingprep.com)</li>
 *   <li>{@code cn} / {@code hk} → {@link AkShareMarketDataProvider} (本地 akshare-bridge)</li>
 * </ul>
 * 标记 {@code @Primary} 以确保 {@code GetKlineUseCase} 注入到本路由器,
 * 其余两个 provider 仍作为普通 bean 存在并被本类依赖注入。
 */
@Slf4j
@Component
@Primary
public class MarketDataProviderRouter implements MarketDataProvider {

    private final FmpMarketDataProvider fmpProvider;
    private final AkShareMarketDataProvider akShareProvider;

    public MarketDataProviderRouter(
            FmpMarketDataProvider fmpProvider,
            AkShareMarketDataProvider akShareProvider
    ) {
        this.fmpProvider = fmpProvider;
        this.akShareProvider = akShareProvider;
    }

    @Override
    public List<Kline> fetchHistorical(
            String symbol,
            String market,
            String period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        MarketDataProvider target = resolve(market);
        return target.fetchHistorical(symbol, market, period, startDate, endDate);
    }

    private MarketDataProvider resolve(String market) {
        String normalized = market == null ? "us" : market.toLowerCase();
        if (AkShareMarketDataProvider.SUPPORTED_MARKETS.contains(normalized)) {
            log.debug("[ROUTE] market={} -> AKShare", normalized);
            return akShareProvider;
        }
        if ("us".equals(normalized)) {
            log.debug("[ROUTE] market={} -> FMP", normalized);
            return fmpProvider;
        }
        throw new IllegalArgumentException("Unsupported market: " + market
                + " (supported: us, cn, hk)");
    }
}
