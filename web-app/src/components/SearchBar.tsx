import { useState } from 'react';
import { useT } from '../i18n';
import type { KlineQuery, Market, Period } from '../types';

function formatDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function defaultStartDate(): string {
  const d = new Date();
  d.setFullYear(d.getFullYear() - 2);
  return formatDate(d);
}

function defaultEndDate(): string {
  return formatDate(new Date());
}

interface Props {
  onSearch: (query: KlineQuery) => void;
  loading: boolean;
}

export default function SearchBar({ onSearch, loading }: Props) {
  const { t } = useT();
  const [symbol, setSymbol]       = useState('');
  const [market, setMarket]       = useState<Market>('us');
  const [period, setPeriod]       = useState<Period>('daily');
  const [startDate, setStartDate] = useState(defaultStartDate);
  const [endDate, setEndDate]     = useState(defaultEndDate);

  const handleSubmit = () => {
    const trimmed = symbol.trim().toUpperCase();
    if (!trimmed) return;
    onSearch({ symbol: trimmed, market, period, startDate, endDate });
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleSubmit();
  };

  return (
    <div className="panel">
      <div className="search-bar">
        <div className="field">
          <label>{t.search.symbol}</label>
          <input
            type="text"
            placeholder={t.search.symbolPlaceholder}
            value={symbol}
            onChange={e => setSymbol(e.target.value)}
            onKeyDown={handleKeyDown}
          />
        </div>
        <div className="field">
          <label>{t.search.market}</label>
          <select value={market} onChange={e => setMarket(e.target.value as Market)}>
            {Object.entries(t.search.markets).map(([v, label]) => (
              <option key={v} value={v}>{label}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>{t.search.period}</label>
          <select value={period} onChange={e => setPeriod(e.target.value as Period)}>
            {Object.entries(t.search.periods).map(([v, label]) => (
              <option key={v} value={v}>{label}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>{t.search.startDate}</label>
          <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} />
        </div>
        <div className="field">
          <label>{t.search.endDate}</label>
          <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} />
        </div>
        <div className="field">
          <label>&nbsp;</label>
          <button className="btn-primary-blue" disabled={loading} onClick={handleSubmit}>
            {loading ? t.search.querying : t.search.query}
          </button>
        </div>
      </div>
    </div>
  );
}
