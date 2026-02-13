#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 生产环境安全检查脚本
# =====================================================
# 在部署前运行此脚本检查安全配置
# 用法: ./scripts/security-check.sh
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

echo "=========================================="
echo "🔐 律所系统生产环境安全检查"
echo "=========================================="
echo ""

# 检查 .env 文件
check_env_file() {
    echo "📋 检查环境配置..."
    
    if [ ! -f ".env" ]; then
        echo -e "${RED}❌ .env 文件不存在${NC}"
        echo "   请复制 .env.example 并配置: cp .env.example .env"
        ((ERRORS++))
        return
    fi
    
    # 加载环境变量
    source .env
    
    # 检查必需的密钥
    check_required_secret "DB_PASSWORD" "$DB_PASSWORD" "数据库密码"
    check_required_secret "JWT_SECRET" "$JWT_SECRET" "JWT 密钥" 64
    check_required_secret "MINIO_SECRET_KEY" "$MINIO_SECRET_KEY" "MinIO 密钥"
    check_required_secret "ONLYOFFICE_JWT_SECRET" "$ONLYOFFICE_JWT_SECRET" "OnlyOffice JWT 密钥"
    check_required_secret "OCR_API_KEY" "$OCR_API_KEY" "OCR API Key"
    
    # 检查 Redis 密码
    if [ -z "$REDIS_PASSWORD" ]; then
        echo -e "${YELLOW}⚠️  REDIS_PASSWORD 未设置${NC}"
        echo "   生产环境建议设置 Redis 密码"
        ((WARNINGS++))
    fi
    
    # 检查 Swagger 是否关闭
    if [ "$SWAGGER_ENABLED" = "true" ]; then
        echo -e "${YELLOW}⚠️  SWAGGER_ENABLED=true${NC}"
        echo "   生产环境应关闭 API 文档"
        ((WARNINGS++))
    fi
    
    # 检查默认密钥
    check_not_default "DB_PASSWORD" "$DB_PASSWORD" "your_secure_db_password_here"
    check_not_default "JWT_SECRET" "$JWT_SECRET" "your_very_long_and_secure_jwt_secret_key_here_at_least_64_characters"
    check_not_default "MINIO_SECRET_KEY" "$MINIO_SECRET_KEY" "your_secure_minio_password_here"
    check_not_default "ONLYOFFICE_JWT_SECRET" "$ONLYOFFICE_JWT_SECRET" "your_onlyoffice_jwt_secret_change_in_production"
    check_not_default "OCR_API_KEY" "$OCR_API_KEY" "your_ocr_api_key_here"
}

