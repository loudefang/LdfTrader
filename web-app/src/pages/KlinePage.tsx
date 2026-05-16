import { useState } from 'react';
import SearchBar from '../components/SearchBar';
import StockTabs, { tabKey } from '../components/StockTabs';
import KlineChart from '../components/KlineChart';
import { fetchKline } from '../api';
import { useT } from '../i18n';
import type { KlineQuery, StockTab, ChartVisibility, IndicatorConfig } from '../types';
import { DEFAULT_VISIBILITY, DEFAULT_CONFIG } from '../types';

const PREFS_KEY = 'ldftrader_chart_prefs';

function loadPrefs(): { visibility: ChartVisibility; config: IndicatorConfig } {
  try {
    const raw = localStorage.getItem(PREFS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      return {
        visibility: { ...DEFAULT_VISIBILITY, ...parsed.visibility },
        config: {
          maLines:   Array.isArray(parsed.config?.maLines)   ? parsed.config.maLines   : DEFAULT_CONFIG.maLines,
          bollLines: Array.isArray(parsed.config?.bollLines) ? parsed.config.bollLines : DEFAULT_CONFIG.bollLines,
          macdLines: Array.isArray(parsed.config?.macdLines) ? parsed.config.macdLines : DEFAULT_CONFIG.macdLines,
        },
      };
    }
  } catch { /* ignore */ }
  return { visibility: DEFAULT_VISIBILITY, config: DEFAULT_CONFIG };
}

function savePrefs(visibility: ChartVisibility, config: IndicatorConfig) {
  localStorage.setItem(PREFS_KEY, JSON.stringify({ visibility, config }));
}

export default function KlinePage() {
  const { t } = useT();
  const [tabs, setTabs]           = useState<StockTab[]>([]);
  const [activeKey, setActiveKey] = useState<string | null>(null);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState<string | null>(null);

  const [prefs, setPrefs] = useState(loadPrefs);
  const { visibility, config } = prefs;

  const activeTab = tabs.find(tab => tabKey(tab) === activeKey) ?? null;

  const handleSearch = async (query: KlineQuery) => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchKline(query);
      const newTab: StockTab = {
        ...query,
        klines: result.klines,
        indicators: result.indicators,
        totalCount: result.totalCount,
      };
      const key = tabKey(newTab);
      setTabs(prev => {
        const without = prev.filter(tab => tabKey(tab) !== key);
        return [...without, newTab];
      });
      setActiveKey(key);
    } catch (e) {
      setError(e instanceof Error ? e.message : '查询失败');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = (key: string) => {
    setTabs(prev => {
      const next = prev.filter(tab => tabKey(tab) !== key);
      if (key === activeKey) {
        setActiveKey(next.length ? tabKey(next[next.length - 1]) : null);
      }
      return next;
    });
  };

  const toggleVisibility = (key: keyof ChartVisibility) => {
    const next = { ...visibility, [key]: !visibility[key] };
    savePrefs(next, config);
    setPrefs({ visibility: next, config });
  };

  const handleConfigChange = (next: IndicatorConfig) => {
    savePrefs(visibility, next);
    setPrefs({ visibility, config: next });
  };

  const filterButtons: { key: keyof ChartVisibility; label: string }[] = [
    { key: 'kline',  label: t.chart.kline  },
    { key: 'volume', label: t.chart.volume },
    { key: 'ma',     label: t.chart.ma     },
    { key: 'boll',   label: t.chart.boll   },
    { key: 'macd',   label: t.chart.macd   },
    { key: 'rsi',    label: t.chart.rsi    },
  ];

  return (
    <div className="container-fluid py-3">
      <SearchBar onSearch={handleSearch} loading={loading} />

      {error && <div className="alert-error">{error}</div>}

      <StockTabs
        tabs={tabs}
        activeKey={activeKey}
        onSelect={setActiveKey}
        onClose={handleClose}
      />

      {activeTab && (
        <div className="chart-filter-bar">
          <div className="filter-group">
            {filterButtons.map(({ key, label }) => (
              <button
                key={key}
                className={`filter-btn${visibility[key] ? ' active' : ''}`}
                onClick={() => toggleVisibility(key)}
              >
                {label}
              </button>
            ))}
          </div>
          <div className="filter-divider" />
          <span className="toolbar-count">{t.chart.total(activeTab.totalCount)}</span>
          <button className="btn-backtest" disabled title={t.nav.comingSoon}>
            {t.chart.backtest}
          </button>
        </div>
      )}

      <div className="chart-wrapper">
        {loading ? (
          <div className="chart-empty">
            <span className="spinner" />
            {t.chart.loading}
          </div>
        ) : activeTab && activeTab.klines.length > 0 ? (
          <KlineChart
            symbol={activeTab.symbol}
            klines={activeTab.klines}
            indicators={activeTab.indicators}
            visibility={visibility}
            config={config}
            onConfigChange={handleConfigChange}
          />
        ) : (
          <div className="chart-empty">{t.chart.noData}</div>
        )}
      </div>
    </div>
  );
}
