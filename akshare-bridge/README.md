# akshare-bridge

中国市场行情数据旁车服务，使用 [AKShare](https://akshare.akfamily.xyz/) 拉取 A股 / 港股
历史 K 线，并以与 `FmpMarketDataProvider` 对齐的 JSON 格式返回，供 Java
`market-data-service` 通过 HTTP 调用。

- 端口: **8186** (可用 `AKSHARE_BRIDGE_PORT` 覆盖)
- 不需要任何 API Key，AKShare 直接抓取公开行情接口。
- 依赖: 见 `requirements.txt`

## 启动方式

`./scripts/start.sh` 已经会自动拉起。若想手动调试:

```bash
cd akshare-bridge
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python main.py
```

## 接口

```
GET /health
GET /historical?symbol=600519&market=cn&period=daily&start=2024-01-01&end=2024-12-31
```

返回:

```json
[
  {"date": "2024-01-02", "open": 1700.0, "high": 1720.5,
   "low": 1690.0, "close": 1710.3, "volume": 12345678},
  ...
]
```

`market` 取值:
- `cn` — A股 (6位代码, 例: 600519、000001、300750)
- `hk` — 港股 (5位代码, 例: 00700、09988)

`period` 取值: `daily` / `weekly` / `monthly`，K 线均为前复权 (`qfq`).
