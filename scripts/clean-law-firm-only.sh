#!/bin/bash
# =====================================================
# 仅清理律所系统相关资源（保留其他容器如 frpc）
# =====================================================
# 用法: ./clean-law-firm-only.sh [--force]
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 解析参数
FORCE_FLAG=""
for arg in "$@"; do
    case $arg in
        --force) FORCE_FLAG="true" ;;
    esac
done

# 项目相关容器名称模式
CONTAINER_PATTERNS=(
    "law-firm-*"
    "dev-*"
    "test-*"
)

# 项目相关数据卷模式
VOLUME_PATTERNS=(
    "law-firm*"
    "postgres_data"
    "redis_data"
    "minio_data"
    "onlyoffice*"
    "ocr*"
    "prometheus_data"
    "grafana_data"
    "elasticsearch*"
)

# 项目相关网络模式
NETWORK_PATTERNS=(
    "law-firm*"
    "dev-*"
    "test-*"
)

echo -e "${YELLOW}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${YELLOW}║              清理律所系统相关资源                           ║${NC}"
echo -e "${YELLOW}╠══════════════════════════════════════════════════════════════╣${NC}"
echo -e "${YELLOW}║  此操作将删除以下资源：                                      ║${NC}"
echo -e "${YELLOW}║  • 律所系统相关容器（law-firm-*）                           ║${NC}"
echo -e "${YELLOW}║  • 律所系统相关镜像                                          ║${NC}"
echo -e "${YELLOW}║  • 律所系统相关数据卷（数据库、文件等）                      ║${NC}"
echo -e "${YELLOW}║  • 律所系统相关网络                                          ║${NC}"
echo -e "${YELLOW}║                                                              ║${NC}"
echo -e "${YELLOW}║  ✅ 其他容器（如 frpc）将被保留                             ║${NC}"
echo -e "${YELLOW}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 显示当前资源统计
show_stats() {
    echo -e "${CYAN}当前律所系统相关资源统计:${NC}"
    
    # 统计容器
    CONTAINER_COUNT=0
    for pattern in "${CONTAINER_PATTERNS[@]}"; do
        COUNT=$(docker ps -a --format "{{.Names}}" | grep -E "^${pattern//\*/.*}$" 2>/dev/null | wc -l | tr -d ' ')
        CONTAINER_COUNT=$((CONTAINER_COUNT + COUNT))
    done
    echo "  容器: $CONTAINER_COUNT 个"
    
    # 统计镜像
    IMAGE_COUNT=$(docker images --format "{{.Repository}}" | grep -E "(law-firm|dev-|test-)" 2>/dev/null | wc -l | tr -d ' ')
    echo "  镜像: $IMAGE_COUNT 个"
    
    # 统计数据卷
    VOLUME_COUNT=0
    for pattern in "${VOLUME_PATTERNS[@]}"; do
        COUNT=$(docker volume ls --format "{{.Name}}" | grep -E "^${pattern//\*/.*}$" 2>/dev/null | wc -l | tr -d ' ')
        VOLUME_COUNT=$((VOLUME_COUNT + COUNT))
    done
    echo "  数据卷: $VOLUME_COUNT 个"
    
    # 统计网络
    NETWORK_COUNT=0
    for pattern in "${NETWORK_PATTERNS[@]}"; do
        COUNT=$(docker network ls --format "{{.Name}}" | grep -E "^${pattern//\*/.*}$" 2>/dev/null | wc -l | tr -d ' ')
        NETWORK_COUNT=$((NETWORK_COUNT + COUNT))
    done
    echo "  网络: $NETWORK_COUNT 个"
    echo ""
}

show_stats

# 确认操作（除非使用 --force）
if [ "$FORCE_FLAG" != "true" ]; then
    echo -e "${YELLOW}请确认您要删除律所系统相关资源！${NC}"
    read -p "输入 'DELETE' 确认清理: " confirm
    if [ "$confirm" != "DELETE" ]; then
        echo "操作已取消。"
        exit 0
    fi
fi

echo ""
echo -e "${YELLOW}[1/5] 停止并删除律所系统相关容器...${NC}"
cd "$PROJECT_ROOT/docker" 2>/dev/null || true

# 停止所有相关的 docker-compose 服务
for compose_file in docker-compose*.yml; do
    if [ -f "$compose_file" ]; then
        echo "  停止 $compose_file..."
        docker compose -f "$compose_file" down -v --remove-orphans 2>/dev/null || true
        if [ -f "../.env" ]; then
            docker compose --env-file ../.env -f "$compose_file" down -v --remove-orphans 2>/dev/null || true
        fi
    fi
done

# 删除匹配模式的容器
for pattern in "${CONTAINER_PATTERNS[@]}"; do
    CONTAINERS=$(docker ps -a --format "{{.Names}}" | grep -E "^${pattern//\*/.*}$" 2>/dev/null || true)
    if [ -n "$CONTAINERS" ]; then
        echo "$CONTAINERS" | while read container; do
            echo "  删除容器: $container"
            docker stop "$container" 2>/dev/null || true
            docker rm -f "$container" 2>/dev/null || true
        done
    fi
done
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${YELLOW}[2/5] 删除律所系统相关镜像...${NC}"
IMAGES=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep -E "(law-firm|dev-|test-)" 2>/dev/null || true)
if [ -n "$IMAGES" ]; then
    echo "$IMAGES" | while read image; do
        echo "  删除镜像: $image"
        docker rmi -f "$image" 2>/dev/null || true
    done
else
    echo "  没有找到相关镜像"
fi
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${YELLOW}[3/5] 删除律所系统相关数据卷...${NC}"
for pattern in "${VOLUME_PATTERNS[@]}"; do
    VOLUMES=$(docker volume ls --format "{{.Name}}" | grep -E "^${pattern//\*/.*}$" 2>/dev/null || true)
    if [ -n "$VOLUMES" ]; then
        echo "$VOLUMES" | while read volume; do
            echo "  删除数据卷: $volume"
            docker volume rm "$volume" 2>/dev/null || true
        done
    fi
done
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${YELLOW}[4/5] 删除律所系统相关网络...${NC}"
for pattern in "${NETWORK_PATTERNS[@]}"; do
    NETWORKS=$(docker network ls --format "{{.Name}}" | grep -E "^${pattern//\*/.*}$" 2>/dev/null || true)
    if [ -n "$NETWORKS" ]; then
        echo "$NETWORKS" | while read network; do
            # 跳过默认网络
            if [ "$network" != "bridge" ] && [ "$network" != "host" ] && [ "$network" != "none" ]; then
                echo "  删除网络: $network"
                docker network rm "$network" 2>/dev/null || true
            fi
        done
    fi
done
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${YELLOW}[5/5] 清理构建缓存...${NC}"
docker builder prune -af --filter "until=0s" 2>/dev/null || true
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}律所系统资源清理完成!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""

# 显示清理后的统计
echo -e "${CYAN}清理后资源统计:${NC}"
show_stats

echo -e "${GREEN}✓ 律所系统相关资源已清除${NC}"
echo -e "${CYAN}✓ 其他容器（如 frpc）已保留${NC}"
echo ""
echo "提示: 如需重新部署，请运行:"
echo "  ./scripts/deploy.sh --quick"
echo ""
