package ai.jzhu.trading.web.domain.port;

import ai.jzhu.trading.common.dto.backtest.BacktestRequest;
import ai.jzhu.trading.common.dto.backtest.SimpleBacktestResponse;
import ai.jzhu.trading.common.dto.backtest.StrategyInfo;

import java.util.List;

public interface BacktestPort {
    SimpleBacktestResponse run(BacktestRequest request);
    List<StrategyInfo> listStrategies();
}
