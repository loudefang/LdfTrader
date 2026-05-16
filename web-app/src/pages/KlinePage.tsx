import { useState } from 'react';
import SearchBar from '../components/SearchBar';
import StockTabs, { tabKey } from '../components/StockTabs';
import KlineChart from '../components/KlineChart';
import { fetchKline } from '../api';
import type { KlineQuery, StockTab } from '../types';

export default function KlinePage() {
  const [tabs, setTabs] = useState<StockTab[]>([]);
  const [activeKey, setActiveKey] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const activeTab = tabs.find((t) => tabKey(t) === activeKey) ?? null;

  const handleSearch = async (query: KlineQuery) => {
    setLoading(true);
    setError(null);
    try {
      const klines = await fetchKline(query);
      const newTab: StockTab = { ...query, klines };
      const key = tabKey(newTab);
      setTabs((prev) => {
        const without = prev.filter((t) => tabKey(t) !== key);
        return [...without, newTab];
      });
      setActiveKey(key);
    } catch (e) {
      const msg = e instanceof Error ? e.message : '查询失败';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = (key: string) => {
    setTabs((prev) => {
      const next = prev.filter((t) => tabKey(t) !== key);
      if (key === activeKey) {
        setActiveKey(next.length ? tabKey(next[next.length - 1]) : null);
      }
      return next;
    });
  };

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
        <div className="status-row">共 {activeTab.klines.length} 条数据</div>
      )}

      <div className="chart-wrapper">
        {loading ? (
          <div className="chart-empty">
            <span className="spinner" />
            正在加载行情数据...
          </div>
        ) : activeTab && activeTab.klines.length > 0 ? (
          <KlineChart symbol={activeTab.symbol} klines={activeTab.klines} />
        ) : (
          <div className="chart-empty">请输入股票代码开始查询</div>
        )}
      </div>
    </div>
  );
}
