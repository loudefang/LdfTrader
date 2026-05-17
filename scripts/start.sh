#!/usr/bin/env bash
# 一键启动 LdfTrader 全部服务
#   - TimescaleDB (Docker)
#   - market-data-service (Spring Boot, :8182)
#   - web-service        (Spring Boot, :8181)
#   - web-app            (Vite,        :3000)
#
# 用法:
#   ./scripts/start.sh              # 启动所有服务
#   ./scripts/start.sh --no-frontend  # 只启动后端 + 数据库
#   ./scripts/start.sh --skip-build   # 跳过 Maven 编译

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
PID_DIR="$ROOT_DIR/.pids"
MVN_SETTINGS="$SCRIPT_DIR/maven-settings.xml"
MVN_OPTS_ARGS=(-s "$MVN_SETTINGS" -gs "$MVN_SETTINGS")

mkdir -p "$LOG_DIR" "$PID_DIR"

# --- 参数解析 ---
START_FRONTEND=true
SKIP_BUILD=false
for arg in "$@"; do
    case "$arg" in
        --no-frontend) START_FRONTEND=false ;;
        --skip-build)  SKIP_BUILD=true ;;
        -h|--help)
            grep '^#' "$0" | head -20
            exit 0
            ;;
    esac
done

# --- 颜色输出 ---
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'
info()  { printf "${CYAN}[INFO]${NC}  %s\n"  "$*"; }
ok()    { printf "${GREEN}[ OK ]${NC}  %s\n"  "$*"; }
warn()  { printf "${YELLOW}[WARN]${NC}  %s\n"  "$*"; }
err()   { printf "${RED}[FAIL]${NC}  %s\n"    "$*"; }

