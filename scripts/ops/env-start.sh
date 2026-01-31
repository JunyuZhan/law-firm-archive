#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 统一环境启动脚本
# =====================================================
# 用法: ./env-start.sh [dev|test|prod] [--full] [--services=SERVICE1,SERVICE2]
#   dev:  开发环境
#   test: 测试环境
#   prod: 生产环境
#   --full:     启动全量服务（仅 dev/test）
#   --services: 指定要启动的服务（逗号分隔）
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
FULL_MODE=false
SERVICES=""

# 显示使用说明
show_usage() {
    echo -e "${CYAN}用法:${NC}"
    echo "  ./env-start.sh [dev|test|prod] [选项]"
    echo ""
    echo -e "${CYAN}环境类型:${NC}"
    echo "  dev   - 开发环境"
    echo "  test  - 测试环境"
    echo "  prod  - 生产环境"
    echo ""
    echo -e "${CYAN}选项:${NC}"
    echo "  --full           启动全量服务（仅 dev/test）"
    echo "  --services=LIST  指定要启动的服务（逗号分隔）"
    echo ""
    echo -e "${CYAN}示例:${NC}"
    echo "  ./env-start.sh dev"
    echo "  ./env-start.sh dev --full"
    echo "  ./env-start.sh test --services=postgres,redis"
    echo "  ./env-start.sh prod"
}

# 解析参数
for arg in "$@"; do
    case $arg in
        dev|test|prod)
            ENV_TYPE="$arg"
            ;;
        --full)
            FULL_MODE=true
            ;;
        --services=*)
            SERVICES="${arg#*=}"
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
    echo -e "${RED}错误: 必须指定环境类型 (dev|test|prod)${NC}"
    show_usage
    exit 1
fi

# 验证环境类型
if [[ ! "$ENV_TYPE" =~ ^(dev|test|prod)$ ]]; then
    echo -e "${RED}错误: 无效的环境类型 '$ENV_TYPE'，必须是 dev、test 或 prod${NC}"
    exit 1
fi

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}律师事务所管理系统 - 环境启动${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

cd "$DOCKER_DIR"

# 确定使用的 docker-compose 文件
case "$ENV_TYPE" in
    dev)
        if [ "$FULL_MODE" = true ]; then
            COMPOSE_FILE="docker-compose.dev-full.yml"
            ENV_NAME="开发环境（全量）"
        else
            COMPOSE_FILE="docker-compose.dev.yml"
            ENV_NAME="开发环境"
        fi
        ;;
    test)
        COMPOSE_FILE="docker-compose.test.yml"
        ENV_NAME="测试环境"
        if [ "$FULL_MODE" = true ]; then
            echo -e "${YELLOW}提示: 测试环境不支持 --full 选项${NC}"
        fi
        ;;
    prod)
        COMPOSE_FILE="docker-compose.prod.yml"
        ENV_NAME="生产环境"
        if [ "$FULL_MODE" = true ]; then
            echo -e "${YELLOW}提示: 生产环境不支持 --full 选项${NC}"
        fi
        # 检查 .env 文件
        if [ ! -f "$PROJECT_ROOT/.env" ]; then
            echo -e "${RED}错误: 生产环境需要 .env 配置文件${NC}"
            echo "请先创建 $PROJECT_ROOT/.env 文件"
            exit 1
        fi
        ;;
esac

# 检查 docker-compose 文件是否存在
if [ ! -f "$COMPOSE_FILE" ]; then
    echo -e "${RED}错误: 找不到 $COMPOSE_FILE${NC}"
    exit 1
fi

echo -e "${CYAN}环境: $ENV_NAME${NC}"
echo -e "${CYAN}配置文件: $COMPOSE_FILE${NC}"

# 确定要启动的服务
if [ -n "$SERVICES" ]; then
    # 用户指定了服务列表
    SERVICE_LIST=$(echo "$SERVICES" | tr ',' ' ')
    echo -e "${YELLOW}启动服务: $SERVICE_LIST${NC}"
else
    # 默认启动所有服务
    SERVICE_LIST=""
    echo -e "${YELLOW}启动所有服务${NC}"
fi

echo ""
echo -e "${BLUE}[1/2] 启动 Docker 服务...${NC}"

# 生产环境使用 --env-file
if [ "$ENV_TYPE" = "prod" ]; then
    docker compose --env-file "$PROJECT_ROOT/.env" -f "$COMPOSE_FILE" up -d $SERVICE_LIST
else
    docker compose -f "$COMPOSE_FILE" up -d $SERVICE_LIST
fi

echo ""
echo -e "${BLUE}[2/2] 等待服务就绪...${NC}"
sleep 3

# 检查 PostgreSQL 是否就绪
POSTGRES_CONTAINER=""
case "$ENV_TYPE" in
    dev)
        POSTGRES_CONTAINER="law-firm-postgres"
        DB_NAME="law_firm_dev"
        ;;
    test)
        POSTGRES_CONTAINER="law-firm-test-postgres"
        DB_NAME="law_firm_test"
        ;;
    prod)
        POSTGRES_CONTAINER="law-firm-postgres"
        DB_NAME="law_firm"
        ;;
esac

if docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}$"; then
    echo -e "${YELLOW}等待 PostgreSQL 就绪...${NC}"
    timeout=30
    while [ $timeout -gt 0 ]; do
        if docker exec "$POSTGRES_CONTAINER" pg_isready -U law_admin -d "$DB_NAME" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ PostgreSQL 已就绪${NC}"
            break
        fi
        sleep 1
        timeout=$((timeout - 1))
    done
    
    if [ $timeout -eq 0 ]; then
        echo -e "${YELLOW}警告: PostgreSQL 可能尚未完全就绪，请稍候${NC}"
    fi
fi

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}环境启动完成!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
echo "服务状态:"
if [ "$ENV_TYPE" = "prod" ]; then
    docker compose --env-file "$PROJECT_ROOT/.env" -f "$COMPOSE_FILE" ps
else
    docker compose -f "$COMPOSE_FILE" ps
fi
echo ""
echo "下一步操作:"
case "$ENV_TYPE" in
    dev)
        echo "  初始化数据库: ./scripts/ops/reset-db.sh --dev"
        echo "  查看日志:     cd docker && docker compose -f $COMPOSE_FILE logs -f"
        echo "  停止服务:     ./scripts/ops/env-stop.sh dev"
        ;;
    test)
        echo "  初始化数据库: ./scripts/ops/reset-db.sh --test"
        echo "  查看日志:     cd docker && docker compose -f $COMPOSE_FILE logs -f"
        echo "  停止服务:     ./scripts/ops/env-stop.sh test"
        ;;
    prod)
        echo "  查看日志:     cd docker && docker compose --env-file ../.env -f $COMPOSE_FILE logs -f"
        echo "  停止服务:     ./scripts/ops/env-stop.sh prod"
        ;;
esac
echo ""
