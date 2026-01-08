#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 数据库重置脚本
# =====================================================
# 版本: 1.0.0
# 更新日期: 2026-01-08
# 用法: ./reset-db.sh [--force]
# =====================================================

set -e

CONTAINER_NAME="law-firm-postgres"
DB_NAME="law_firm_dev"
DB_USER="law_admin"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INIT_DB_DIR="$SCRIPT_DIR/init-db"

echo "=========================================="
echo "律师事务所管理系统 - 数据库重置"
echo "=========================================="

# 检查容器是否运行
if ! docker ps | grep -q $CONTAINER_NAME; then
    echo "错误: PostgreSQL 容器未运行"
    exit 1
fi

# 确认操作（除非使用 --force）
if [ "$1" != "--force" ]; then
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

# 使用 init-database.sh
cd "$INIT_DB_DIR"
./init-database.sh --docker

echo ""
echo "=========================================="
echo "数据库重置完成!"
echo "=========================================="
echo ""
echo "默认账号:"
echo "  用户名: admin    密码: admin123    角色: 管理员"
echo "  用户名: director 密码: lawyer123   角色: 律所主任"
echo "  用户名: lawyer1  密码: lawyer123   角色: 律师"
echo ""
