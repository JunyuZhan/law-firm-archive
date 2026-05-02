#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
VERSION_FILE="$ROOT_DIR/VERSION"
COMPOSE_FILE="$ROOT_DIR/docker/docker-compose.yml"

if [[ -f "$VERSION_FILE" ]]; then
  APP_VERSION=$(tr -d ' \r\n' < "$VERSION_FILE")
else
  APP_VERSION="latest"
fi

echo "==> 构建档案管理系统镜像 (版本: $APP_VERSION)"
echo "==> 使用 compose 文件: $COMPOSE_FILE"

docker compose -f "$COMPOSE_FILE" build \
  --build-arg "APP_VERSION=$APP_VERSION" \
  elasticsearch backend frontend

echo ""
echo "构建完成。镜像已标记版本: $APP_VERSION"
echo ""
echo "启动方式："
echo "  docker compose -f $COMPOSE_FILE up -d"
