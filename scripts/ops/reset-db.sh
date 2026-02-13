#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 数据库重置脚本
# =====================================================
# 版本: 1.1.0
# 更新日期: 2026-01-11
# 用法: ./reset-db.sh [--force] [--dev|--test|--prod]
#   --force: 跳过确认提示
#   --dev:   强制使用开发数据库 (law_firm_dev)
#   --test:  强制使用测试数据库 (law_firm_test)
#   --prod:  强制使用生产数据库 (law_firm)
# =====================================================

set -e

DB_USER="law_admin"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
INIT_DB_DIR="$SCRIPTS_DIR/init-db"

# 自动检测 PostgreSQL 容器名（支持多种命名）
detect_postgres_container() {
    local pattern="$1"
    if [ -n "$pattern" ]; then
        # 使用指定的模式查找
        docker ps --format '{{.Names}}' | grep -E "^${pattern}$" | head -1
    else
        # 自动检测：优先检测标准命名
        if docker ps --format '{{.Names}}' | grep -q "^law-firm-postgres$"; then
            echo "law-firm-postgres"
        elif docker ps --format '{{.Names}}' | grep -q "^law-firm-test-postgres$"; then
            echo "law-firm-test-postgres"
        elif docker ps --format '{{.Names}}' | grep -q "^dev-postgres$"; then
            echo "dev-postgres"
        elif docker ps --format '{{.Names}}' | grep -qE "postgres.*law"; then
            docker ps --format '{{.Names}}' | grep -E "postgres.*law" | head -1
        else
            # 尝试通过镜像名查找
            docker ps --filter "ancestor=postgres:15-alpine" --format '{{.Names}}' | head -1
        fi
    fi
}

CONTAINER_NAME=$(detect_postgres_container "${CONTAINER_NAME_PATTERN:-}")

# 解析参数
FORCE_FLAG=""
ENV_FLAG=""
for arg in "$@"; do
    case $arg in
        --force) FORCE_FLAG="true" ;;
        --dev)   ENV_FLAG="dev" ;;
        --test)  ENV_FLAG="test" ;;
        --prod)  ENV_FLAG="prod" ;;
    esac
done

# 根据参数或自动检测确定数据库名
if [ "$ENV_FLAG" == "prod" ]; then
    DB_NAME="law_firm"
    ENV_NAME="生产"
    CONTAINER_NAME_PATTERN="law-firm-postgres"
elif [ "$ENV_FLAG" == "test" ]; then
    DB_NAME="law_firm_test"
    ENV_NAME="测试"
    CONTAINER_NAME_PATTERN="law-firm-test-postgres"
elif [ "$ENV_FLAG" == "dev" ]; then
    DB_NAME="law_firm_dev"
    ENV_NAME="开发"
    CONTAINER_NAME_PATTERN="law-firm-postgres"
else
    # 自动检测：优先使用开发数据库
    if docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='law_firm_dev'" 2>/dev/null | grep -q 1; then
        DB_NAME="law_firm_dev"
        ENV_NAME="开发"
        CONTAINER_NAME_PATTERN="law-firm-postgres"
    elif docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='law_firm_test'" 2>/dev/null | grep -q 1; then
        DB_NAME="law_firm_test"
        ENV_NAME="测试"
        CONTAINER_NAME_PATTERN="law-firm-test-postgres"
    elif docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='law_firm'" 2>/dev/null | grep -q 1; then
        DB_NAME="law_firm"
        ENV_NAME="生产"
        CONTAINER_NAME_PATTERN="law-firm-postgres"
    else
        # 默认使用开发环境数据库名
        DB_NAME="law_firm_dev"
        ENV_NAME="开发"
        CONTAINER_NAME_PATTERN="law-firm-postgres"
    fi
    # 重新检测容器名
    CONTAINER_NAME=$(detect_postgres_container "${CONTAINER_NAME_PATTERN}")
fi

echo "=========================================="
echo "律师事务所管理系统 - 数据库重置"
echo "=========================================="
echo "环境: $ENV_NAME"
echo "数据库: $DB_NAME"
echo "=========================================="

# 检查容器是否运行
if [ -z "$CONTAINER_NAME" ] || ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "错误: PostgreSQL 容器未运行"
    echo ""
    echo "请先启动开发环境："
    echo "  cd docker"
    echo "  docker compose -f docker-compose.dev.yml up -d postgres"
    echo ""
    echo "或者运行开发环境启动脚本："
    echo "  ./scripts/ops/env-start.sh dev"
    exit 1
fi

echo "检测到 PostgreSQL 容器: $CONTAINER_NAME"

# 确认操作（除非使用 --force）
if [ "$FORCE_FLAG" != "true" ]; then
    read -p "警告: 此操作将删除所有数据并重新初始化数据库，是否继续? (y/N) " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "操作已取消"
        exit 0
    fi
fi

echo ""
echo "1. 断开现有连接并重建数据库..."
docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" > /dev/null 2>&1 || true
docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"

echo ""
echo "2. 执行初始化脚本..."

# 按顺序执行 SQL 文件
for sql_file in $(ls "$INIT_DB_DIR"/*.sql 2>/dev/null | sort); do
    filename=$(basename "$sql_file")
    echo "  执行: $filename"
    docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME < "$sql_file"
done

echo ""
echo "=========================================="
echo "数据库重置完成!"
echo "=========================================="
echo ""
echo "默认账号（密码统一为 admin123）:"
echo "  用户名: admin    角色: 管理员"
echo "  用户名: director 角色: 律所主任"
echo "  用户名: lawyer1  角色: 律师"
echo "  用户名: leader   角色: 团队负责人"
echo "  用户名: finance  角色: 财务"
echo "  用户名: staff    角色: 行政"
echo "  用户名: trainee  角色: 实习律师"
echo ""
