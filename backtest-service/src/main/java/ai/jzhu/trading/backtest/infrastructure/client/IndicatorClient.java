package ai.jzhu.trading.backtest.infrastructure.client;

import ai.jzhu.trading.backtest.domain.port.IndicatorPort;
import ai.jzhu.trading.common.dto.KlineResponse;
import ai.jzhu.trading.common.dto.indicator.IndicatorRequest;
import ai.jzhu.trading.common.dto.indicator.IndicatorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class IndicatorClient implements IndicatorPort {

    private final WebClient indicatorWebClient;

    public IndicatorClient(WebClient indicatorWebClient) {
        this.indicatorWebClient = indicatorWebClient;
    }

    @Override
    public IndicatorResponse calculate(List<KlineResponse> klines, String symbol,
                                       String market, String period) {
        var request = new IndicatorRequest(klines, symbol, market, period);
        try {
            return indicatorWebClient.post()
                    .uri("/api/indicators/calculate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::propagate)
                    .bodyToMono(IndicatorResponse.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
        } catch (WebClientRequestException e) {
            log.error("[DOWNSTREAM DOWN] indicator-service unreachable: {}", e.getMessage());
            throw new IllegalStateException("Indicator service is unavailable", e);
        } catch (WebClientResponseException e) {
            log.error("[DOWNSTREAM ERROR] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Indicator service returned " + e.getStatusCode().value(), e);
        }
    }

    private Mono<? extends Throwable> propagate(org.springframework.web.reactive.function.client.ClientResponse resp) {
        return resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> new WebClientResponseException(
                        resp.statusCode().value(), "Downstream " + resp.statusCode(),
                        resp.headers().asHttpHeaders(), body.getBytes(), null));
    }
}
