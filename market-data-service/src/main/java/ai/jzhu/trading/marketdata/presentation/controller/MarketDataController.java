package ai.jzhu.trading.marketdata.presentation.controller;

import ai.jzhu.trading.common.dto.KlineResponse;
import ai.jzhu.trading.marketdata.application.usecase.GetKlineUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final GetKlineUseCase getKlineUseCase;

    @GetMapping("/kline")
    public List<KlineResponse> getKline(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "us") String market,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return getKlineUseCase.execute(symbol, market, period, startDate, endDate);
    }
}
