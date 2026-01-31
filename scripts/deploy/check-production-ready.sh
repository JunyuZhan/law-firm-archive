#!/bin/bash
# 生产环境部署前快速检查脚本
# 使用方法: ./scripts/check-production-ready.sh

set -e

# 获取项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

echo "=============================================="
echo "  生产环境部署前检查"
echo "=============================================="
echo ""

ERRORS=0
WARNINGS=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    ERRORS=$((ERRORS + 1))
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
    WARNINGS=$((WARNINGS + 1))
}

# 1. 检查环境变量文件
echo "【1/10】检查环境变量配置..."

# 优先检查项目根目录的 .env，兼容 docker/.env 位置
ENV_FILE=""
if [ -f ".env" ]; then
    ENV_FILE=".env"
elif [ -f "docker/.env" ]; then
    ENV_FILE="docker/.env"
fi

if [ -n "$ENV_FILE" ]; then
    source "$ENV_FILE"
    
    # 检查 JWT_SECRET
    if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "your_very_long_and_secure_jwt_secret_key_here_at_least_64_characters" ]; then
        check_fail "JWT_SECRET 未设置或使用默认值"
    elif [ ${#JWT_SECRET} -lt 32 ]; then
        check_warn "JWT_SECRET 长度不足32字符，建议至少64字符"
    else
        check_pass "JWT_SECRET 已配置"
    fi
    
    # 检查数据库密码
    if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" = "your_secure_db_password_here" ]; then
        check_fail "DB_PASSWORD 未设置或使用默认值"
    elif [ ${#DB_PASSWORD} -lt 12 ]; then
        check_warn "DB_PASSWORD 长度不足12字符，建议至少16字符"
    else
        check_pass "DB_PASSWORD 已配置"
    fi
    
    # 检查 MinIO 密钥
    if [ "$MINIO_ACCESS_KEY" = "minioadmin" ] || [ "$MINIO_SECRET_KEY" = "minioadmin" ]; then
        check_fail "MinIO 仍使用默认密钥 minioadmin"
    else
        check_pass "MinIO 密钥已修改"
    fi
    
    # 检查 Swagger
    if [ "$SWAGGER_ENABLED" = "true" ]; then
        check_warn "生产环境 Swagger UI 已启用，建议关闭"
    else
        check_pass "Swagger UI 已禁用"
    fi

    # 检查 OnlyOffice JWT 密钥
    if [ -z "$ONLYOFFICE_JWT_SECRET" ] || [ "$ONLYOFFICE_JWT_SECRET" = "law-firm-onlyoffice-default-secret-2024" ]; then
        if [ "$ONLYOFFICE_JWT_ENABLED" = "true" ]; then
            check_warn "OnlyOffice 使用了默认或空的 JWT 密钥，生产环境建议修改"
        fi
    else
        check_pass "OnlyOffice JWT 密钥已配置"
    fi
else
    check_fail "未找到 .env 文件，请先运行 ./scripts/deploy.sh 创建配置"
fi

echo ""

# 2. 检查配置文件
echo "【2/10】检查应用配置文件..."
if [ -f "backend/src/main/resources/application-prod.yml" ]; then
    check_pass "生产环境配置文件存在"
    
    # 检查日志级别
    if grep -q "com.lawfirm: debug" backend/src/main/resources/application-prod.yml; then
        check_warn "生产环境日志级别为 debug，建议改为 info"
    else
        check_pass "日志级别配置正确"
    fi
else
    check_fail "未找到 application-prod.yml 配置文件"
fi

echo ""

# 3. 检查 Docker 配置
echo "【3/10】检查 Docker 配置..."
if [ -f "docker/docker-compose.prod.yml" ]; then
    check_pass "Docker Compose 生产配置文件存在"
    
    # 检查重启策略
    if grep -q "restart: unless-stopped" docker/docker-compose.prod.yml; then
        check_pass "容器重启策略已配置"
    else
        check_warn "容器重启策略未配置"
    fi
else
    check_fail "未找到 docker-compose.prod.yml 文件"
fi

echo ""

# 4. 检查数据库初始化脚本
echo "【4/10】检查数据库初始化脚本..."
# 获取项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$SCRIPTS_DIR")"
cd "$PROJECT_ROOT"

if [ -f "scripts/init-db/01-system-schema.sql" ]; then
    check_pass "数据库初始化脚本存在"
else
    check_fail "未找到数据库初始化脚本"
fi

echo ""

# 5. 检查备份脚本
echo "【5/10】检查备份配置..."
if [ -f "scripts/ops/db-auto-backup.sh" ] || [ -f "scripts/ops/backup.sh" ]; then
    check_pass "备份脚本存在"
    echo "  提示: 请在生产服务器上配置定时任务(cron)定期执行备份脚本"
else
    check_warn "未找到备份脚本，建议配置自动备份"
fi

echo ""

# 6. 检查安全修复
echo "【6/10】检查安全修复..."
if grep -q "captchaId" backend/src/main/java/com/lawfirm/interfaces/rest/AuthController.java && \
   grep -q "captchaService.verifyCaptcha" backend/src/main/java/com/lawfirm/interfaces/rest/AuthController.java; then
    check_pass "登录验证码强制要求已实施"
else
    check_warn "请确认登录验证码强制要求已实施"
fi

if grep -q "shouldLockAccount" backend/src/main/java/com/lawfirm/application/system/service/AuthService.java; then
    check_pass "账户锁定机制已实施"
else
    check_warn "请确认账户锁定机制已实施"
fi

echo ""

# 7. 检查内存泄露修复
echo "【7/10】检查资源管理..."
if grep -q "try-with-resources\|try (" backend/src/main/java/com/lawfirm/application/document/service/DocumentAppService.java; then
    check_pass "文件流资源管理已优化"
else
    check_warn "请确认文件流资源管理已优化"
fi

echo ""

# 8. 检查依赖版本
echo "【8/10】检查依赖版本..."
if [ -f "backend/pom.xml" ]; then
    if command -v mvn &> /dev/null; then
        echo "  检查 Maven 依赖..."
        # 这里可以添加依赖检查逻辑
        check_pass "Maven 依赖文件存在"
    else
        check_warn "Maven 未安装，无法检查依赖版本"
    fi
else
    check_fail "未找到 pom.xml 文件"
fi

echo ""

# 9. 检查部署文档
echo "【9/10】检查部署文档..."
if [ -f "docs/PRODUCTION_DEPLOYMENT_CHECKLIST.md" ]; then
    check_pass "部署检查清单文档存在"
else
    check_warn "未找到部署检查清单文档"
fi

if [ -f "DEPLOY.md" ]; then
    check_pass "部署指南文档存在"
fi

echo ""

# 10. 检查敏感信息
echo "【10/10】检查敏感信息泄露..."
# 检查代码中是否包含硬编码密码
if grep -r "password.*=.*['\"].*123" backend/src/main/java --include="*.java" 2>/dev/null | grep -v "dev_password_123" | grep -v "minioadmin" | grep -v "test"; then
    check_warn "代码中可能包含硬编码密码，请检查"
else
    check_pass "未发现明显的硬编码密码"
fi

# 检查配置文件中的默认密码
if grep -q "dev_password_123\|minioadmin\|your-256-bit-secret" backend/src/main/resources/application-prod.yml 2>/dev/null; then
    check_fail "生产环境配置文件中包含默认密码或密钥"
else
    check_pass "生产环境配置文件未包含默认密码"
fi

echo ""

# 总结
echo "=============================================="
echo "  检查完成"
echo "=============================================="
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ 所有检查通过，可以部署生产环境${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ 发现 $WARNINGS 个警告，建议修复后再部署${NC}"
    exit 0
else
    echo -e "${RED}✗ 发现 $ERRORS 个错误和 $WARNINGS 个警告，必须修复后才能部署${NC}"
    echo ""
    echo "请参考 docs/PRODUCTION_DEPLOYMENT_CHECKLIST.md 进行详细检查"
    exit 1
fi
