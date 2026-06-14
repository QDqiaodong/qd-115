#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env file not found at $ENV_FILE"
    exit 1
fi

set -a
source "$ENV_FILE"
set +a

check_port_occupied() {
    local port=$1
    if command -v lsof > /dev/null 2>&1; then
        local result
        result=$(lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | grep -v "^COMMAND")
        if [ -n "$result" ]; then
            echo "$result"
            return 0
        fi
    fi
    if command -v ss > /dev/null 2>&1; then
        local result
        result=$(ss -nlt "sport = :$port" 2>/dev/null | grep ":$port")
        if [ -n "$result" ]; then
            echo "$result"
            return 0
        fi
    fi
    if command -v netstat > /dev/null 2>&1; then
        local result
        result=$(netstat -an 2>/dev/null | grep "[.:]$port " | grep LISTEN)
        if [ -n "$result" ]; then
            echo "$result"
            return 0
        fi
    fi
    return 1
}

echo "========================================"
echo "  家用影音设备运维台账 - 启动脚本"
echo "========================================"
echo ""

resolve_port() {
    local var_name=$1
    local label=$2
    local start_port=${!var_name}
    local port=$start_port
    local max_port=$((start_port + 200))

    while check_port_occupied "$port" >/dev/null; do
        echo "⚠️  $label 端口 $port 已占用，尝试 $((port + 1))"
        port=$((port + 1))
        if [ "$port" -gt "$max_port" ]; then
            echo "✗ 错误：$label 从 $start_port 起连续 200 个端口均不可用。"
            exit 1
        fi
    done

    export "$var_name=$port"
    if [ "$port" != "$start_port" ]; then
        echo "  ✓ $label 已自动顺延为 $port"
    else
        echo "  ✓ $label 端口 $port 可用"
    fi
}

resolve_port FRONTEND_PORT "前端"
resolve_port BACKEND_PORT "后端"
resolve_port MYSQL_PORT "MySQL"
resolve_port REDIS_PORT "Redis"

echo ""
echo "端口检查完成，开始构建并启动容器..."
echo ""

cd "$SCRIPT_DIR"
docker compose --env-file "$ENV_FILE" up -d --build

BUILD_EXIT=$?
if [ $BUILD_EXIT -ne 0 ]; then
    echo ""
    echo "✗ Docker 构建启动失败，退出码: $BUILD_EXIT"
    exit $BUILD_EXIT
fi

echo ""
echo "等待服务启动完成 (15秒) ..."
sleep 15

echo ""
echo "========================================"
echo "  服务启动完成！"
echo "========================================"
echo ""
echo "前端访问地址："
echo "  http://localhost:$FRONTEND_PORT"
echo "  http://127.0.0.1:$FRONTEND_PORT"
echo ""
echo "后端 API 地址："
echo "  http://127.0.0.1:$BACKEND_PORT/api"
echo ""
echo "MySQL：127.0.0.1:$MYSQL_PORT"
echo "Redis：127.0.0.1:$REDIS_PORT"
echo ""
echo "停止服务：./stop.sh"
echo "========================================"
