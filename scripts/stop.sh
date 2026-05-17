#!/usr/bin/env bash
# 一键关闭 LdfTrader 全部服务
#
# 用法:
#   ./scripts/stop.sh           # 关闭后端 + 前端 (保留 TimescaleDB 容器运行)
#   ./scripts/stop.sh --all     # 同时停止 TimescaleDB 容器
#   ./scripts/stop.sh --purge   # 同时停止并删除容器 (数据卷保留)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PID_DIR="$ROOT_DIR/.pids"

STOP_DB=false
PURGE_DB=false
for arg in "$@"; do
    case "$arg" in
        --all)    STOP_DB=true ;;
        --purge)  STOP_DB=true; PURGE_DB=true ;;
        -h|--help)
            grep '^#' "$0" | head -10
            exit 0
            ;;
    esac
done

GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'
info()  { printf "${CYAN}[INFO]${NC}  %s\n"  "$*"; }
ok()    { printf "${GREEN}[ OK ]${NC}  %s\n"  "$*"; }
warn()  { printf "${YELLOW}[WARN]${NC}  %s\n"  "$*"; }
err()   { printf "${RED}[FAIL]${NC}  %s\n"    "$*"; }

if [ -f "$ROOT_DIR/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT_DIR/.env"
    set +a
fi
DB_CONTAINER_NAME="${DB_CONTAINER_NAME:-trading-timescaledb}"

# --- 通过 PID 文件停止 ---
stop_by_pidfile() {
    local name="$1"
    local pid_file="$PID_DIR/${name}.pid"

    if [ ! -f "$pid_file" ]; then
        warn "$name 没有 PID 文件，尝试按端口清理"
        return 1
    fi
    local pid
    pid=$(cat "$pid_file")
    if ! kill -0 "$pid" 2>/dev/null; then
        warn "$name 进程 (PID $pid) 已不存在，清理 PID 文件"
        rm -f "$pid_file"
        return 0
    fi

    info "停止 $name (PID $pid)..."
    # 杀掉进程组,确保 mvn 派生的 java 也被终止
    local pgid
    pgid=$(ps -o pgid= -p "$pid" 2>/dev/null | tr -d ' ' || true)
    if [ -n "$pgid" ]; then
        kill -TERM -"$pgid" 2>/dev/null || true
    else
        kill -TERM "$pid" 2>/dev/null || true
    fi

    for i in {1..15}; do
        if ! kill -0 "$pid" 2>/dev/null; then
            ok "$name 已停止"
            rm -f "$pid_file"
            return 0
        fi
        sleep 1
    done

    warn "$name 未在 15s 内退出，发送 SIGKILL"
    if [ -n "$pgid" ]; then
        kill -KILL -"$pgid" 2>/dev/null || true
    else
        kill -KILL "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
    ok "$name 已强制停止"
}

# --- 按端口兜底清理 ---
stop_by_port() {
    local name="$1"
    local port="$2"
    local pids
    pids=$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
    if [ -z "$pids" ]; then
        return
    fi
    info "端口 $port 仍被占用 ($name)，PIDs: $pids"
    for pid in $pids; do
        kill -TERM "$pid" 2>/dev/null || true
    done
    sleep 2
    pids=$(lsof -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
    if [ -n "$pids" ]; then
        for pid in $pids; do
            kill -KILL "$pid" 2>/dev/null || true
        done
    fi
    ok "端口 $port 已释放"
}

echo ""
echo "========================================="
echo "  LdfTrader  关闭"
echo "========================================="

AKSHARE_PORT="${AKSHARE_BRIDGE_PORT:-8186}"

stop_by_pidfile "web-app"             || stop_by_port "web-app"             3000
stop_by_pidfile "web-service"         || stop_by_port "web-service"         8181
stop_by_pidfile "backtest-service"    || stop_by_port "backtest-service"    8185
stop_by_pidfile "indicator-service"   || stop_by_port "indicator-service"   8183
stop_by_pidfile "market-data-service" || stop_by_port "market-data-service" 8182
stop_by_pidfile "akshare-bridge"      || stop_by_port "akshare-bridge"      "$AKSHARE_PORT"

# 端口兜底
stop_by_port "web-app"              3000
stop_by_port "web-service"          8181
stop_by_port "backtest-service"     8185
stop_by_port "indicator-service"    8183
stop_by_port "market-data-service"  8182
stop_by_port "akshare-bridge"       "$AKSHARE_PORT"

if [ "$STOP_DB" = "true" ]; then
    if docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER_NAME}$"; then
        info "停止 TimescaleDB 容器..."
        docker stop "$DB_CONTAINER_NAME" >/dev/null
        ok "TimescaleDB 已停止"
    fi
    if [ "$PURGE_DB" = "true" ]; then
        if docker ps -a --format '{{.Names}}' | grep -q "^${DB_CONTAINER_NAME}$"; then
            info "删除 TimescaleDB 容器 (数据卷 trading-pgdata 保留)..."
            docker rm "$DB_CONTAINER_NAME" >/dev/null
            ok "容器已删除"
        fi
    fi
else
    if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER_NAME}$"; then
        info "TimescaleDB 容器保持运行 (使用 --all 一并停止)"
    fi
fi

echo "========================================="
ok "全部服务已关闭"
echo "========================================="