check_required_secret() {
    local name=$1
    local value=$2
    local desc=$3
    local min_length=${4:-16}
    
    if [ -z "$value" ]; then
        echo -e "${RED}❌ $name 未设置${NC}"
        echo "   $desc 必须配置"
        ((ERRORS++))
    elif [ ${#value} -lt $min_length ]; then
        echo -e "${YELLOW}⚠️  $name 长度不足 $min_length 字符${NC}"
        echo "   建议使用更长的密钥"
        ((WARNINGS++))
    else
        echo -e "${GREEN}✓ $name 已配置${NC}"
    fi
}

check_not_default() {
    local name=$1
    local value=$2
    local default=$3
    
    if [ "$value" = "$default" ]; then
        echo -e "${RED}❌ $name 使用了默认值${NC}"
        echo "   请修改为强随机值"
        ((ERRORS++))
    fi
}

# 检查敏感文件
check_sensitive_files() {
    echo ""
    echo "📁 检查敏感文件..."
    
    # 检查 .env 是否被 git 忽略
    if git ls-files .env --error-unmatch 2>/dev/null; then
        echo -e "${RED}❌ .env 未被 .gitignore 忽略${NC}"
        echo "   请将 .env 添加到 .gitignore"
        ((ERRORS++))
    else
        echo -e "${GREEN}✓ .env 已被 git 忽略${NC}"
    fi
    
    # 检查是否有硬编码的密钥
    echo "   检查硬编码密钥..."
    local found_secrets=$(grep -r --include="*.java" --include="*.ts" --include="*.vue" -E "(password|secret|apikey|api_key)\s*=\s*['\"][^'\"]+['\"]" backend/src frontend/apps 2>/dev/null | grep -v "placeholder\|example\|test\|mock" | head -5)
    
    if [ -n "$found_secrets" ]; then
        echo -e "${YELLOW}⚠️  可能存在硬编码的密钥${NC}"
        echo "$found_secrets" | head -3
        ((WARNINGS++))
    else
        echo -e "${GREEN}✓ 未发现明显的硬编码密钥${NC}"
    fi
}

# 检查 Docker 配置
check_docker_config() {
    echo ""
    echo "🐳 检查 Docker 配置..."
    
    # 检查生产配置文件
    if [ ! -f "docker/docker-compose.yml" ]; then
        echo -e "${RED}❌ docker-compose.yml 不存在${NC}"
        ((ERRORS++))
    else
        echo -e "${GREEN}✓ docker-compose.yml 存在${NC}"
        
        # 检查是否使用了健康检查
        if grep -q "healthcheck:" docker/docker-compose.yml; then
            echo -e "${GREEN}✓ 配置了健康检查${NC}"
        else
            echo -e "${YELLOW}⚠️  建议配置容器健康检查${NC}"
            ((WARNINGS++))
        fi
    fi
}

# 检查依赖漏洞
check_dependencies() {
    echo ""
    echo "📦 检查依赖安全..."
    
    # 检查前端依赖
    if [ -f "frontend/pnpm-lock.yaml" ]; then
        echo "   前端: 请运行 'cd frontend && pnpm audit' 检查漏洞"
    fi
    
    # 检查后端依赖
    if [ -f "backend/pom.xml" ]; then
        echo "   后端: 请运行 'cd backend && mvn org.owasp:dependency-check-maven:check' 检查漏洞"
    fi
}

# 检查系统配置提示
check_system_config() {
    echo ""
    echo "📧 系统配置提示..."
    echo "   以下配置需要在系统管理模块的前端界面中配置："
    echo "   - 邮件服务器配置（SMTP）"
    echo "   - 告警通知接收邮箱"
    echo "   - 企业微信/钉钉通知（可选）"
    echo "   部署后请登录系统 -> 系统管理 -> 系统配置 进行设置"
}

# 检查 SSL/TLS 配置
check_ssl() {
    echo ""
    echo "🔒 检查 SSL/TLS 配置..."
    
    if [ -f "docker/nginx/ssl.conf" ] || [ -f "docker/nginx/nginx-ssl.conf" ]; then
        echo -e "${GREEN}✓ 发现 SSL 配置文件${NC}"
    else
        echo -e "${YELLOW}⚠️  未发现 SSL 配置${NC}"
        echo "   生产环境必须使用 HTTPS"
        ((WARNINGS++))
    fi
}

# 主程序
main() {
    check_env_file
    check_sensitive_files
    check_docker_config
    check_dependencies
    check_ssl
    check_system_config
    
    echo ""
    echo "=========================================="
    echo "📊 检查结果"
    echo "=========================================="
    
    if [ $ERRORS -gt 0 ]; then
        echo -e "${RED}❌ 发现 $ERRORS 个错误${NC}"
    fi
    
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠️  发现 $WARNINGS 个警告${NC}"
    fi
    
    if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
        echo -e "${GREEN}✓ 所有检查通过！${NC}"
    fi
    
    echo ""
    
    if [ $ERRORS -gt 0 ]; then
        echo -e "${RED}请修复所有错误后再部署到生产环境${NC}"
        exit 1
    fi
    
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}建议处理警告后再部署${NC}"
    fi
    
    echo -e "${GREEN}安全检查完成${NC}"
}

main
