#!/bin/bash
# =====================================================
# Docker 完全清理脚本 - 删除所有镜像和持久化数据
# =====================================================
# ⚠️ 警告：此脚本会删除 Docker 中的所有资源！
#   • 所有容器（运行中和已停止）
#   • 所有镜像
#   • 所有数据卷
#   • 所有网络（除默认网络外）
#   • 所有构建缓存
# =====================================================
# 用法: ./clean-docker-all.sh [--force]
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

# 解析参数
FORCE_FLAG=""
for arg in "$@"; do
    case $arg in
        --force) FORCE_FLAG="true" ;;
    esac
done

# 显示当前资源统计
show_stats() {
    echo -e "${CYAN}当前 Docker 资源统计:${NC}"
    echo "  镜像: $(docker images -q 2>/dev/null | wc -l | tr -d ' ') 个"
    echo "  容器: $(docker ps -a -q 2>/dev/null | wc -l | tr -d ' ') 个"
    echo "  数据卷: $(docker volume ls -q 2>/dev/null | wc -l | tr -d ' ') 个"
    echo "  网络: $(docker network ls -q 2>/dev/null | wc -l | tr -d ' ') 个"
    echo "  构建缓存: $(docker builder prune --dry-run --all -f 2>/dev/null | grep -E 'Total|总计' | grep -oE '[0-9.]+ [A-Z]+' || echo '未知')"
    echo ""
}

echo -e "${RED}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║                    ⚠️  DANGER ZONE  ⚠️                       ║${NC}"
echo -e "${RED}╠══════════════════════════════════════════════════════════════╣${NC}"
echo -e "${RED}║  此操作将删除 Docker 中的 ${CYAN}所有${RED} 资源：                      ║${NC}"
echo -e "${RED}║  • 所有容器（包括运行中的）                                 ║${NC}"
echo -e "${RED}║  • 所有镜像                                                ║${NC}"
echo -e "${RED}║  • 所有数据卷（数据库、文件等持久化数据）                   ║${NC}"
echo -e "${RED}║  • 所有网络（除默认网络外）                                 ║${NC}"
echo -e "${RED}║  • 所有构建缓存                                            ║${NC}"
echo -e "${RED}║                                                              ║${NC}"
echo -e "${RED}║  ⚠️  此操作不可逆！所有数据将永久丢失！                     ║${NC}"
echo -e "${RED}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

show_stats

# 确认操作（除非使用 --force）
if [ "$FORCE_FLAG" != "true" ]; then
    echo -e "${YELLOW}请仔细确认您要删除所有 Docker 资源！${NC}"
    read -p "输入 'DELETE ALL' 确认清理: " confirm
    if [ "$confirm" != "DELETE ALL" ]; then
        echo "操作已取消。"
        exit 0
    fi
fi

echo ""
echo -e "${YELLOW}[1/6] 停止所有运行中的容器...${NC}"
docker stop $(docker ps -q) 2>/dev/null || echo "  没有运行中的容器"
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${YELLOW}[2/6] 删除所有容器...${NC}"
docker rm -f $(docker ps -a -q) 2>/dev/null || echo "  没有容器需要删除"
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${YELLOW}[3/6] 删除所有镜像...${NC}"
docker rmi -f $(docker images -q) 2>/dev/null || echo "  没有镜像需要删除"
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${YELLOW}[4/6] 删除所有数据卷...${NC}"
docker volume rm $(docker volume ls -q) 2>/dev/null || echo "  没有数据卷需要删除"
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${YELLOW}[5/6] 删除所有自定义网络...${NC}"
# 获取所有非默认网络
NETWORKS=$(docker network ls -q --filter type=custom 2>/dev/null || docker network ls --format "{{.ID}}" | tail -n +2)
if [ -n "$NETWORKS" ]; then
    echo "$NETWORKS" | xargs docker network rm 2>/dev/null || true
fi
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${YELLOW}[6/6] 清理构建缓存和未使用的资源...${NC}"
docker builder prune -af --filter "until=0s" 2>/dev/null || true
docker system prune -af --volumes 2>/dev/null || true
echo -e "${GREEN}✓ 完成${NC}"

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}Docker 完全清理完成!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""

# 显示清理后的统计
echo -e "${CYAN}清理后 Docker 资源统计:${NC}"
show_stats

echo -e "${GREEN}✓ 所有 Docker 资源已清除${NC}"
echo ""
echo "提示: 如需重新使用，请运行:"
echo "  ./scripts/dev-start.sh --full"
echo ""
