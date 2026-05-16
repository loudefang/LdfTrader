CREATE EXTENSION IF NOT EXISTS timescaledb;
SET timezone = 'America/New_York';

CREATE TABLE IF NOT EXISTS kline_daily (
    time TIMESTAMPTZ NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    market VARCHAR(10) NOT NULL,
    open DOUBLE PRECISION,
    high DOUBLE PRECISION,
    low DOUBLE PRECISION,
    close DOUBLE PRECISION,
    volume BIGINT
);

SELECT create_hypertable('kline_daily', 'time', if_not_exists => TRUE);
CREATE UNIQUE INDEX IF NOT EXISTS idx_kline_daily_symbol_time
    ON kline_daily (symbol, market, time DESC);

-- 可选: 周K、月K 表结构相同
CREATE TABLE IF NOT EXISTS kline_weekly (LIKE kline_daily INCLUDING ALL);
SELECT create_hypertable('kline_weekly', 'time', if_not_exists => TRUE);

CREATE TABLE IF NOT EXISTS kline_monthly (LIKE kline_daily INCLUDING ALL);
SELECT create_hypertable('kline_monthly', 'time', if_not_exists => TRUE);
