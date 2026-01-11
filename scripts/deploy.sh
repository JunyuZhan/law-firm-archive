#!/bin/bash
# 律师事务所管理系统 - 一键部署脚本

set -e

# 从前端配置读取律所名称
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_ENV="$SCRIPT_DIR/../frontend/apps/web-antd/.env"
if [ -f "$FRONTEND_ENV" ]; then
    FIRM_NAME=$(grep "^VITE_APP_TITLE=" "$FRONTEND_ENV" | cut -d'=' -f2)
else
    FIRM_NAME="律师事务所"
fi

echo "=============================================="
echo "    ${FIRM_NAME} - Docker 部署"
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

# 标记是否是首次创建
FIRST_TIME=false

if [ ! -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}⚠️  未找到 .env 文件，正在自动创建...${NC}"
    cp "$ENV_EXAMPLE" "$ENV_FILE"
    FIRST_TIME=true
fi

# 读取当前配置
source "$ENV_FILE"

# =====================================================
# 只在首次创建时自动生成所有密钥
# =====================================================
if [ "$FIRST_TIME" = true ]; then
    echo -e "${GREEN}🔐 首次部署，自动生成安全密钥...${NC}"
    
    # 生成 JWT_SECRET
    NEW_JWT_SECRET=$(generate_secret)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|JWT_SECRET=.*|JWT_SECRET=$NEW_JWT_SECRET|" "$ENV_FILE"
    else
        sed -i "s|JWT_SECRET=.*|JWT_SECRET=$NEW_JWT_SECRET|" "$ENV_FILE"
    fi
    echo -e "${GREEN}   ✅ JWT_SECRET${NC}"
    
    # 生成 DB_PASSWORD
    NEW_DB_PASSWORD=$(generate_password)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|DB_PASSWORD=.*|DB_PASSWORD=$NEW_DB_PASSWORD|" "$ENV_FILE"
    else
        sed -i "s|DB_PASSWORD=.*|DB_PASSWORD=$NEW_DB_PASSWORD|" "$ENV_FILE"
    fi
    echo -e "${GREEN}   ✅ DB_PASSWORD${NC}"
    
    # 生成 MINIO_ACCESS_KEY
    NEW_MINIO_ACCESS="lawfirm_minio_$(openssl rand -hex 4 2>/dev/null || head -c 4 /dev/urandom | xxd -p)"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|MINIO_ACCESS_KEY=.*|MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS|" "$ENV_FILE"
    else
        sed -i "s|MINIO_ACCESS_KEY=.*|MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS|" "$ENV_FILE"
    fi
    echo -e "${GREEN}   ✅ MINIO_ACCESS_KEY${NC}"
    
    # 生成 MINIO_SECRET_KEY
    NEW_MINIO_SECRET=$(generate_password)
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=$NEW_MINIO_SECRET|" "$ENV_FILE"
    else
        sed -i "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=$NEW_MINIO_SECRET|" "$ENV_FILE"
    fi
    echo -e "${GREEN}   ✅ MINIO_SECRET_KEY${NC}"
    
    echo ""
    echo -e "${YELLOW}📝 安全密钥已保存到 docker/.env${NC}"
    echo -e "${YELLOW}   请妥善保管此文件！${NC}"
    echo ""
    
    # 重新读取配置
    source "$ENV_FILE"
else
    # =====================================================
    # 非首次部署：只检查是否有危险的默认值，警告但不修改
    # =====================================================
    HAS_UNSAFE=false
    
    if [ "$MINIO_ACCESS_KEY" = "minioadmin" ] || [ "$MINIO_SECRET_KEY" = "minioadmin" ]; then
        echo -e "${RED}⚠️  警告：MinIO 使用了不安全的默认密钥 minioadmin${NC}"
        HAS_UNSAFE=true
    fi
    
    if [ "$DB_PASSWORD" = "your_secure_db_password_here" ] || [ -z "$DB_PASSWORD" ]; then
        echo -e "${RED}⚠️  警告：数据库密码未正确配置${NC}"
        HAS_UNSAFE=true
    fi
    
    if [ "$HAS_UNSAFE" = true ]; then
        echo ""
        echo -e "${YELLOW}请手动编辑 docker/.env 文件修改不安全的配置${NC}"
        echo -e "${YELLOW}修改后需要重建相关服务的数据卷${NC}"
        echo ""
        read -p "是否继续部署？(y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo -e "${GREEN}✅ 使用现有的 .env 配置${NC}"
    fi
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
echo -e "${GREEN}    ✅ ${FIRM_NAME} - 部署完成！${NC}"
echo "=============================================="
echo ""
echo "🌐 主应用：http://localhost"
echo "📚 文档站点：http://localhost/docs/"
echo "🔧 API 地址：http://localhost/api"
echo "📦 MinIO 控制台：http://localhost:9001"
echo ""
echo "👤 默认账号（密码统一为 admin123）："
echo "   admin / director / lawyer1 / leader / finance / staff / trainee"
echo ""
echo "📋 查看日志：cd docker && docker compose -f docker-compose.prod.yml logs -f"
echo "🛑 停止服务：cd docker && docker compose -f docker-compose.prod.yml down"
echo ""
