#!/bin/bash
# 智慧律所管理系统 - 一键部署脚本

set -e

echo "=============================================="
echo "    智慧律所管理系统 - Docker 部署"
echo "=============================================="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

# 切换到项目根目录
cd "$(dirname "$0")/.."

# 检查 .env 文件（在 docker 目录下）
if [ ! -f docker/.env ]; then
    echo "⚠️  未找到 docker/.env 文件，正在从模板创建..."
    cp docker/env.example docker/.env
    echo "📝 请编辑 docker/.env 文件配置密码后重新运行此脚本"
    echo "   vim docker/.env"
    exit 1
fi

# 检查关键配置
source docker/.env
if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "your_very_long_and_secure_jwt_secret_key_here_at_least_64_characters" ]; then
    echo "❌ 请修改 docker/.env 中的 JWT_SECRET"
    echo "   生成方法：openssl rand -base64 64"
    exit 1
fi

if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" = "your_secure_db_password_here" ]; then
    echo "❌ 请修改 docker/.env 中的 DB_PASSWORD"
    exit 1
fi

echo ""
echo "🚀 开始部署..."
echo ""

# 切换到 docker 目录
cd docker

# 构建并启动（使用生产环境配置）
docker compose -f docker-compose.prod.yml up -d --build

echo ""
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "📊 服务状态："
docker compose -f docker-compose.prod.yml ps

echo ""
echo "=============================================="
echo "    部署完成！"
echo "=============================================="
echo ""
echo "🌐 访问地址：http://localhost"
echo "🔧 API 地址：http://localhost/api"
echo "📦 MinIO 控制台：http://localhost:9001"
echo ""
echo "📋 查看日志：cd docker && docker compose -f docker-compose.prod.yml logs -f"
echo "🛑 停止服务：cd docker && docker compose -f docker-compose.prod.yml down"
echo ""

