package ai.jzhu.trading.indicator.presentation.controller;

import ai.jzhu.trading.common.dto.indicator.IndicatorRequest;
import ai.jzhu.trading.common.dto.indicator.IndicatorResponse;
import ai.jzhu.trading.indicator.application.usecase.CalculateIndicatorsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indicators")
@RequiredArgsConstructor
public class IndicatorController {

    private final CalculateIndicatorsUseCase useCase;

    @PostMapping("/calculate")
    public ResponseEntity<IndicatorResponse> calculate(@RequestBody IndicatorRequest request) {
        return ResponseEntity.ok(useCase.calculate(request));
    }
}
