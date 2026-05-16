package ai.jzhu.trading.backtest.presentation.controller;

import ai.jzhu.strategy.domain.strategy.TradingStrategy;
import ai.jzhu.trading.backtest.application.usecase.RunBacktestUseCase;
import ai.jzhu.trading.common.dto.backtest.BacktestRequest;
import ai.jzhu.trading.common.dto.backtest.SimpleBacktestResponse;
import ai.jzhu.trading.common.dto.backtest.StrategyInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final RunBacktestUseCase runBacktestUseCase;
    private final List<TradingStrategy> strategies;

    @PostMapping("/run")
    public SimpleBacktestResponse run(@RequestBody BacktestRequest request) {
        return runBacktestUseCase.execute(request);
    }

    @GetMapping("/strategies")
    public List<StrategyInfo> listStrategies() {
        return strategies.stream()
                .map(s -> new StrategyInfo(s.getId(), s.getName(), s.getDescription()))
                .toList();
    }
}
