import { useT } from '../i18n';
import type { StockTab } from '../types';

interface Props {
  tabs: StockTab[];
  activeKey: string | null;
  onSelect: (key: string) => void;
  onClose: (key: string) => void;
}

export function tabKey(t: Pick<StockTab, 'symbol' | 'market' | 'period'>): string {
  return `${t.symbol}-${t.market}-${t.period}`;
}

export default function StockTabs({ tabs, activeKey, onSelect, onClose }: Props) {
  const { t } = useT();
  if (tabs.length === 0) return null;

  return (
    <div className="panel">
      <div className="stock-tabs">
        {tabs.map(tab => {
          const key = tabKey(tab);
          const active = key === activeKey;
          const marketLabel = t.search.markets[tab.market] ?? tab.market;
          return (
            <div
              key={key}
              className={`stock-tab ${active ? 'active' : ''}`}
              onClick={() => onSelect(key)}
            >
              <span>{tab.symbol} {marketLabel}</span>
              <span
                className="close"
                onClick={e => { e.stopPropagation(); onClose(key); }}
              >
                ×
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
