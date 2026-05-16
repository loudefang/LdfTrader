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

export interface MacdResult {
  difList: (number | null)[];
  deaList: (number | null)[];
  macdList: (number | null)[];
}

export interface MaResult {
  ma5List: (number | null)[];
  ma10List: (number | null)[];
  ma20List: (number | null)[];
  ma30List: (number | null)[];
  ma60List: (number | null)[];
}

export interface RsiResult {
  rsi6List: (number | null)[];
  rsi12List: (number | null)[];
  rsi24List: (number | null)[];
}

export interface BollResult {
  upperList: (number | null)[];
  middleList: (number | null)[];
  lowerList: (number | null)[];
}

export interface IndicatorResponse {
  macd: MacdResult;
  ma: MaResult;
  rsi: RsiResult;
  boll: BollResult;
}

export interface KlineWithIndicatorsResponse {
  klines: KlineResponse[];
  indicators: IndicatorResponse;
  totalCount: number;
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
  indicators: IndicatorResponse;
  totalCount: number;
}

// ── Chart preference types (persisted to localStorage) ────────────────────────

export interface ChartVisibility {
  kline: boolean;
  volume: boolean;
  ma: boolean;
  boll: boolean;
  macd: boolean;
  rsi: boolean;
}

export interface IndicatorConfig {
  maLines: string[];    // subset of ['MA5','MA10','MA20','MA30','MA60']
  bollLines: string[];  // subset of ['upper','middle','lower']
  macdLines: string[];  // subset of ['dif','dea','hist']
}

export const DEFAULT_VISIBILITY: ChartVisibility = {
  kline: true, volume: true, ma: true, boll: false, macd: true, rsi: true,
};

export const DEFAULT_CONFIG: IndicatorConfig = {
  maLines: ['MA5', 'MA10', 'MA20'],
  bollLines: ['upper', 'middle', 'lower'],
  macdLines: ['dif', 'dea', 'hist'],
};
