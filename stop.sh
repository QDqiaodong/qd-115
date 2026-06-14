#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

cd "$SCRIPT_DIR"

echo "停止并移除 av-ledger 项目容器..."
docker compose --env-file "$ENV_FILE" down

echo ""
echo "服务已停止。"
echo "如需清除数据卷（包括数据库数据），执行："
echo "  docker compose --env-file $ENV_FILE down -v"
