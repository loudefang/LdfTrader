package ai.jzhu.trading.backtest.domain.service;

import ai.jzhu.strategy.domain.indicator.IndicatorData;
import ai.jzhu.strategy.domain.model.KlineData;
import ai.jzhu.strategy.domain.model.TradeSignal;
import ai.jzhu.strategy.domain.strategy.TradingStrategy;
import ai.jzhu.trading.common.dto.backtest.BacktestTradeDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class BacktestEngine {

    public List<BacktestTradeDetail> run(
            List<KlineData> klines,
            IndicatorData indicators,
            TradingStrategy strategy) {

        List<BacktestTradeDetail> trades = new ArrayList<>();
        TradeSignal position = null;

        for (int i = 0; i < klines.size(); i++) {
            if (position == null) {
                Optional<TradeSignal> openOpt = strategy.checkOpenSignal(klines, indicators, i, false);
                if (openOpt.isPresent()) {
                    position = openOpt.get();
                    log.debug("[{}] open @{} price={}", strategy.getId(), klines.get(i).date(), position.price());
                }
            } else {
                Optional<TradeSignal> closeOpt = strategy.checkCloseSignal(klines, indicators, i, position);
                if (closeOpt.isPresent()) {
                    TradeSignal open  = position;
                    TradeSignal close = closeOpt.get();
                    trades.add(new BacktestTradeDetail(
                            open.index(),  close.index(),
                            klines.get(open.index()).date(),  klines.get(close.index()).date(),
                            open.price(),  close.price(),
                            open.direction().name(),
                            open.reason(), close.reason(),
                            true
                    ));
                    log.debug("[{}] close @{} price={}", strategy.getId(), klines.get(i).date(), close.price());
                    position = null;
                }
            }
        }

        if (position != null) {
            KlineData lastKline = klines.get(klines.size() - 1);
            trades.add(new BacktestTradeDetail(
                    position.index(), -1,
                    klines.get(position.index()).date(), null,
                    position.price(), lastKline.close(),
                    position.direction().name(),
                    position.reason(), null,
                    false
            ));
        }

        return trades;
    }
}
