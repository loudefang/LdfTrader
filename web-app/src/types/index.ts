export interface KlineResponse {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface ErrorResponse {
  status: number;
  message: string;
  timestamp: string;
}

export type Market = 'us';
export type Period = 'daily' | 'weekly' | 'monthly';

export interface KlineQuery {
  symbol: string;
  market: Market;
  period: Period;
  startDate: string;
  endDate: string;
}

export interface StockTab {
  symbol: string;
  market: Market;
  period: Period;
  startDate: string;
  endDate: string;
  klines: KlineResponse[];
}
