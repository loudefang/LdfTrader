import { useState } from 'react';
import type { KlineQuery, Market, Period } from '../types';

function formatDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function defaultStartDate(): string {
  const d = new Date();
  d.setFullYear(d.getFullYear() - 5);
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
  const [symbol, setSymbol] = useState('');
  const [market, setMarket] = useState<Market>('us');
  const [period, setPeriod] = useState<Period>('daily');
  const [startDate, setStartDate] = useState(defaultStartDate());
  const [endDate, setEndDate] = useState(defaultEndDate());

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
          <label>股票代码</label>
          <input
            type="text"
            placeholder="输入代码"
            value={symbol}
            onChange={(e) => setSymbol(e.target.value)}
            onKeyDown={handleKeyDown}
          />
        </div>
        <div className="field">
          <label>市场</label>
          <select value={market} onChange={(e) => setMarket(e.target.value as Market)}>
            <option value="us">美股</option>
          </select>
        </div>
        <div className="field">
          <label>周期</label>
          <select value={period} onChange={(e) => setPeriod(e.target.value as Period)}>
            <option value="daily">日K</option>
            <option value="weekly">周K</option>
            <option value="monthly">月K</option>
          </select>
        </div>
        <div className="field">
          <label>开始日期</label>
          <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        </div>
        <div className="field">
          <label>结束日期</label>
          <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </div>
        <div className="field">
          <label>&nbsp;</label>
          <button className="btn-primary-blue" disabled={loading} onClick={handleSubmit}>
            {loading ? '查询中...' : '查询'}
          </button>
        </div>
      </div>
    </div>
  );
}
