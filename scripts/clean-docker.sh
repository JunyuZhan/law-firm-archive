#!/bin/bash
# =====================================================
# 律师事务所管理系统 - Docker 持久化数据清理脚本
# =====================================================
# ⚠️ 警告：此脚本会删除所有 Docker 数据卷、容器和网络！
# 用法: ./clean-docker.sh [--force]
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_DIR="$PROJECT_ROOT/docker"

# 解析参数
FORCE_FLAG=""
for arg in "$@"; do
    case $arg in
        --force) FORCE_FLAG="true" ;;
    esac
done

echo -e "${RED}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║                    DANGER ZONE                               ║${NC}"
echo -e "${RED}╠══════════════════════════════════════════════════════════════╣${NC}"
echo -e "${RED}║  此操作将删除以下 Docker 资源：                              ║${NC}"
echo -e "${RED}║  • 所有相关容器 (law-firm-*, dev-*)                         ║${NC}"
echo -e "${RED}║  • 所有相关数据卷 (数据库、Redis、MinIO、OnlyOffice 等)    ║${NC}"
echo -e "${RED}║  • 所有相关网络                                             ║${NC}"
echo -e "${RED}║                                                              ║${NC}"
echo -e "${RED}║  ⚠️  数据库数据将永久丢失！                                 ║${NC}"
echo -e "${RED}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 确认操作（除非使用 --force）
if [ "$FORCE_FLAG" != "true" ]; then
    read -p "输入 'delete' 确认清理: " confirm
    if [ "$confirm" != "delete" ]; then
        echo "操作已取消。"
        exit 0
    fi
fi

echo ""
echo -e "${YELLOW}[1/4] 停止并删除所有相关容器...${NC}"

# 停止并删除所有相关容器
cd "$DOCKER_DIR"
docker compose -f docker-compose.yml down -v --remove-orphans 2>/dev/null || true
docker compose -f docker-compose.dev-full.yml down -v --remove-orphans 2>/dev/null || true
docker compose -f docker-compose.prod.yml down -v --remove-orphans 2>/dev/null || true

# 强制删除可能残留的容器
CONTAINERS=$(docker ps -a --format "{{.Names}}" | grep -E "(law-firm|dev-)" || true)
if [ -n "$CONTAINERS" ]; then
    echo "$CONTAINERS" | xargs docker rm -f 2>/dev/null || true
fi

echo -e "${GREEN}✓ 容器清理完成${NC}"

echo ""
echo -e "${YELLOW}[2/4] 删除所有相关数据卷...${NC}"

# 删除所有相关数据卷（只匹配项目相关的前缀，避免误删其他项目）
VOLUMES=$(docker volume ls -q | grep -E "^(law-firm|dev-|test-)" || true)
if [ -n "$VOLUMES" ]; then
    echo "$VOLUMES" | xargs docker volume rm 2>/dev/null || true
fi

# 也清理docker-compose创建的数据卷（通过compose文件中的volume名称）
# 这些数据卷通常以项目目录名或服务名命名，更安全
cd "$DOCKER_DIR"
for compose_file in docker-compose*.yml; do
    if [ -f "$compose_file" ]; then
        docker compose -f "$compose_file" down -v --remove-orphans 2>/dev/null || true
    fi
done

echo -e "${GREEN}✓ 数据卷清理完成${NC}"

echo ""
echo -e "${YELLOW}[3/4] 删除所有相关网络...${NC}"

# 删除所有相关网络
NETWORKS=$(docker network ls -q --filter "name=law-firm" --filter "name=dev-" || true)
if [ -n "$NETWORKS" ]; then
    echo "$NETWORKS" | xargs docker network rm 2>/dev/null || true
fi

# 也尝试通过名称匹配删除
NETWORKS_BY_NAME=$(docker network ls --format "{{.Name}}" | grep -E "(law-firm|dev-)" || true)
if [ -n "$NETWORKS_BY_NAME" ]; then
    echo "$NETWORKS_BY_NAME" | xargs docker network rm 2>/dev/null || true
fi

echo -e "${GREEN}✓ 网络清理完成${NC}"

echo ""
echo -e "${YELLOW}[4/4] 验证清理结果...${NC}"

# 验证清理结果
REMAINING_CONTAINERS=$(docker ps -a --format "{{.Names}}" | grep -E "(law-firm|dev-)" | wc -l | tr -d ' ')
REMAINING_VOLUMES=$(docker volume ls -q | grep -E "(law-firm|dev-|postgres|redis|minio|onlyoffice|ocr|elasticsearch)" | wc -l | tr -d ' ')
REMAINING_NETWORKS=$(docker network ls --format "{{.Name}}" | grep -E "(law-firm|dev-)" | wc -l | tr -d ' ')

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}Docker 持久化数据清理完成!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
echo "清理结果:"
echo "  容器: $REMAINING_CONTAINERS 个剩余"
echo "  数据卷: $REMAINING_VOLUMES 个剩余"
echo "  网络: $REMAINING_NETWORKS 个剩余"
echo ""

if [ "$REMAINING_CONTAINERS" -eq 0 ] && [ "$REMAINING_VOLUMES" -eq 0 ] && [ "$REMAINING_NETWORKS" -eq 0 ]; then
    echo -e "${GREEN}✓ 所有资源已完全清除${NC}"
else
    echo -e "${YELLOW}⚠ 仍有部分资源残留，可能需要手动清理${NC}"
fi

echo ""
echo "下一步操作:"
echo "  启动开发环境: ./scripts/env-start.sh"
echo "  初始化数据库: ./scripts/reset-db.sh --dev"
echo ""
