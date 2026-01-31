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
echo -e "${RED}║  ✅ 其他应用的容器和数据卷不会被删除                        ║${NC}"
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
ENV_FILE="$PROJECT_ROOT/.env"

# 如果存在.env文件，使用它；否则不使用
if [ -f "$ENV_FILE" ]; then
    docker compose --env-file "$ENV_FILE" -f docker-compose.yml down -v --remove-orphans 2>/dev/null || true
    docker compose --env-file "$ENV_FILE" -f docker-compose.dev.yml down -v --remove-orphans 2>/dev/null || true
    docker compose --env-file "$ENV_FILE" -f docker-compose.prod.yml down -v --remove-orphans 2>/dev/null || true
    docker compose --env-file "$ENV_FILE" -f docker-compose.swarm.yml down -v --remove-orphans 2>/dev/null || true
else
    docker compose -f docker-compose.yml down -v --remove-orphans 2>/dev/null || true
    docker compose -f docker-compose.dev.yml down -v --remove-orphans 2>/dev/null || true
    docker compose -f docker-compose.prod.yml down -v --remove-orphans 2>/dev/null || true
    docker compose -f docker-compose.swarm.yml down -v --remove-orphans 2>/dev/null || true
fi

# 强制删除可能残留的容器（只清理 law-firm-* 前缀的容器）
# 注意：onlyoffice、redis、prometheus、grafana 容器（无前缀）不会被删除，因为它们可以共享
CONTAINERS=$(docker ps -a --format "{{.Names}}" | grep -E "^(law-firm|dev-)" | grep -vE "^(onlyoffice|redis|prometheus|grafana)$" || true)
if [ -n "$CONTAINERS" ]; then
    echo "$CONTAINERS" | xargs docker rm -f 2>/dev/null || true
fi

echo -e "${GREEN}✓ 容器清理完成${NC}"

echo ""
echo -e "${YELLOW}[2/4] 删除所有相关数据卷...${NC}"

# 优先通过docker-compose清理数据卷（最安全，只清理本项目定义的资源）
# 这些数据卷由docker-compose管理，名称通常为：项目名_服务名_data
# 例如：law-firm-prod_minio_data、law-firm-dev_postgres_data
cd "$DOCKER_DIR"
ENV_FILE="$PROJECT_ROOT/.env"
for compose_file in docker-compose*.yml; do
    if [ -f "$compose_file" ]; then
        if [ -f "$ENV_FILE" ]; then
            docker compose --env-file "$ENV_FILE" -f "$compose_file" down -v --remove-orphans 2>/dev/null || true
        else
            docker compose -f "$compose_file" down -v --remove-orphans 2>/dev/null || true
        fi
    fi
done

# 清理可能残留的数据卷（只匹配项目相关的前缀，避免误删其他项目）
# Docker Compose会自动给数据卷加上项目名称前缀，如：
# - law-firm-prod_minio_data（生产环境）
# - law-firm-dev_postgres_data（开发环境）
# - law-firm-test_redis_data（测试环境）
# - law-firm_*（下划线格式，如 law-firm_grafana_data）
# 其他应用的数据卷（如pis_minio_data）不会被删除
VOLUMES=$(docker volume ls -q | grep -E "^(law-firm|dev-|test-)" || true)
if [ -n "$VOLUMES" ]; then
    echo "$VOLUMES" | xargs docker volume rm 2>/dev/null || true
fi

# 清理下划线格式的数据卷（law-firm_*）
VOLUMES_UNDERSCORE=$(docker volume ls -q | grep -E "^law-firm_" || true)
if [ -n "$VOLUMES_UNDERSCORE" ]; then
    echo "$VOLUMES_UNDERSCORE" | xargs docker volume rm 2>/dev/null || true
fi

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

# 验证清理结果（只统计 law-firm-* 前缀的容器）
REMAINING_CONTAINERS=$(docker ps -a --format "{{.Names}}" | grep -E "^(law-firm|dev-)" | wc -l | tr -d ' ')
REMAINING_VOLUMES=$(docker volume ls -q | grep -E "^(law-firm|dev-|test-)" | wc -l | tr -d ' ')
REMAINING_NETWORKS=$(docker network ls --format "{{.Name}}" | grep -E "^(law-firm|dev-)" | wc -l | tr -d ' ')

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
echo "  启动开发环境: ./scripts/ops/env-start.sh"
echo "  初始化数据库: ./scripts/ops/reset-db.sh --dev"
echo ""
