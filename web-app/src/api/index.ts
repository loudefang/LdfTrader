import axios, { AxiosError } from 'axios';
import type {
  KlineQuery, KlineWithIndicatorsResponse, ErrorResponse,
  StrategyInfo, BacktestRequest, SimpleBacktestResponse,
} from '../types';

const api = axios.create({
  baseURL: 'http://localhost:8181/api/web',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export async function fetchKline(query: KlineQuery): Promise<KlineWithIndicatorsResponse> {
  try {
    const { data } = await api.get<KlineWithIndicatorsResponse>('/kline', {
      params: {
        symbol: query.symbol,
        market: query.market,
        period: query.period,
        startDate: query.startDate,
        endDate: query.endDate,
      },
    });
    return data;
  } catch (err) {
    const axiosErr = err as AxiosError<ErrorResponse>;
    const msg =
      axiosErr.response?.data?.message ||
      axiosErr.message ||
      '请求失败，请稍后重试';
    throw new Error(msg);
  }
}

export async function fetchStrategies(): Promise<StrategyInfo[]> {
  try {
    const { data } = await api.get<StrategyInfo[]>('/strategies');
    return data;
  } catch (err) {
    const axiosErr = err as AxiosError<ErrorResponse>;
    throw new Error(axiosErr.response?.data?.message || axiosErr.message || '获取策略列表失败');
  }
}

export async function runBacktest(req: BacktestRequest): Promise<SimpleBacktestResponse> {
  try {
    const { data } = await api.post<SimpleBacktestResponse>('/backtest/run', req, {
      timeout: 180000,
    });
    return data;
  } catch (err) {
    const axiosErr = err as AxiosError<ErrorResponse>;
    throw new Error(axiosErr.response?.data?.message || axiosErr.message || '回测失败');
  }
}

export default api;
