#!/bin/bash

# 智慧律所管理系统 - 数据库重置脚本
# 整合日期: 2026-01-05

set -e

CONTAINER_NAME="law-postgres"
DB_NAME="law_firm_dev"
DB_USER="law_admin"

echo "=========================================="
echo "智慧律所管理系统 - 数据库重置"
echo "=========================================="

# 检查容器是否运行
if ! docker ps | grep -q $CONTAINER_NAME; then
    echo "错误: PostgreSQL 容器未运行"
    exit 1
fi

# 确认操作
read -p "警告: 此操作将删除所有数据并重新初始化数据库，是否继续? (y/N) " confirm
if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
    echo "操作已取消"
    exit 0
fi

echo ""
echo "1. 断开现有连接并重建数据库..."
docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" > /dev/null 2>&1
docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
docker exec $CONTAINER_NAME psql -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"

echo ""
echo "2. 执行表结构脚本..."

# 表结构脚本
for script in scripts/init-db/0[0-9]-*.sql scripts/init-db/1[0-5]-*.sql; do
    if [ -f "$script" ]; then
        echo "   执行: $script"
        docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME < "$script" > /dev/null 2>&1
    fi
done

echo ""
echo "3. 执行初始化数据脚本..."

# 初始化数据脚本
for script in scripts/init-db/2[0-9]-*.sql; do
    if [ -f "$script" ]; then
        echo "   执行: $script"
        docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME < "$script" > /dev/null 2>&1
    fi
done

echo ""
echo "=========================================="
echo "数据库重置完成!"
echo "=========================================="
