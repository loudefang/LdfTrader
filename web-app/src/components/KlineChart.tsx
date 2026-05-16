import { useEffect, useRef } from 'react';
import * as echarts from 'echarts';
import type { EChartsOption } from 'echarts';
import type { KlineResponse } from '../types';

const COLOR_UP = '#ef4444';
const COLOR_DOWN = '#22c55e';
const TEXT_PRIMARY = '#e6edf3';
const TEXT_MUTED = '#8b949e';
const BORDER = '#30363d';
const BG_PANEL = '#161b22';

interface Props {
  symbol: string;
  klines: KlineResponse[];
}

export default function KlineChart({ symbol, klines }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    chartRef.current = echarts.init(containerRef.current, undefined, {
      renderer: 'canvas',
    });

    const handleResize = () => chartRef.current?.resize();
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chartRef.current?.dispose();
      chartRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!chartRef.current) return;

    const dates = klines.map((k) => k.date);
    const candleData = klines.map((k) => [k.open, k.close, k.low, k.high]);
    const volumes = klines.map((k, i) => ({
      value: k.volume,
      itemStyle: {
        color: k.close >= k.open ? COLOR_UP : COLOR_DOWN,
      },
      _index: i,
    }));

    const option: EChartsOption = {
      backgroundColor: 'transparent',
      title: {
        text: symbol,
        left: 12,
        top: 8,
        textStyle: { color: TEXT_PRIMARY, fontSize: 16, fontWeight: 'bold' },
      },
      legend: {
        data: ['K线'],
        top: 8,
        right: 24,
        textStyle: { color: TEXT_PRIMARY },
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        backgroundColor: BG_PANEL,
        borderColor: BORDER,
        textStyle: { color: TEXT_PRIMARY },
        formatter: (params: unknown) => {
          const arr = params as Array<{
            seriesName: string;
            data: number[] | number;
            dataIndex: number;
          }>;
          const candle = arr.find((p) => p.seriesName === 'K线');
          if (!candle || !Array.isArray(candle.data)) return '';
          const i = candle.dataIndex;
          const k = klines[i];
          if (!k) return '';
          const up = k.close >= k.open;
          const color = up ? COLOR_UP : COLOR_DOWN;
          const fmt = (n: number) => n.toFixed(2);
          const fmtVol = (n: number) =>
            n >= 1e8 ? (n / 1e8).toFixed(2) + '亿' :
            n >= 1e4 ? (n / 1e4).toFixed(2) + '万' :
            n.toString();
          return `
            <div style="font-size:12px">
              <div style="margin-bottom:4px;color:${TEXT_MUTED}">${k.date}</div>
              <div>开盘: <span style="color:${color}">${fmt(k.open)}</span></div>
              <div>收盘: <span style="color:${color}">${fmt(k.close)}</span></div>
              <div>最高: <span style="color:${color}">${fmt(k.high)}</span></div>
              <div>最低: <span style="color:${color}">${fmt(k.low)}</span></div>
              <div>成交量: ${fmtVol(k.volume)}</div>
            </div>
          `;
        },
      },
      axisPointer: {
        link: [{ xAxisIndex: 'all' }],
      },
      grid: [
        { left: 60, right: 24, top: 50, height: '60%' },
        { left: 60, right: 24, top: '74%', height: '16%' },
      ],
      xAxis: [
        {
          type: 'category',
          data: dates,
          boundaryGap: false,
          axisLine: { lineStyle: { color: BORDER } },
          axisLabel: { color: TEXT_MUTED },
          splitLine: { show: false },
        },
        {
          type: 'category',
          gridIndex: 1,
          data: dates,
          boundaryGap: false,
          axisLine: { lineStyle: { color: BORDER } },
          axisLabel: { show: false },
          axisTick: { show: false },
          splitLine: { show: false },
        },
      ],
      yAxis: [
        {
          scale: true,
          splitArea: { show: false },
          axisLine: { lineStyle: { color: BORDER } },
          axisLabel: { color: TEXT_MUTED },
          splitLine: { lineStyle: { color: BORDER, opacity: 0.3 } },
        },
        {
          scale: true,
          gridIndex: 1,
          splitNumber: 2,
          axisLine: { lineStyle: { color: BORDER } },
          axisLabel: { color: TEXT_MUTED },
          splitLine: { show: false },
        },
      ],
      dataZoom: [
        {
          type: 'inside',
          xAxisIndex: [0, 1],
          start: 60,
          end: 100,
        },
        {
          show: true,
          type: 'slider',
          xAxisIndex: [0, 1],
          bottom: 10,
          height: 18,
          start: 60,
          end: 100,
          borderColor: BORDER,
          textStyle: { color: TEXT_MUTED },
          fillerColor: 'rgba(37, 99, 235, 0.15)',
          handleStyle: { color: '#2563eb' },
        },
      ],
      series: [
        {
          name: 'K线',
          type: 'candlestick',
          data: candleData,
          itemStyle: {
            color: COLOR_UP,
            color0: COLOR_DOWN,
            borderColor: COLOR_UP,
            borderColor0: COLOR_DOWN,
          },
        },
        {
          name: '成交量',
          type: 'bar',
          xAxisIndex: 1,
          yAxisIndex: 1,
          data: volumes,
        },
      ],
    };

    chartRef.current.setOption(option, true);
  }, [symbol, klines]);

  return <div ref={containerRef} style={{ width: '100%', height: 600 }} />;
}
