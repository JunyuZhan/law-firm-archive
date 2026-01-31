#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 统一环境重置脚本
# =====================================================
# ⚠️ 警告：此脚本会删除所有数据并重新初始化！
# 用法: ./env-reset.sh [dev|test|prod] [--force]
#   dev:  开发环境
#   test: 测试环境
#   prod: 生产环境（不推荐在生产环境使用）
#   --force: 跳过确认提示
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

# 解析参数
ENV_TYPE=""
FORCE_FLAG=""

# 显示使用说明
show_usage() {
    echo -e "${CYAN}用法:${NC}"
    echo "  ./env-reset.sh [dev|test|prod] [选项]"
    echo ""
    echo -e "${CYAN}环境类型:${NC}"
    echo "  dev   - 开发环境"
    echo "  test  - 测试环境"
    echo "  prod  - 生产环境（不推荐）"
    echo ""
    echo -e "${CYAN}选项:${NC}"
    echo "  --force  跳过确认提示"
    echo ""
    echo -e "${CYAN}示例:${NC}"
    echo "  ./env-reset.sh dev"
    echo "  ./env-reset.sh test --force"
}

# 解析参数
for arg in "$@"; do
    case $arg in
        dev|test|prod)
            ENV_TYPE="$arg"
            ;;
        --force)
            FORCE_FLAG="true"
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

# 生产环境警告
if [ "$ENV_TYPE" = "prod" ]; then
    echo -e "${RED}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║                    ⚠️  严重警告  ⚠️                         ║${NC}"
    echo -e "${RED}╠══════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${RED}║  您正在重置生产环境！                                        ║${NC}"
    echo -e "${RED}║  此操作将删除所有生产数据！                                  ║${NC}"
    echo -e "${RED}║  请确认您完全理解此操作的后果！                              ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    read -p "输入 'RESET PRODUCTION' 确认: " confirm
    if [ "$confirm" != "RESET PRODUCTION" ]; then
        echo "操作已取消"
        exit 0
    fi
fi

echo -e "${RED}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║                    DANGER ZONE                               ║${NC}"
echo -e "${RED}╠══════════════════════════════════════════════════════════════╣${NC}"
echo -e "${RED}║  此操作将执行以下操作：                                        ║${NC}"
echo -e "${RED}║  1. 停止并删除所有相关容器                                    ║${NC}"
echo -e "${RED}║  2. 删除所有数据卷（数据库数据将永久丢失！）                  ║${NC}"
echo -e "${RED}║  3. 重新启动环境                                              ║${NC}"
echo -e "${RED}║  4. 重新初始化数据库                                          ║${NC}"
echo -e "${RED}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 确认操作（除非使用 --force）
if [ "$FORCE_FLAG" != "true" ]; then
    read -p "输入 'reset' 确认重置: " confirm
    if [ "$confirm" != "reset" ]; then
        echo "操作已取消"
        exit 0
    fi
fi

echo ""
echo -e "${YELLOW}[1/4] 停止并删除环境...${NC}"
./scripts/ops/env-stop.sh "$ENV_TYPE" --remove-volumes

echo ""
echo -e "${YELLOW}[2/4] 重新启动环境...${NC}"
case "$ENV_TYPE" in
    dev)
        ./scripts/ops/env-start.sh dev
        ;;
    test)
        ./scripts/ops/env-start.sh test
        ;;
    prod)
        ./scripts/ops/env-start.sh prod
        ;;
esac

echo ""
echo -e "${YELLOW}[3/4] 等待服务就绪...${NC}"
sleep 5

echo ""
echo -e "${YELLOW}[4/4] 初始化数据库...${NC}"
case "$ENV_TYPE" in
    dev)
        ./scripts/reset-db.sh --dev --force
        ;;
    test)
        ./scripts/reset-db.sh --test --force
        ;;
    prod)
        echo -e "${RED}警告: 生产环境数据库初始化需要手动执行${NC}"
        echo "请运行: ./scripts/ops/reset-db.sh --prod --force"
        ;;
esac

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}环境重置完成!${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""
