#!/usr/bin/env bash
# 查看 LdfTrader 各服务状态
# 用法: ./scripts/status.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PID_DIR="$ROOT_DIR/.pids"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m'

if [ -f "$ROOT_DIR/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$ROOT_DIR/.env"
    set +a
fi
DB_CONTAINER_NAME="${DB_CONTAINER_NAME:-trading-timescaledb}"

check_service() {
    local name="$1"
    local port="$2"
    local pid_file="$PID_DIR/${name}.pid"
    local pid=""
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
    fi

    local pid_alive="-"
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        pid_alive="$pid"
    fi

    if lsof -iTCP:"$port" -sTCP:LISTEN -P -n >/dev/null 2>&1; then
        printf "  ${GREEN}● %-22s${NC} :%-5s  PID=%s\n" "$name" "$port" "$pid_alive"
    else
        printf "  ${RED}○ %-22s${NC} :%-5s  (未运行)\n" "$name" "$port"
    fi
}

echo ""
echo "========================================="
echo "  LdfTrader  状态"
echo "========================================="

if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER_NAME}$"; then
    printf "  ${GREEN}● %-22s${NC} :%-5s  (docker)\n" "$DB_CONTAINER_NAME" "${DB_PORT:-5432}"
else
    if docker ps -a --format '{{.Names}}' 2>/dev/null | grep -q "^${DB_CONTAINER_NAME}$"; then
        printf "  ${YELLOW}○ %-22s${NC} :%-5s  (容器存在但未运行)\n" "$DB_CONTAINER_NAME" "${DB_PORT:-5432}"
    else
        printf "  ${RED}○ %-22s${NC} :%-5s  (容器不存在)\n" "$DB_CONTAINER_NAME" "${DB_PORT:-5432}"
    fi
fi

check_service "akshare-bridge"      "${AKSHARE_BRIDGE_PORT:-8186}"
check_service "market-data-service" 8182
check_service "indicator-service"   8183
check_service "backtest-service"    8185
check_service "web-service"         8181
check_service "web-app"             3000

echo "========================================="
echo ""
