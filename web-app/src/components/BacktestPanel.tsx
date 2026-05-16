import { useEffect, useState } from 'react';
import { fetchStrategies, runBacktest } from '../api';
import { useT } from '../i18n';
import type { StrategyInfo, SimpleBacktestResponse, BacktestRequest } from '../types';

interface Props {
  symbol: string;
  market: string;
  period: string;
  startDate: string;
  endDate: string;
  result: SimpleBacktestResponse | null;
  onResult: (result: SimpleBacktestResponse | null) => void;
}

export default function BacktestPanel({
  symbol, market, period, startDate, endDate, result, onResult,
}: Props) {
  const { t } = useT();
  const [strategies, setStrategies] = useState<StrategyInfo[]>([]);
  const [selectedId, setSelectedId]   = useState('');
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState<string | null>(null);

  useEffect(() => {
    fetchStrategies()
      .then(list => {
        setStrategies(list);
        if (list.length > 0) setSelectedId(list[0].id);
      })
      .catch(() => setError('获取策略列表失败'));
  }, []);

  const handleRun = async () => {
    if (!selectedId) return;
    setLoading(true);
    setError(null);
    try {
      const req: BacktestRequest = { symbol, market, period, startDate, endDate, strategyId: selectedId };
      const res = await runBacktest(req);
      onResult(res);
    } catch (e) {
      setError(e instanceof Error ? e.message : '回测失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="backtest-panel panel">
      <div className="backtest-panel-row">
        <label className="backtest-label">{t.backtest.strategy}</label>
        <select
          className="backtest-select"
          value={selectedId}
          onChange={e => setSelectedId(e.target.value)}
          disabled={loading || strategies.length === 0}
        >
          {strategies.map(s => (
            <option key={s.id} value={s.id} title={s.description}>
              {s.name}
            </option>
          ))}
          {strategies.length === 0 && <option value="">加载中...</option>}
        </select>

        <button
          className="btn-run-backtest"
          onClick={handleRun}
          disabled={loading || !selectedId}
        >
          {loading ? t.backtest.loading : t.backtest.run}
        </button>

        <button
          className="btn-clear-backtest"
          onClick={() => { onResult(null); setError(null); }}
          disabled={loading || !result}
        >
          {t.backtest.clear}
        </button>
      </div>

      {error && (
        <div className="alert-error" style={{ marginTop: 8 }}>{error}</div>
      )}

      {result && !error && (
        <div className="backtest-result-summary">
          <span className="backtest-strategy-name">{result.strategyName}</span>
          <span className="backtest-trade-count">{t.backtest.tradesCount(result.totalTrades)}</span>
          {result.trades.some(t => !t.closed) && (
            <span className="backtest-open-tip">（含未平仓）</span>
          )}
        </div>
      )}
    </div>
  );
}
