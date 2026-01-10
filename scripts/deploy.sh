#!/bin/bash
# 智慧律所管理系统 - 一键部署脚本

set -e

echo "=============================================="
echo "    智慧律所管理系统 - Docker 部署"
echo "=============================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}❌ Docker Compose 未安装，请先安装 Docker Compose${NC}"
    exit 1
fi

# 切换到项目根目录
cd "$(dirname "$0")/.."
PROJECT_ROOT=$(pwd)

# =====================================================
# 自动生成安全密钥函数
# =====================================================
generate_secret() {
    openssl rand -base64 32 2>/dev/null || head -c 32 /dev/urandom | base64
}

generate_password() {
    openssl rand -base64 16 2>/dev/null || head -c 16 /dev/urandom | base64
}

# =====================================================
# 检查并创建 .env 文件
# =====================================================
ENV_FILE="docker/.env"
ENV_EXAMPLE="docker/env.example"

if [ ! -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}⚠️  未找到 .env 文件，正在自动创建...${NC}"
    cp "$ENV_EXAMPLE" "$ENV_FILE"
fi

# 读取当前配置
source "$ENV_FILE"

# 标记是否有更新
UPDATED=false

# =====================================================
# 自动生成 JWT_SECRET
# =====================================================
if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "your_very_long_and_secure_jwt_secret_key_here_at_least_64_characters" ]; then
    NEW_JWT_SECRET=$(generate_secret)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|JWT_SECRET=.*|JWT_SECRET=$NEW_JWT_SECRET|" "$ENV_FILE"
    else
        sed -i "s|JWT_SECRET=.*|JWT_SECRET=$NEW_JWT_SECRET|" "$ENV_FILE"
    fi
    echo -e "${GREEN}✅ 已自动生成 JWT_SECRET${NC}"
    UPDATED=true
fi

# =====================================================
# 自动生成 DB_PASSWORD
# =====================================================
if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" = "your_secure_db_password_here" ]; then
    NEW_DB_PASSWORD=$(generate_password)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|DB_PASSWORD=.*|DB_PASSWORD=$NEW_DB_PASSWORD|" "$ENV_FILE"
    else
        sed -i "s|DB_PASSWORD=.*|DB_PASSWORD=$NEW_DB_PASSWORD|" "$ENV_FILE"
    fi
    echo -e "${GREEN}✅ 已自动生成 DB_PASSWORD${NC}"
    UPDATED=true
fi

# =====================================================
# 自动生成 MINIO_ACCESS_KEY（如果是默认值 minioadmin）
# =====================================================
if [ -z "$MINIO_ACCESS_KEY" ] || [ "$MINIO_ACCESS_KEY" = "minioadmin" ]; then
    NEW_MINIO_ACCESS="lawfirm_minio_$(openssl rand -hex 4 2>/dev/null || head -c 4 /dev/urandom | xxd -p)"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|MINIO_ACCESS_KEY=.*|MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS|" "$ENV_FILE"
    else
        sed -i "s|MINIO_ACCESS_KEY=.*|MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS|" "$ENV_FILE"
    fi
    echo -e "${GREEN}✅ 已自动生成 MINIO_ACCESS_KEY${NC}"
    UPDATED=true
fi

# =====================================================
# 自动生成 MINIO_SECRET_KEY
# =====================================================
if [ -z "$MINIO_SECRET_KEY" ] || [ "$MINIO_SECRET_KEY" = "your_secure_minio_password_here" ] || [ "$MINIO_SECRET_KEY" = "minioadmin" ]; then
    NEW_MINIO_SECRET=$(generate_password)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=$NEW_MINIO_SECRET|" "$ENV_FILE"
    else
        sed -i "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=$NEW_MINIO_SECRET|" "$ENV_FILE"
    fi
    echo -e "${GREEN}✅ 已自动生成 MINIO_SECRET_KEY${NC}"
    UPDATED=true
fi

# =====================================================
# 显示生成的密钥（首次部署时）
# =====================================================
if [ "$UPDATED" = true ]; then
    echo ""
    echo -e "${YELLOW}📝 已自动生成安全密钥，配置已保存到 docker/.env${NC}"
    echo -e "${YELLOW}   请妥善保管此文件，密钥仅生成一次！${NC}"
    echo ""
    # 重新读取配置
    source "$ENV_FILE"
fi

# =====================================================
# 运行生产环境检查脚本（可选）
# =====================================================
if [ -f "scripts/check-production-ready.sh" ] && [ "${SKIP_CHECK:-}" != "true" ]; then
    echo ""
    echo "🔍 运行生产环境检查..."
    echo ""
    if ! bash scripts/check-production-ready.sh; then
        echo ""
        echo -e "${YELLOW}⚠️  部分检查项未通过，但可以继续部署${NC}"
        echo "   详细检查清单请参考: docs/PRODUCTION_DEPLOYMENT_CHECKLIST.md"
        echo ""
        read -p "是否继续部署？(y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
        fi
    fi
    echo ""
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
echo -e "${GREEN}    ✅ 部署完成！${NC}"
echo "=============================================="
echo ""
echo "🌐 访问地址：http://localhost"
echo "🔧 API 地址：http://localhost/api"
echo "📦 MinIO 控制台：http://localhost:9001"
echo ""
echo "📋 查看日志：cd docker && docker compose -f docker-compose.prod.yml logs -f"
echo "🛑 停止服务：cd docker && docker compose -f docker-compose.prod.yml down"
echo ""
