import { useEffect, useRef } from 'react';
import * as echarts from 'echarts';
import { useT } from '../i18n';
import type { KlineResponse, IndicatorResponse, ChartVisibility, IndicatorConfig } from '../types';

const COLOR_UP     = '#ef4444';
const COLOR_DOWN   = '#22c55e';
const TEXT_PRIMARY = '#e6edf3';
const TEXT_MUTED   = '#8b949e';
const BORDER       = '#30363d';

const MA_META = [
  { name: 'MA5',  color: '#f5c842' },
  { name: 'MA10', color: '#4a90d9' },
  { name: 'MA20', color: '#9b59b6' },
  { name: 'MA30', color: '#2ecc71' },
  { name: 'MA60', color: '#f97316' },
] as const;

const GRIDS = [
  { left: 70, right: 10, top: 50,  height: 315 },
  { left: 70, right: 10, top: 395, height: 72  },
  { left: 70, right: 10, top: 490, height: 100 },
  { left: 70, right: 10, top: 615, height: 58  },
];

const fmt2   = (v: number | null) => (v != null ? v.toFixed(2) : '—');
const fmtVol = (n: number) =>
  n >= 1e8 ? (n / 1e8).toFixed(2) + '亿' :
  n >= 1e4 ? (n / 1e4).toFixed(2) + '万' :
  String(n);

interface Props {
  symbol: string;
  klines: KlineResponse[];
  indicators: IndicatorResponse;
  visibility: ChartVisibility;
  config: IndicatorConfig;
  onConfigChange: (config: IndicatorConfig) => void;
}

