package ai.jzhu.trading.marketdata.infrastructure.external;

import ai.jzhu.trading.marketdata.domain.model.Kline;
import ai.jzhu.trading.marketdata.domain.port.MarketDataProvider;
import ai.jzhu.trading.marketdata.infrastructure.external.dto.AkShareHistoricalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 通过本地 AKShare 旁车 (akshare-bridge, 默认 :8186) 拉取 A股 / 港股行情。
 * 与 {@link FmpMarketDataProvider} 共同实现 {@link MarketDataProvider},
 * 由 {@link MarketDataProviderRouter} 按 market 分发。
 */
@Slf4j
@Component
public class AkShareMarketDataProvider implements MarketDataProvider {

    public static final Set<String> SUPPORTED_MARKETS = Set.of("cn", "hk");

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final WebClient akshareWebClient;

    public AkShareMarketDataProvider(WebClient akshareWebClient) {
        this.akshareWebClient = akshareWebClient;
    }

    @Override
    public List<Kline> fetchHistorical(
            String symbol,
            String market,
            String period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String normalizedMarket = market == null ? "" : market.toLowerCase();
        if (!SUPPORTED_MARKETS.contains(normalizedMarket)) {
            throw new IllegalArgumentException(
                    "AkShareMarketDataProvider 不支持 market=" + market + " (仅支持 cn / hk)");
        }
        String normalizedPeriod = resolvePeriod(period);

        log.info("[AKSHARE CALL] symbol={} market={} period={} from={} to={}",
                symbol, normalizedMarket, normalizedPeriod, startDate, endDate);

        try {
            List<AkShareHistoricalResponse> response = akshareWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/historical")
                            .queryParam("symbol", symbol)
                            .queryParam("market", normalizedMarket)
                            .queryParam("period", normalizedPeriod)
                            .queryParam("start", startDate.format(ISO_DATE))
                            .queryParam("end", endDate.format(ISO_DATE))
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                    .onStatus(HttpStatusCode::is5xxServerError, this::handle5xx)
                    .bodyToMono(new ParameterizedTypeReference<List<AkShareHistoricalResponse>>() {})
                    .timeout(Duration.ofSeconds(60))
                    .block();

            if (response == null || response.isEmpty()) {
                log.warn("[AKSHARE EMPTY] symbol={} market={} returned no data",
                        symbol, normalizedMarket);
                return List.of();
            }

            List<Kline> klines = new ArrayList<>(response.size());
            for (AkShareHistoricalResponse p : response) {
                klines.add(new Kline(
                        LocalDate.parse(p.date(), ISO_DATE),
                        p.open(),
                        p.high(),
                        p.low(),
                        p.close(),
                        p.volume()
                ));
            }
            log.info("[AKSHARE OK] symbol={} market={} got {} bars",
                    symbol, normalizedMarket, klines.size());
            return klines;
        } catch (WebClientResponseException e) {
            log.error("[AKSHARE ERROR] status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 404) {
                return List.of();
            }
            throw new IllegalStateException(
                    "AKShare bridge 调用失败: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[AKSHARE UNEXPECTED] symbol={} market={} error={}",
                    symbol, normalizedMarket, e.getMessage(), e);
            throw new IllegalStateException(
                    "无法从 AKShare bridge 获取行情, 请确认 akshare-bridge (:8186) 在运行", e);
        }
    }

    private String resolvePeriod(String period) {
        String normalized = period == null ? "daily" : period.toLowerCase();
        return switch (normalized) {
            case "daily", "weekly", "monthly" -> normalized;
            default -> throw new IllegalArgumentException("Unsupported period: " + period);
        };
    }

    private Mono<? extends Throwable> handle4xx(
            org.springframework.web.reactive.function.client.ClientResponse resp) {
        return resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> new WebClientResponseException(
                        resp.statusCode().value(),
                        "AKShare bridge 4xx",
                        resp.headers().asHttpHeaders(),
                        body.getBytes(),
                        null));
    }

    private Mono<? extends Throwable> handle5xx(
            org.springframework.web.reactive.function.client.ClientResponse resp) {
        return resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> new WebClientResponseException(
                        resp.statusCode().value(),
                        "AKShare bridge 5xx",
                        resp.headers().asHttpHeaders(),
                        body.getBytes(),
                        null));
    }
}
