package ai.jzhu.trading.web.presentation.controller;

import ai.jzhu.trading.common.dto.KlineWithIndicatorsResponse;
import ai.jzhu.trading.web.application.usecase.GetKlineUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class KlineController {

    private final GetKlineUseCase getKlineUseCase;

    @GetMapping("/kline")
    public KlineWithIndicatorsResponse getKline(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "us") String market,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return getKlineUseCase.execute(symbol, market, period, startDate, endDate);
    }
}