export default function KlineChart({ symbol, klines, indicators, visibility, config, onConfigChange }: Props) {
  const { t } = useT();
  const containerRef = useRef<HTMLDivElement | null>(null);
  const chartRef     = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;
    chartRef.current = echarts.init(containerRef.current, undefined, { renderer: 'canvas' });
    const onResize = () => chartRef.current?.resize();
    window.addEventListener('resize', onResize);
    return () => {
      window.removeEventListener('resize', onResize);
      chartRef.current?.dispose();
      chartRef.current = null;
    };
  }, []);

  useEffect(() => {
    const chart = chartRef.current;
    if (!chart || klines.length === 0) return;
    if (!indicators?.macd || !indicators?.ma || !indicators?.rsi || !indicators?.boll) return;

    const dates      = klines.map(k => k.date);
    const candleData = klines.map(k => [k.open, k.close, k.low, k.high]);
    const volumes    = klines.map(k => ({
      value: k.volume,
      itemStyle: { color: k.close >= k.open ? COLOR_UP : COLOR_DOWN },
    }));

    const { macd, ma, rsi, boll } = indicators;
    const maDataMap: Record<string, (number | null)[]> = {
      MA5: ma.ma5List, MA10: ma.ma10List, MA20: ma.ma20List,
      MA30: ma.ma30List, MA60: ma.ma60List,
    };
    const macdBarData = macd.macdList.map(v =>
      v == null ? null : { value: v, itemStyle: { color: v >= 0 ? COLOR_UP : COLOR_DOWN } }
    );

    // legend.selected computed from visibility × config
    const selected: Record<string, boolean> = {
      'K线':     visibility.kline,
      'Volume':  visibility.volume,
      'BOLL上轨': visibility.boll && config.bollLines.includes('upper'),
      'BOLL中轨': visibility.boll && config.bollLines.includes('middle'),
      'BOLL下轨': visibility.boll && config.bollLines.includes('lower'),
      'DIF':     visibility.macd && config.macdLines.includes('dif'),
      'DEA':     visibility.macd && config.macdLines.includes('dea'),
      'MACD柱':  visibility.macd && config.macdLines.includes('hist'),
      'RSI6':    visibility.rsi,
    };
    MA_META.forEach(c => { selected[c.name] = visibility.ma && config.maLines.includes(c.name); });

    const buildTooltip = (raw: unknown): string => {
      const arr = raw as Array<{ seriesName: string; data: unknown; dataIndex: number }>;
      if (!arr.length) return '';
      const idx = arr[0].dataIndex;
      const k = klines[idx];
      if (!k) return '';

      const getVal = (name: string): number | null => {
        const p = arr.find(x => x.seriesName === name);
        if (!p || p.data == null) return null;
        const d = p.data;
        if (typeof d === 'number') return d;
        if (typeof d === 'object' && !Array.isArray(d))
          return (d as { value?: number }).value ?? null;
        return null;
      };

      const col  = k.close >= k.open ? COLOR_UP : COLOR_DOWN;
      const pct  = ((k.close - k.open) / k.open * 100).toFixed(2);
      const sign = k.close >= k.open ? '+' : '';

      return `
        <div style="font-size:12px;line-height:1.9;min-width:220px;padding:2px 4px">
          <div style="color:${TEXT_MUTED};margin-bottom:2px">${k.date}</div>
          <div>
            ${t.tooltip.open} <b style="color:${col}">${fmt2(k.open)}</b>&nbsp;
            ${t.tooltip.close} <b style="color:${col}">${fmt2(k.close)}</b>
            <span style="color:${col}">&nbsp;${sign}${pct}%</span>
          </div>
          <div>
            ${t.tooltip.high} <b style="color:${col}">${fmt2(k.high)}</b>&nbsp;
            ${t.tooltip.low} <b style="color:${col}">${fmt2(k.low)}</b>
          </div>
          <div style="color:${TEXT_MUTED}">${t.tooltip.volume} ${fmtVol(k.volume)}</div>
          <div style="border-top:1px solid ${BORDER};margin:3px 0"></div>
          <div>
            <span style="color:#f5c842">MA5</span> ${fmt2(getVal('MA5'))}&nbsp;
            <span style="color:#4a90d9">MA10</span> ${fmt2(getVal('MA10'))}&nbsp;
            <span style="color:#9b59b6">MA20</span> ${fmt2(getVal('MA20'))}
          </div>
          <div>
            <span style="color:#2ecc71">MA30</span> ${fmt2(getVal('MA30'))}&nbsp;
            <span style="color:#f97316">MA60</span> ${fmt2(getVal('MA60'))}
          </div>
          <div>
            <span style="color:#6b7280">${t.tooltip.boll}</span>
            ${fmt2(getVal('BOLL上轨'))} / ${fmt2(getVal('BOLL中轨'))} / ${fmt2(getVal('BOLL下轨'))}
          </div>
          <div style="border-top:1px solid ${BORDER};margin:3px 0"></div>
          <div>
            <span style="color:#4a90d9">${t.indicator.macdDif}</span> ${fmt2(getVal('DIF'))}&nbsp;
            <span style="color:#f5c842">${t.indicator.macdDea}</span> ${fmt2(getVal('DEA'))}&nbsp;
            <span style="color:${TEXT_MUTED}">MACD</span> ${fmt2(getVal('MACD柱'))}
          </div>
          <div><span style="color:#f5c842">RSI6</span> ${fmt2(getVal('RSI6'))}</div>
        </div>`;
    };

    const xAxisCfg = (gridIndex: number) => ({
      type: 'category' as const,
      gridIndex,
      data: dates,
      boundaryGap: false,
      axisLine:  { lineStyle: { color: BORDER } },
      axisTick:  { show: gridIndex === 0, lineStyle: { color: BORDER } },
      axisLabel: gridIndex === 0
        ? { color: TEXT_MUTED, fontSize: 11 }
        : { show: false },
      splitLine: { show: false },
    });

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const option: any = {
      backgroundColor: 'transparent',
      animation: false,

      legend: {
        show: false,
        data: [
          'K线', ...MA_META.map(c => c.name),
          'BOLL上轨', 'BOLL中轨', 'BOLL下轨',
          'Volume', 'DIF', 'DEA', 'MACD柱', 'RSI6',
        ],
        selected,
      },

      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross', link: [{ xAxisIndex: 'all' }] },
        backgroundColor: '#1c2128',
        borderColor: BORDER,
        borderWidth: 1,
        textStyle: { color: TEXT_PRIMARY, fontSize: 12 },
        formatter: buildTooltip,
        confine: true,
      },

      axisPointer: {
        link: [{ xAxisIndex: 'all' }],
        label: { backgroundColor: '#2d333b', fontSize: 10 },
      },

      grid: GRIDS,

      xAxis: [0, 1, 2, 3].map(xAxisCfg),

      yAxis: [
        {
          gridIndex: 0,
          scale: true,
          splitArea: { show: false },
          axisLine:  { lineStyle: { color: BORDER } },
          axisLabel: { color: TEXT_MUTED, fontSize: 11 },
          splitLine: { lineStyle: { color: BORDER, opacity: 0.3 } },
        },
        {
          gridIndex: 1,
          scale: true,
          splitNumber: 2,
          axisLine:  { lineStyle: { color: BORDER } },
          axisLabel: {
            color: TEXT_MUTED,
            fontSize: 10,
            formatter: (v: number) =>
              v >= 1e6 ? (v / 1e6).toFixed(1) + 'M' :
              v >= 1e3 ? (v / 1e3).toFixed(1) + 'K' : String(v),
          },
          splitLine: { show: false },
        },
        {
          gridIndex: 2,
          scale: true,
          splitNumber: 3,
          axisLine:  { lineStyle: { color: BORDER } },
          axisLabel: { color: TEXT_MUTED, fontSize: 10 },
          splitLine: { lineStyle: { color: BORDER, opacity: 0.3 } },
        },
        {
          gridIndex: 3,
          min: 0,
          max: 100,
          splitNumber: 2,
          axisLine:  { lineStyle: { color: BORDER } },
          axisLabel: { color: TEXT_MUTED, fontSize: 10 },
          splitLine: { lineStyle: { color: BORDER, opacity: 0.3 } },
        },
      ],

      dataZoom: [
        { type: 'inside', xAxisIndex: [0, 1, 2, 3], start: 60, end: 100 },
        {
          type: 'slider',
          xAxisIndex: [0, 1, 2, 3],
          bottom: 6,
          height: 18,
          start: 60,
          end: 100,
          borderColor: BORDER,
          textStyle: { color: TEXT_MUTED, fontSize: 10 },
          fillerColor: 'rgba(37,99,235,0.15)',
          handleStyle: { color: '#2563eb' },
        },
      ],

      series: [
        {
          name: 'K线',
          type: 'candlestick',
          xAxisIndex: 0,
          yAxisIndex: 0,
          data: candleData,
          itemStyle: {
            color: COLOR_UP, color0: COLOR_DOWN,
            borderColor: COLOR_UP, borderColor0: COLOR_DOWN,
          },
        },
        ...MA_META.map(c => ({
          name: c.name,
          type: 'line',
          xAxisIndex: 0,
          yAxisIndex: 0,
          data: maDataMap[c.name],
          symbol: 'none',
          lineStyle: { color: c.color, width: 1 },
        })),
        ...(['BOLL上轨', 'BOLL中轨', 'BOLL下轨'] as const).map((name, i) => ({
          name,
          type: 'line',
          xAxisIndex: 0,
          yAxisIndex: 0,
          data: [boll.upperList, boll.middleList, boll.lowerList][i],
          symbol: 'none',
          lineStyle: { color: '#6b7280', type: 'dashed', width: 1 },
        })),
        {
          name: 'Volume',
          type: 'bar',
          xAxisIndex: 1,
          yAxisIndex: 1,
          data: volumes,
          barMaxWidth: 8,
        },
        {
          name: 'DIF',
          type: 'line',
          xAxisIndex: 2,
          yAxisIndex: 2,
          data: macd.difList,
          symbol: 'none',
          lineStyle: { color: '#4a90d9', width: 1 },
        },
        {
          name: 'DEA',
          type: 'line',
          xAxisIndex: 2,
          yAxisIndex: 2,
          data: macd.deaList,
          symbol: 'none',
          lineStyle: { color: '#f5c842', width: 1 },
        },
        {
          name: 'MACD柱',
          type: 'bar',
          xAxisIndex: 2,
          yAxisIndex: 2,
          data: macdBarData,
          barMaxWidth: 6,
        },
        {
          name: 'RSI6',
          type: 'line',
          xAxisIndex: 3,
          yAxisIndex: 3,
          data: rsi.rsi6List,
          symbol: 'none',
          lineStyle: { color: '#f5c842', width: 1.5 },
          markLine: {
            silent: true,
            symbol: ['none', 'none'],
            lineStyle: { color: '#4b5563', type: 'dashed', width: 1 },
            label: { show: true, formatter: '{c}', color: '#6b7280', fontSize: 10, position: 'end' },
            data: [{ yAxis: 20 }, { yAxis: 80 }],
          },
        },
      ],
    };

    chart.setOption(option, true);
  }, [symbol, klines, indicators, visibility, config, t]);

  const toggleMaLine = (line: string) => {
    const next = config.maLines.includes(line)
      ? config.maLines.filter(l => l !== line)
      : [...config.maLines, line];
    onConfigChange({ ...config, maLines: next });
  };

  const toggleBollLine = (line: string) => {
    const next = config.bollLines.includes(line)
      ? config.bollLines.filter(l => l !== line)
      : [...config.bollLines, line];
    onConfigChange({ ...config, bollLines: next });
  };

  const toggleMacdLine = (line: string) => {
    const next = config.macdLines.includes(line)
      ? config.macdLines.filter(l => l !== line)
      : [...config.macdLines, line];
    onConfigChange({ ...config, macdLines: next });
  };

  return (
    <div style={{ position: 'relative', width: '100%', height: 720 }}>
      <div ref={containerRef} style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }} />
      {klines.length > 0 && (
        <div className="chart-config-overlay">
          <span className="config-symbol">{symbol}</span>
          {visibility.ma && (
            <span className="config-group">
              <span className="config-label">MA</span>
              {MA_META.map(c => (
                <button
                  key={c.name}
                  className={`config-chip${config.maLines.includes(c.name) ? ' active' : ''}`}
                  style={config.maLines.includes(c.name) ? { borderColor: c.color, color: c.color } : {}}
                  onClick={() => toggleMaLine(c.name)}
                >
                  {c.name}
                </button>
              ))}
            </span>
          )}
          {visibility.boll && (
            <span className="config-group">
              <span className="config-label">BOLL</span>
              {[
                { key: 'upper',  label: t.indicator.bollUpper  },
                { key: 'middle', label: t.indicator.bollMiddle },
                { key: 'lower',  label: t.indicator.bollLower  },
              ].map(({ key, label }) => (
                <button
                  key={key}
                  className={`config-chip${config.bollLines.includes(key) ? ' active' : ''}`}
                  onClick={() => toggleBollLine(key)}
                >
                  {label}
                </button>
              ))}
            </span>
          )}
          {visibility.macd && (
            <span className="config-group">
              <span className="config-label">MACD</span>
              {[
                { key: 'dif',  label: t.indicator.macdDif  },
                { key: 'dea',  label: t.indicator.macdDea  },
                { key: 'hist', label: t.indicator.macdHist },
              ].map(({ key, label }) => (
                <button
                  key={key}
                  className={`config-chip${config.macdLines.includes(key) ? ' active' : ''}`}
                  onClick={() => toggleMacdLine(key)}
                >
                  {label}
                </button>
              ))}
            </span>
          )}
        </div>
      )}
    </div>
  );
}