# --- 加载 .env (仅用于本脚本判断, 各服务自己也会通过 spring-dotenv 读) ---
if [ -f "$ROOT_DIR/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT_DIR/.env"
    set +a
fi

DB_CONTAINER_NAME="${DB_CONTAINER_NAME:-trading-timescaledb}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-trading_platform}"
DB_USER="${DB_USER:-trading}"
DB_PASSWORD="${DB_PASSWORD:-trading123}"

# --- 1. TimescaleDB ---
start_timescaledb() {
    info "检查 TimescaleDB 容器 ($DB_CONTAINER_NAME)..."
    if ! command -v docker >/dev/null 2>&1; then
        err "未找到 docker 命令，请先安装 Docker。"
        exit 1
    fi

    if docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER_NAME}$"; then
        ok "TimescaleDB 已在运行"
    elif docker ps -a --format '{{.Names}}' | grep -q "^${DB_CONTAINER_NAME}$"; then
        info "启动已存在的容器 $DB_CONTAINER_NAME ..."
        docker start "$DB_CONTAINER_NAME" >/dev/null
        ok "TimescaleDB 已启动"
    else
        info "首次创建 TimescaleDB 容器..."
        docker run -d \
            --name "$DB_CONTAINER_NAME" \
            -p "${DB_PORT}:5432" \
            -e POSTGRES_DB="$DB_NAME" \
            -e POSTGRES_USER="$DB_USER" \
            -e POSTGRES_PASSWORD="$DB_PASSWORD" \
            -v trading-pgdata:/var/lib/postgresql/data \
            timescale/timescaledb:latest-pg16 >/dev/null
        ok "TimescaleDB 容器已创建并启动"
    fi

    info "等待数据库就绪..."
    for i in {1..30}; do
        if docker exec "$DB_CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
            ok "数据库就绪"
            break
        fi
        sleep 1
        if [ "$i" = "30" ]; then
            err "数据库 30 秒内未就绪，请检查 docker logs $DB_CONTAINER_NAME"
            exit 1
        fi
    done

    # 初始化建表 (幂等)
    local check_sql="SELECT to_regclass('public.kline_daily');"
    local exists
    exists=$(docker exec "$DB_CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -tAc "$check_sql" 2>/dev/null || echo "")
    if [ -z "$exists" ] || [ "$exists" = "" ]; then
        info "初始化数据库 schema..."
        docker exec -i "$DB_CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" \
            < "$ROOT_DIR/db/init/01_init_kline.sql" >/dev/null
        ok "schema 初始化完成"
    else
        ok "schema 已存在，跳过初始化"
    fi

    local check_ind_sql="SELECT to_regclass('public.ma_daily');"
    local ind_exists
    ind_exists=$(docker exec "$DB_CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -tAc "$check_ind_sql" 2>/dev/null || echo "")
    if [ -z "$ind_exists" ] || [ "$ind_exists" = "" ]; then
        info "初始化 indicators schema..."
        docker exec -i "$DB_CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" \
            < "$ROOT_DIR/db/init/02_init_indicators.sql" >/dev/null
        ok "indicators schema 初始化完成"
    else
        ok "indicators schema 已存在，跳过初始化"
    fi
}

# --- 2. Maven 编译 ---
maven_build() {
    if [ "$SKIP_BUILD" = "true" ]; then
        warn "跳过 Maven 编译 (--skip-build)"
        return
    fi
    info "Maven 编译 (mvn install -DskipTests)..."
    (cd "$ROOT_DIR" && mvn "${MVN_OPTS_ARGS[@]}" -q -DskipTests install)
    ok "Maven 编译完成"
}

# --- 3. 启动 Spring Boot 服务 ---
start_spring_service() {
    local module="$1"
    local port="$2"
    local pid_file="$PID_DIR/${module}.pid"
    local log_file="$LOG_DIR/${module}.log"

    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
        warn "$module 已在运行 (PID $(cat "$pid_file"))，跳过"
        return
    fi

    info "启动 $module (端口 $port)..."
    (
        cd "$ROOT_DIR"
        nohup mvn "${MVN_OPTS_ARGS[@]}" -pl "$module" -q spring-boot:run \
            > "$log_file" 2>&1 &
        echo $! > "$pid_file"
    )
    local pid
    pid=$(cat "$pid_file")
    info "$module 进程 PID=$pid, 日志: $log_file"

    info "等待 $module 端口 $port 就绪..."
    for i in {1..90}; do
        if lsof -iTCP:"$port" -sTCP:LISTEN -P -n >/dev/null 2>&1; then
            ok "$module 就绪 (http://localhost:$port)"
            return
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            err "$module 启动失败，请查看日志: $log_file"
            exit 1
        fi
        sleep 1
    done
    err "$module 90 秒内未监听端口 $port，请查看日志: $log_file"
    exit 1
}

# --- 3.5. 启动 AKShare Python 旁车 (A股/港股 行情源) ---
start_akshare_bridge() {
    local pid_file="$PID_DIR/akshare-bridge.pid"
    local log_file="$LOG_DIR/akshare-bridge.log"
    local bridge_dir="$ROOT_DIR/akshare-bridge"
    local venv_dir="$bridge_dir/.venv"
    local port="${AKSHARE_BRIDGE_PORT:-8186}"

    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
        warn "akshare-bridge 已在运行 (PID $(cat "$pid_file"))，跳过"
        return
    fi

    if ! command -v python3 >/dev/null 2>&1; then
        err "未找到 python3 命令，无法启动 akshare-bridge (A股/港股将不可用)"
        return
    fi

    if [ ! -d "$venv_dir" ]; then
        info "首次运行，为 akshare-bridge 创建 venv 并安装依赖..."
        (cd "$bridge_dir" && python3 -m venv .venv)
        "$venv_dir/bin/pip" install --upgrade pip >/dev/null
        "$venv_dir/bin/pip" install -r "$bridge_dir/requirements.txt"
        ok "akshare-bridge 依赖安装完成"
    fi

    info "启动 akshare-bridge (端口 $port)..."
    (
        cd "$bridge_dir"
        nohup "$venv_dir/bin/python" -m uvicorn main:app \
            --host 0.0.0.0 --port "$port" \
            > "$log_file" 2>&1 &
        echo $! > "$pid_file"
    )
    local pid
    pid=$(cat "$pid_file")
    info "akshare-bridge 进程 PID=$pid, 日志: $log_file"

    info "等待 akshare-bridge 端口 $port 就绪..."
    for i in {1..60}; do
        if lsof -iTCP:"$port" -sTCP:LISTEN -P -n >/dev/null 2>&1; then
            ok "akshare-bridge 就绪 (http://localhost:$port)"
            return
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            err "akshare-bridge 启动失败，请查看日志: $log_file"
            return
        fi
        sleep 1
    done
    err "akshare-bridge 60 秒内未监听端口 $port，请查看日志: $log_file"
}

# --- 4. 启动前端 ---
start_frontend() {
    local pid_file="$PID_DIR/web-app.pid"
    local log_file="$LOG_DIR/web-app.log"
    local app_dir="$ROOT_DIR/web-app"

    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
        warn "web-app 已在运行 (PID $(cat "$pid_file"))，跳过"
        return
    fi

    if [ ! -d "$app_dir/node_modules" ]; then
        info "首次运行，执行 npm install..."
        (cd "$app_dir" && npm install)
    fi

    info "启动 web-app (端口 3000)..."
    (
        cd "$app_dir"
        nohup npm run dev > "$log_file" 2>&1 &
        echo $! > "$pid_file"
    )
    local pid
    pid=$(cat "$pid_file")
    info "web-app 进程 PID=$pid, 日志: $log_file"

    info "等待 web-app 端口 3000 就绪..."
    for i in {1..60}; do
        if lsof -iTCP:3000 -sTCP:LISTEN -P -n >/dev/null 2>&1; then
            ok "web-app 就绪 (http://localhost:3000)"
            return
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            err "web-app 启动失败，请查看日志: $log_file"
            exit 1
        fi
        sleep 1
    done
    err "web-app 60 秒内未监听端口 3000，请查看日志: $log_file"
    exit 1
}

# --- 执行 ---
echo ""
echo "========================================="
echo "  LdfTrader  启动"
echo "========================================="
start_timescaledb
maven_build
start_akshare_bridge
start_spring_service "market-data-service" 8182
start_spring_service "indicator-service"   8183
start_spring_service "backtest-service"    8185
start_spring_service "web-service"         8181
if [ "$START_FRONTEND" = "true" ]; then
    start_frontend
fi

echo ""
echo "========================================="
ok "全部服务启动完成"
echo "========================================="
echo "  TimescaleDB     : localhost:${DB_PORT}"
echo "  akshare-bridge  : http://localhost:${AKSHARE_BRIDGE_PORT:-8186}  (A股/港股)"
echo "  market-data     : http://localhost:8182"
echo "  indicator       : http://localhost:8183"
echo "  backtest        : http://localhost:8185"
echo "  web-service     : http://localhost:8181"
if [ "$START_FRONTEND" = "true" ]; then
    echo "  web-app     : http://localhost:3000"
fi
echo ""
echo "  日志目录    : $LOG_DIR"
echo "  关闭命令    : ./scripts/stop.sh"
echo "========================================="
