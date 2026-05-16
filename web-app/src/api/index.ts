import axios, { AxiosError } from 'axios';
import type { KlineQuery, KlineResponse, ErrorResponse } from '../types';

const api = axios.create({
  baseURL: 'http://localhost:8181/api/web',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
});

export async function fetchKline(query: KlineQuery): Promise<KlineResponse[]> {
  try {
    const { data } = await api.get<KlineResponse[]>('/kline', {
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

export default api;
