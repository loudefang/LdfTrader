package ai.jzhu.trading.marketdata.infrastructure.external;

import ai.jzhu.trading.marketdata.domain.model.Kline;
import ai.jzhu.trading.marketdata.domain.port.MarketDataProvider;
import ai.jzhu.trading.marketdata.infrastructure.external.dto.FmpHistoricalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class FmpMarketDataProvider implements MarketDataProvider {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final WebClient fmpWebClient;
    private final String apiKey;

    public FmpMarketDataProvider(
            WebClient fmpWebClient,
            @Value("${fmp.api-key}") String apiKey
    ) {
        this.fmpWebClient = fmpWebClient;
        this.apiKey = apiKey;
    }

    @Override
    public List<Kline> fetchHistorical(
            String symbol,
            String market,
            String period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("FMP_API_KEY is not configured");
            throw new IllegalStateException("FMP API key is missing. Set FMP_API_KEY in .env");
        }

        String endpoint = resolveEndpoint(period);
        log.info("[FMP CALL] GET {} symbol={} from={} to={}", endpoint, symbol, startDate, endDate);

        try {
            List<FmpHistoricalResponse> response = fmpWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(endpoint)
                            .queryParam("symbol", symbol)
                            .queryParam("from", startDate.format(ISO_DATE))
                            .queryParam("to", endDate.format(ISO_DATE))
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::handle4xx)
                    .onStatus(HttpStatusCode::is5xxServerError, this::handle5xx)
                    .bodyToMono(new ParameterizedTypeReference<List<FmpHistoricalResponse>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || response.isEmpty()) {
                log.warn("[FMP EMPTY] symbol={} returned no historical data", symbol);
                return List.of();
            }

            List<FmpHistoricalResponse> historical = new ArrayList<>(response);
            Collections.reverse(historical);

            List<Kline> klines = new ArrayList<>(historical.size());
            for (FmpHistoricalResponse p : historical) {
                klines.add(new Kline(
                        LocalDate.parse(p.date(), ISO_DATE),
                        p.open(),
                        p.high(),
                        p.low(),
                        p.close(),
                        p.volume()
                ));
            }
            log.info("[FMP OK] symbol={} got {} bars", symbol, klines.size());
            return klines;
        } catch (WebClientResponseException e) {
            log.error("[FMP ERROR] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 429) {
                throw new IllegalStateException("FMP API rate limit reached. Please try again later.", e);
            }
            if (e.getStatusCode().value() == 404) {
                return List.of();
            }
            throw new IllegalStateException("FMP API call failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[FMP UNEXPECTED] symbol={} error={}", symbol, e.getMessage(), e);
            throw new IllegalStateException("Failed to fetch market data from FMP", e);
        }
    }

    private String resolveEndpoint(String period) {
        String normalized = period == null ? "daily" : period.toLowerCase();
        return switch (normalized) {
            case "daily", "weekly", "monthly" -> "/historical-price-eod/full";
            default -> throw new IllegalArgumentException("Unsupported period: " + period);
        };
    }

    private Mono<? extends Throwable> handle4xx(org.springframework.web.reactive.function.client.ClientResponse resp) {
        return resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> new WebClientResponseException(
                        resp.statusCode().value(),
                        "FMP 4xx",
                        resp.headers().asHttpHeaders(),
                        body.getBytes(),
                        null));
    }

    private Mono<? extends Throwable> handle5xx(org.springframework.web.reactive.function.client.ClientResponse resp) {
        return resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> new WebClientResponseException(
                        resp.statusCode().value(),
                        "FMP 5xx",
                        resp.headers().asHttpHeaders(),
                        body.getBytes(),
                        null));
    }
}
