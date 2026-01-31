#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 统一环境停止脚本
# =====================================================
# 用法: ./env-stop.sh [dev|prod] [--remove-volumes]
#   dev:  开发环境
#   prod: 生产环境
#   --remove-volumes: 同时删除数据卷（危险操作）
# =====================================================

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_DIR="$PROJECT_ROOT/docker"

# 解析参数
ENV_TYPE=""
REMOVE_VOLUMES=false

# 显示使用说明
show_usage() {
    echo -e "${CYAN}用法:${NC}"
    echo "  ./env-stop.sh [dev|prod] [选项]"
    echo ""
    echo -e "${CYAN}环境类型:${NC}"
    echo "  dev   - 开发环境"
    echo "  prod  - 生产环境"
    echo ""
    echo -e "${CYAN}选项:${NC}"
    echo "  --remove-volumes  同时删除数据卷（危险操作）"
    echo ""
    echo -e "${CYAN}示例:${NC}"
    echo "  ./env-stop.sh dev"
    echo "  ./env-stop.sh dev --remove-volumes"
    echo "  ./env-stop.sh prod"
}

# 解析参数
for arg in "$@"; do
    case $arg in
        dev|prod)
            ENV_TYPE="$arg"
            ;;
        --remove-volumes)
            REMOVE_VOLUMES=true
            ;;
        --help|-h)
            show_usage
            exit 0
            ;;
        *)
            echo -e "${RED}错误: 未知参数 $arg${NC}"
            show_usage
            exit 1
            ;;
    esac
done

# 检查环境类型
if [ -z "$ENV_TYPE" ]; then
    echo -e "${RED}错误: 必须指定环境类型 (dev|prod)${NC}"
    show_usage
    exit 1
fi

# 验证环境类型
if [[ ! "$ENV_TYPE" =~ ^(dev|prod)$ ]]; then
    echo -e "${RED}错误: 无效的环境类型 '$ENV_TYPE'，必须是 dev 或 prod${NC}"
    exit 1
fi

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}律师事务所管理系统 - 环境停止${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

cd "$DOCKER_DIR"

# 确定使用的 docker-compose 文件
case "$ENV_TYPE" in
    dev)
        COMPOSE_FILE="docker-compose.dev.yml"
        ENV_NAME="开发环境"
        ;;
    prod)
        COMPOSE_FILE="docker-compose.prod.yml"
        ENV_NAME="生产环境"
        ;;
esac

echo -e "${CYAN}环境: $ENV_NAME${NC}"

# 警告删除数据卷
if [ "$REMOVE_VOLUMES" = true ]; then
    echo -e "${RED}⚠️  警告: 将删除所有数据卷，数据将永久丢失！${NC}"
    read -p "确认继续? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        echo "操作已取消"
        exit 0
    fi
fi

echo ""
echo -e "${YELLOW}[1/2] 停止服务...${NC}"

# 停止服务
if [ "$ENV_TYPE" = "prod" ]; then
    docker compose --env-file "$PROJECT_ROOT/.env" -f "$COMPOSE_FILE" down ${REMOVE_VOLUMES:+-v} --remove-orphans
else
    docker compose -f "$COMPOSE_FILE" down ${REMOVE_VOLUMES:+-v} --remove-orphans
fi

echo -e "${GREEN}✓ 服务已停止${NC}"

if [ "$REMOVE_VOLUMES" = true ]; then
    echo ""
    echo -e "${YELLOW}[2/2] 清理数据卷...${NC}"
    echo -e "${GREEN}✓ 数据卷已删除${NC}"
fi

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}环境停止完成!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
