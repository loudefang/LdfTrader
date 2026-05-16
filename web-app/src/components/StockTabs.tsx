import type { StockTab } from '../types';

const MARKET_LABEL: Record<string, string> = {
  us: '美股',
};

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
  if (tabs.length === 0) return null;

  return (
    <div className="panel">
      <div className="stock-tabs">
        {tabs.map((t) => {
          const key = tabKey(t);
          const active = key === activeKey;
          return (
            <div
              key={key}
              className={`stock-tab ${active ? 'active' : ''}`}
              onClick={() => onSelect(key)}
            >
              <span>
                {t.symbol} {MARKET_LABEL[t.market] ?? t.market}
              </span>
              <span
                className="close"
                onClick={(e) => {
                  e.stopPropagation();
                  onClose(key);
                }}
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
