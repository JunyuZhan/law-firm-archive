#!/bin/bash
# 重置数据库脚本
# 用法: ./scripts/reset-db.sh

set -e

echo "=== 智慧律所管理系统 - 数据库重置脚本 ==="
echo ""

# 检查docker是否运行
if ! docker ps > /dev/null 2>&1; then
    echo "错误: Docker未运行，请先启动Docker"
    exit 1
fi

# 检查postgres容器是否运行
if ! docker ps | grep -q law-postgres; then
    echo "错误: law-postgres容器未运行"
    echo "请先运行: cd docker && docker-compose up -d postgres"
    exit 1
fi

echo "1. 停止后端服务（如果正在运行）..."
echo "   请确保后端服务已停止"
echo ""

echo "2. 断开所有连接并删除数据库..."
# 先终止所有连接到目标数据库的会话
docker exec -i law-postgres psql -U law_admin -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'law_firm_dev' AND pid <> pg_backend_pid();" 2>/dev/null || true
# 等待一下确保连接已断开
sleep 1
# 删除数据库
docker exec -i law-postgres psql -U law_admin -d postgres -c "DROP DATABASE IF EXISTS law_firm_dev;"
# 创建新数据库
docker exec -i law-postgres psql -U law_admin -d postgres -c "CREATE DATABASE law_firm_dev;"

echo "3. 执行初始化脚本..."

# 按顺序执行所有SQL脚本
for script in scripts/init-db/*.sql; do
    if [ -f "$script" ]; then
        echo "   执行: $script"
        docker exec -i law-postgres psql -U law_admin -d law_firm_dev < "$script"
    fi
done

echo ""
echo "=== 数据库重置完成 ==="
echo ""
echo "默认管理员账号:"
echo "  用户名: admin"
echo "  密码: admin123"
echo ""
echo "请重启后端服务后登录系统"
