package ai.jzhu.trading.web.infrastructure.client;

import ai.jzhu.trading.common.dto.backtest.BacktestRequest;
import ai.jzhu.trading.common.dto.backtest.SimpleBacktestResponse;
import ai.jzhu.trading.common.dto.backtest.StrategyInfo;
import ai.jzhu.trading.web.domain.port.BacktestPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
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
public class BacktestClient implements BacktestPort {

    private static final ParameterizedTypeReference<List<StrategyInfo>> STRATEGY_LIST =
            new ParameterizedTypeReference<>() {};

    private final WebClient backtestWebClient;

    public BacktestClient(WebClient backtestWebClient) {
        this.backtestWebClient = backtestWebClient;
    }

    @Override
    public SimpleBacktestResponse run(BacktestRequest request) {
        try {
            return backtestWebClient.post()
                    .uri("/api/backtest/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::propagate)
                    .bodyToMono(SimpleBacktestResponse.class)
                    .timeout(Duration.ofSeconds(120))
                    .block();
        } catch (WebClientRequestException e) {
            log.error("[DOWNSTREAM DOWN] backtest-service unreachable: {}", e.getMessage());
            throw new IllegalStateException("Backtest service is unavailable", e);
        } catch (WebClientResponseException e) {
            log.error("[DOWNSTREAM ERROR] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Backtest service returned " + e.getStatusCode().value(), e);
        }
    }

    @Override
    public List<StrategyInfo> listStrategies() {
        try {
            return backtestWebClient.get()
                    .uri("/api/backtest/strategies")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::propagate)
                    .bodyToMono(STRATEGY_LIST)
                    .timeout(Duration.ofSeconds(10))
                    .block();
        } catch (WebClientRequestException e) {
            log.error("[DOWNSTREAM DOWN] backtest-service unreachable: {}", e.getMessage());
            throw new IllegalStateException("Backtest service is unavailable", e);
        } catch (WebClientResponseException e) {
            log.error("[DOWNSTREAM ERROR] status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Backtest service returned " + e.getStatusCode().value(), e);
        }
    }

    private Mono<? extends Throwable> propagate(org.springframework.web.reactive.function.client.ClientResponse resp) {
        return resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> new WebClientResponseException(
                        resp.statusCode().value(), "Downstream " + resp.statusCode(),
                        resp.headers().asHttpHeaders(), body.getBytes(), null));
    }
}
