package ai.jzhu.trading.web.application.usecase;

import ai.jzhu.trading.common.dto.backtest.BacktestRequest;
import ai.jzhu.trading.common.dto.backtest.SimpleBacktestResponse;
import ai.jzhu.trading.common.dto.backtest.StrategyInfo;
import ai.jzhu.trading.web.domain.port.BacktestPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RunBacktestUseCase {

    private final BacktestPort backtestPort;

    public SimpleBacktestResponse execute(BacktestRequest request) {
        return backtestPort.run(request);
    }

    public List<StrategyInfo> listStrategies() {
        return backtestPort.listStrategies();
    }
}
