# LdfTrader

量化交易平台：FMP 行情数据 + TimescaleDB 本地缓存 + Spring Boot 微服务 + React 前端。

```
┌──────────────┐     ┌──────────────┐     ┌────────────────────┐     ┌──────────────┐
│   web-app    │ ──> │ web-service  │ ──> │ market-data-service│ ──> │  FMP API     │
│  React :3000 │     │ Boot   :8181 │     │ Boot         :8182 │     │              │
└──────────────┘     └──────────────┘     └──────────┬─────────┘     └──────────────┘
                                                     │
                                                     ▼
                                          ┌─────────────────────┐
                                          │ TimescaleDB (Docker)│
                                          │             :5432   │
                                          └─────────────────────┘
```

## 模块概览

| 模块 | 角色 | 端口 |
|---|---|---|
| `trading-common` | 共享 DTO (KlineResponse, ErrorResponse) | — |
| `strategy-core` | 策略引擎核心 (占位) | — |
| `market-data-service` | FMP 接入 + TimescaleDB 缓存 | 8182 |
| `web-service` | 前端 BFF 聚合层 | 8181 |
| `web-app` | React 前端 (Vite + ECharts) | 3000 |

---

## 一键启动 / 关闭

```bash
# 启动全部 (DB + 后端 + 前端)
./scripts/start.sh

# 启动后端但不启动前端
./scripts/start.sh --no-frontend

# 跳过 Maven 编译 (假定已编译)
./scripts/start.sh --skip-build

# 查看运行状态
./scripts/status.sh

# 关闭后端 + 前端 (保留 DB 容器)
./scripts/stop.sh

# 关闭所有 (含 TimescaleDB 容器)
./scripts/stop.sh --all

# 关闭并删除容器 (数据卷 trading-pgdata 保留)
./scripts/stop.sh --purge
```

启动日志写入 `logs/`，进程 PID 写入 `.pids/`，两个目录已加入 `.gitignore`。

---

## 首次配置

```bash
# 1. 填入 FMP API Key
vi .env
#    FMP_API_KEY=你的key   <-- 必填

# 2. 一键启动 (首次会自动 docker run、mvn install、npm install)
./scripts/start.sh
```

打开浏览器访问 <http://localhost:3000>，输入 `TSLA` 点击查询。

---

## 常用命令

### 数据库

```bash
# 进入 psql
docker exec -it trading-timescaledb psql -U trading -d trading_platform

# 查看已缓存的股票
docker exec -it trading-timescaledb psql -U trading -d trading_platform \
  -c "SELECT symbol, market, COUNT(*) FROM kline_daily GROUP BY symbol, market ORDER BY 1;"

# 手动重新执行初始化 SQL
docker exec -i trading-timescaledb psql -U trading -d trading_platform \
  < db/init/01_init_kline.sql

# 清空某只股票缓存 (排错用)
docker exec -it trading-timescaledb psql -U trading -d trading_platform \
  -c "DELETE FROM kline_daily WHERE symbol='TSLA';"
```

### Maven

```bash
# 全量编译 (跳过测试)
mvn -DskipTests install

# 只编译 + 安装某个模块及其依赖
mvn -pl market-data-service -am -DskipTests install

# 单独启动某个 Spring Boot 服务
mvn -pl market-data-service spring-boot:run
mvn -pl web-service         spring-boot:run

# 清理
mvn clean
```

### 前端

```bash
cd web-app
npm install          # 首次或依赖变更后
npm run dev          # 开发模式, :3000
npm run build        # 生产构建
npm run preview      # 预览生产构建
npm run lint         # tsc --noEmit 类型检查
```

### 日志

```bash
tail -f logs/market-data-service.log    # 看缓存命中 / FMP 调用
tail -f logs/web-service.log            # 看 BFF 请求日志
tail -f logs/web-app.log                # 看 Vite 输出
```

### API 直连测试

```bash
# 通过 BFF (推荐, 经过 CORS)
curl "http://localhost:8181/api/web/kline?symbol=TSLA&startDate=2024-01-01&endDate=2024-03-31"

# 直连 market-data-service
curl "http://localhost:8182/api/market-data/kline?symbol=AAPL&period=daily"

# 周 K / 月 K
curl "http://localhost:8182/api/market-data/kline?symbol=NVDA&period=weekly"
curl "http://localhost:8182/api/market-data/kline?symbol=NVDA&period=monthly"
```

### Docker

```bash
# 仅启动 TimescaleDB (首次)
docker run -d --name trading-timescaledb \
  -p 5432:5432 \
  -e POSTGRES_DB=trading_platform \
  -e POSTGRES_USER=trading \
  -e POSTGRES_PASSWORD=trading123 \
  -v trading-pgdata:/var/lib/postgresql/data \
  timescale/timescaledb:latest-pg16

docker start trading-timescaledb     # 启动已有容器
docker stop  trading-timescaledb     # 停止
docker rm    trading-timescaledb     # 删除容器 (数据卷保留)
docker volume rm trading-pgdata      # 彻底清空数据 (谨慎!)

docker logs -f trading-timescaledb   # 看 DB 日志
```

---

## 故障排查

| 现象 | 处理 |
|---|---|
| 启动脚本报 `FMP_API_KEY is missing` | 编辑 `.env`，填入 [financialmodelingprep.com](https://financialmodelingprep.com/) 申请的 key |
| 浏览器报 CORS 错误 | 确认前端跑在 `:3000`（CORS 白名单写死），不要用 `127.0.0.1` |
| 端口被占用 | `./scripts/stop.sh` 一键清理；或 `lsof -i :8181` 找出占用者 |
| `[FMP ERROR] status=429` | FMP 免费额度限流，等几分钟或升级套餐 |
| 反复查询同一区间但仍走 FMP | 检查 `kline_daily` 表数据；可能 `symbol/market` 大小写不一致 |
| `mvn` 报 Java 版本错误 | 需要 JDK 21；`java -version` 检查 |

---

## 目录结构

```
LdfTrader/
├── .env                          # 环境变量 (含 FMP_API_KEY)
├── pom.xml                       # 父 POM
├── README.md                     # 本文档
├── scripts/
│   ├── start.sh                  # 一键启动
│   ├── stop.sh                   # 一键关闭
│   └── status.sh                 # 状态查看
├── db/init/01_init_kline.sql     # K线表 / hypertable 初始化
├── trading-common/               # 共享 DTO
├── strategy-core/                # 策略核心 (占位)
├── market-data-service/          # 行情服务 :8182
├── web-service/                  # BFF      :8181
├── web-app/                      # React 前端 :3000
├── logs/                         # 启动脚本输出 (gitignored)
└── .pids/                        # 进程 PID 文件 (gitignored)
```
