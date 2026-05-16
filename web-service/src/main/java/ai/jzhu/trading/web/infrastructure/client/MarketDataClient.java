package ai.jzhu.trading.web.infrastructure.client;

import ai.jzhu.trading.common.dto.KlineResponse;
import ai.jzhu.trading.web.domain.port.MarketDataPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class MarketDataClient implements MarketDataPort {

    private static final ParameterizedTypeReference<List<KlineResponse>> KLINE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final WebClient marketDataWebClient;

    public MarketDataClient(WebClient marketDataWebClient) {
        this.marketDataWebClient = marketDataWebClient;
    }

    @Override
    public List<KlineResponse> getKline(
            String symbol,
            String market,
            String period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        try {
            List<KlineResponse> response = marketDataWebClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path("/api/market-data/kline")
                                .queryParam("symbol", symbol)
                                .queryParam("market", market)
                                .queryParam("period", period);
                        if (startDate != null) {
                            b.queryParam("startDate", startDate.format(ISO_DATE));
                        }
                        if (endDate != null) {
                            b.queryParam("endDate", endDate.format(ISO_DATE));
                        }
                        return b.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::propagate)
                    .bodyToMono(KLINE_LIST)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return response == null ? List.of() : response;
        } catch (WebClientRequestException e) {
            log.error("[DOWNSTREAM DOWN] market-data-service unreachable: {}", e.getMessage());
            throw new IllegalStateException("Market data service is unavailable", e);
        } catch (WebClientResponseException e) {
            log.error("[DOWNSTREAM ERROR] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException(
                    "Market data service returned " + e.getStatusCode().value(), e);
        }
    }

    private Mono<? extends Throwable> propagate(org.springframework.web.reactive.function.client.ClientResponse resp) {
        return resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> new WebClientResponseException(
                        resp.statusCode().value(),
                        "Downstream " + resp.statusCode(),
                        resp.headers().asHttpHeaders(),
                        body.getBytes(),
                        null));
    }
}
