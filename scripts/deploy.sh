#!/bin/bash
# 部署脚本入口（重定向到 deploy/ 目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/deploy/deploy.sh" "$@"
