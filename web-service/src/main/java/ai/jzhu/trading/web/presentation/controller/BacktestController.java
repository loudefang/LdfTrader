package ai.jzhu.trading.web.presentation.controller;

import ai.jzhu.trading.common.dto.backtest.BacktestRequest;
import ai.jzhu.trading.common.dto.backtest.SimpleBacktestResponse;
import ai.jzhu.trading.common.dto.backtest.StrategyInfo;
import ai.jzhu.trading.web.application.usecase.RunBacktestUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class BacktestController {

    private final RunBacktestUseCase runBacktestUseCase;

    @PostMapping("/backtest/run")
    public SimpleBacktestResponse run(@RequestBody BacktestRequest request) {
        return runBacktestUseCase.execute(request);
    }

    @GetMapping("/strategies")
    public List<StrategyInfo> listStrategies() {
        return runBacktestUseCase.listStrategies();
    }
}
