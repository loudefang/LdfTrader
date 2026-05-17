"""
AKShare bridge service.

Exposes a thin HTTP layer over AKShare so the Java market-data-service can
fetch A股 / 港股 daily/weekly/monthly OHLCV bars in the same shape that the
FMP adapter already returns.

Endpoint
--------
GET /historical?symbol=600519&market=cn&period=daily&start=2024-01-01&end=2024-12-31

Response: a JSON array of bars, ordered by date ascending:
    [{ "date": "2024-01-02", "open": 1700.0, "high": 1720.5,
       "low": 1690.0, "close": 1710.3, "volume": 12345678 }, ...]
"""
from __future__ import annotations

import logging
from datetime import date
from typing import Literal

import akshare as ak
import pandas as pd
from fastapi import FastAPI, HTTPException, Query

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
log = logging.getLogger("akshare-bridge")

app = FastAPI(title="akshare-bridge", version="1.0.0")

Market = Literal["cn", "hk"]
Period = Literal["daily", "weekly", "monthly"]


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "akshare": ak.__version__}


@app.get("/historical")
def historical(
    symbol: str = Query(..., description="A股6位代码 / 港股5位代码"),
    market: Market = Query(..., description="cn=A股, hk=港股"),
    period: Period = Query("daily", description="daily / weekly / monthly"),
    start: date = Query(..., description="开始日期 (YYYY-MM-DD)"),
    end: date = Query(..., description="结束日期 (YYYY-MM-DD)"),
) -> list[dict]:
    start_str = start.strftime("%Y%m%d")
    end_str = end.strftime("%Y%m%d")
    log.info(
        "[AKSHARE CALL] market=%s symbol=%s period=%s start=%s end=%s",
        market, symbol, period, start_str, end_str,
    )

    try:
        if market == "cn":
            df = ak.stock_zh_a_hist(
                symbol=symbol,
                period=period,
                start_date=start_str,
                end_date=end_str,
                adjust="qfq",
            )
        else:  # hk
            df = ak.stock_hk_hist(
                symbol=symbol,
                period=period,
                start_date=start_str,
                end_date=end_str,
                adjust="qfq",
            )
    except Exception as e:
        log.exception("[AKSHARE ERROR] market=%s symbol=%s", market, symbol)
        raise HTTPException(status_code=502, detail=f"AKShare 调用失败: {e}") from e

    if df is None or df.empty:
        log.warning("[AKSHARE EMPTY] market=%s symbol=%s", market, symbol)
        return []

    bars = _to_bars(df)
    log.info("[AKSHARE OK] market=%s symbol=%s rows=%d", market, symbol, len(bars))
    return bars


_COLUMN_ALIASES = {
    "日期": "date",
    "开盘": "open",
    "最高": "high",
    "最低": "low",
    "收盘": "close",
    "成交量": "volume",
}


def _to_bars(df: pd.DataFrame) -> list[dict]:
    df = df.rename(columns=_COLUMN_ALIASES)
    required = {"date", "open", "high", "low", "close", "volume"}
    missing = required - set(df.columns)
    if missing:
        raise HTTPException(
            status_code=500,
            detail=f"AKShare 返回缺少字段: {sorted(missing)}; 拿到: {list(df.columns)}",
        )

    df = df[["date", "open", "high", "low", "close", "volume"]].copy()
    df["date"] = pd.to_datetime(df["date"]).dt.strftime("%Y-%m-%d")
    df = df.sort_values("date").reset_index(drop=True)

    bars: list[dict] = []
    for row in df.itertuples(index=False):
        bars.append({
            "date": row.date,
            "open": float(row.open),
            "high": float(row.high),
            "low": float(row.low),
            "close": float(row.close),
            "volume": int(row.volume) if pd.notna(row.volume) else 0,
        })
    return bars


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8186, reload=False)
