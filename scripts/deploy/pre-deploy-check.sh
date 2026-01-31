#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 部署前统一检查脚本
# =====================================================
# 整合所有检查功能，在部署前运行
# 整合了 check-production-ready.sh 的所有检查项
# 用法: ./scripts/deploy/pre-deploy-check.sh
# =====================================================

set -e

# 获取项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$SCRIPTS_DIR")"
cd "$PROJECT_ROOT"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

ERRORS=0
WARNINGS=0

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}    ${BOLD}生产环境部署前检查${NC}                                    ${CYAN}║${NC}"
echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 检查函数
check_pass() {
    echo -e "  ${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "  ${RED}✗${NC} $1"
    ERRORS=$((ERRORS + 1))
}

check_warn() {
    echo -e "  ${YELLOW}⚠${NC} $1"
    WARNINGS=$((WARNINGS + 1))
}

check_info() {
    echo -e "  ${BLUE}ℹ${NC} $1"
}

# =====================================================
# 1. 检查 Docker 环境
# =====================================================
check_docker() {
    echo -e "${BOLD}【1/10】检查 Docker 环境...${NC}"
    
    if ! command -v docker &> /dev/null; then
        check_fail "Docker 未安装"
        echo "    安装命令: curl -fsSL https://get.docker.com | sh"
        return
    else
        check_pass "Docker 已安装 ($(docker --version | cut -d' ' -f3 | cut -d',' -f1))"
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        check_fail "Docker Compose 未安装"
        return
    else
        check_pass "Docker Compose 已安装"
    fi
    
    # 检查 Docker 服务是否运行
    if ! docker info &> /dev/null; then
        check_fail "Docker 服务未运行"
        echo "    启动命令: sudo systemctl start docker"
    else
        check_pass "Docker 服务运行正常"
    fi
    
    echo ""
}

# =====================================================
# 2. 检查环境变量配置
# =====================================================
check_env_config() {
    echo -e "${BOLD}【2/10】检查环境变量配置...${NC}"
    
    # 查找 .env 文件
    ENV_FILE=""
    if [ -f ".env" ]; then
        ENV_FILE=".env"
    elif [ -f "docker/.env" ]; then
        ENV_FILE="docker/.env"
    fi
    
    if [ -z "$ENV_FILE" ]; then
        check_fail "未找到 .env 文件"
        echo "    请运行: cp docker/env.example .env"
        echo "    然后编辑 .env 文件设置密码和密钥"
        echo ""
        return
    fi
    
    check_pass "找到 .env 文件: $ENV_FILE"
    source "$ENV_FILE"
    
    # 检查必需的密钥
    if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "your_very_long_and_secure_jwt_secret_key_here_at_least_64_characters" ]; then
        check_fail "JWT_SECRET 未设置或使用默认值"
    elif [ ${#JWT_SECRET} -lt 32 ]; then
        check_warn "JWT_SECRET 长度不足32字符，建议至少64字符"
    else
        check_pass "JWT_SECRET 已配置（${#JWT_SECRET} 字符）"
    fi
    
    if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" = "your-strong-db-password-here" ]; then
        check_fail "DB_PASSWORD 未设置或使用默认值"
    elif [ ${#DB_PASSWORD} -lt 12 ]; then
        check_warn "DB_PASSWORD 长度不足12字符，建议至少16字符"
    else
        check_pass "DB_PASSWORD 已配置（${#DB_PASSWORD} 字符）"
    fi
    
    if [ "$MINIO_ACCESS_KEY" = "minioadmin" ] || [ "$MINIO_SECRET_KEY" = "minioadmin" ]; then
        check_fail "MinIO 仍使用默认密钥 minioadmin"
    else
        check_pass "MinIO 密钥已修改"
    fi
    
    if [ -z "$ONLYOFFICE_JWT_SECRET" ] || [ "$ONLYOFFICE_JWT_SECRET" = "law-firm-onlyoffice-default-secret-2024" ]; then
        check_warn "OnlyOffice JWT_SECRET 未设置或使用默认值"
    else
        check_pass "OnlyOffice JWT_SECRET 已配置"
    fi
    
    if [ "$SWAGGER_ENABLED" = "true" ]; then
        check_warn "生产环境 Swagger UI 已启用，建议关闭"
    else
        check_pass "Swagger UI 已禁用"
    fi
    
    if [ -z "$REDIS_PASSWORD" ]; then
        check_warn "REDIS_PASSWORD 未设置，生产环境建议设置密码"
    else
        check_pass "Redis 密码已配置"
    fi
    
    echo ""
}

# =====================================================
# 3. 检查配置文件
# =====================================================
check_config_files() {
    echo -e "${BOLD}【3/10】检查应用配置文件...${NC}"
    
    if [ -f "backend/src/main/resources/application-prod.yml" ]; then
        check_pass "生产环境配置文件存在"
        
        # 检查日志级别
        if grep -q "com.lawfirm: debug" backend/src/main/resources/application-prod.yml; then
            check_warn "生产环境日志级别为 debug，建议改为 info"
        else
            check_pass "日志级别配置正确"
        fi
        
        # 检查是否包含默认密码
        if grep -q "dev_password_123\|minioadmin\|your-256-bit-secret" backend/src/main/resources/application-prod.yml 2>/dev/null; then
            check_fail "生产环境配置文件中包含默认密码或密钥"
        else
            check_pass "生产环境配置文件未包含默认密码"
        fi
    else
        check_fail "未找到 application-prod.yml 配置文件"
    fi
    
    if [ -f "docker/docker-compose.prod.yml" ]; then
        check_pass "Docker Compose 生产配置文件存在"
        
        if grep -q "restart: unless-stopped" docker/docker-compose.prod.yml; then
            check_pass "容器重启策略已配置"
        else
            check_warn "容器重启策略未配置"
        fi
        
        if grep -q "healthcheck:" docker/docker-compose.prod.yml; then
            check_pass "容器健康检查已配置"
        else
            check_warn "容器健康检查未配置"
        fi
    else
        check_fail "未找到 docker-compose.prod.yml 文件"
    fi
    
    echo ""
}

# =====================================================
# 4. 检查数据库初始化脚本
# =====================================================
check_database_scripts() {
    echo -e "${BOLD}【4/10】检查数据库初始化脚本...${NC}"
    
    if [ -f "scripts/init-db/01-system-schema.sql" ]; then
        check_pass "数据库初始化脚本存在"
        
        # 统计 SQL 文件数量
        SQL_COUNT=$(find scripts/init-db -name "*.sql" | wc -l | tr -d ' ')
        check_info "找到 $SQL_COUNT 个 SQL 初始化脚本"
    else
        check_fail "未找到数据库初始化脚本"
    fi
    
    echo ""
}

# =====================================================
# 5. 检查备份配置
# =====================================================
check_backup_config() {
    echo -e "${BOLD}【5/10】检查备份配置...${NC}"
    
    if [ -f "scripts/ops/db-auto-backup.sh" ] || [ -f "scripts/ops/backup.sh" ]; then
        check_pass "备份脚本存在"
        check_info "提示: 请在生产服务器上配置定时任务(cron)定期执行备份脚本"
    else
        check_warn "未找到备份脚本，建议配置自动备份"
    fi
    
    echo ""
}

# =====================================================
# 6. 检查敏感信息泄露
# =====================================================
check_sensitive_info() {
    echo -e "${BOLD}【6/10】检查敏感信息泄露...${NC}"
    
    # 检查 .env 是否被 git 忽略
    if git ls-files .env --error-unmatch 2>/dev/null; then
        check_fail ".env 未被 .gitignore 忽略"
    else
        check_pass ".env 已被 git 忽略"
    fi
    
    # 检查代码中是否包含硬编码密码
    HARDCODED=$(grep -r --include="*.java" --include="*.ts" --include="*.vue" \
        -E "(password|secret|apikey|api_key)\s*=\s*['\"][^'\"]+['\"]" \
        backend/src frontend/apps 2>/dev/null | \
        grep -v "placeholder\|example\|test\|mock\|dev_password_123\|minioadmin\|show-forget-password\|show-remember-me\|show-code-login\|show-qrcode-login\|show-register\|show-third-party-login\|generateRandomPassword\|LawFirm@2026" | head -3)
    
    if [ -n "$HARDCODED" ]; then
        check_warn "可能存在硬编码的密钥"
        echo "$HARDCODED" | while read line; do
            echo "    $line"
        done
    else
        check_pass "未发现明显的硬编码密钥"
    fi
    
    echo ""
}

# =====================================================
# 7. 检查部署文档
# =====================================================
check_documentation() {
    echo -e "${BOLD}【7/10】检查部署文档...${NC}"
    
    if [ -f "docs/PRODUCTION_DEPLOYMENT_CHECKLIST.md" ]; then
        check_pass "部署检查清单文档存在"
    else
        check_warn "未找到部署检查清单文档"
    fi
    
    if [ -f "docs/PRODUCTION_CONFIG_CHECK_REPORT.md" ]; then
        check_pass "配置检查报告存在"
    fi
    
    if [ -f "docs/PRODUCTION_QUICK_START.md" ]; then
        check_pass "快速部署指南存在"
    fi
    
    echo ""
}

# =====================================================
# 8. 检查安全功能
# =====================================================
check_security_features() {
    echo -e "${BOLD}【8/10】检查安全功能...${NC}"
    
    # 检查登录验证码
    if grep -q "captchaId" backend/src/main/java/com/lawfirm/interfaces/rest/AuthController.java && \
       grep -q "captchaService.verifyCaptcha" backend/src/main/java/com/lawfirm/interfaces/rest/AuthController.java 2>/dev/null; then
        check_pass "登录验证码强制要求已实施"
    else
        check_warn "请确认登录验证码强制要求已实施"
    fi
    
    # 检查账户锁定机制
    if grep -q "shouldLockAccount" backend/src/main/java/com/lawfirm/application/system/service/AuthService.java 2>/dev/null; then
        check_pass "账户锁定机制已实施"
    else
        check_warn "请确认账户锁定机制已实施"
    fi
    
    # 检查文件流资源管理
    if grep -q "try-with-resources\|try (" backend/src/main/java/com/lawfirm/application/document/service/DocumentAppService.java 2>/dev/null; then
        check_pass "文件流资源管理已优化"
    else
        check_warn "请确认文件流资源管理已优化"
    fi
    
    echo ""
}

# =====================================================
# 9. 检查依赖版本
# =====================================================
check_dependencies() {
    echo -e "${BOLD}【9/10】检查依赖版本...${NC}"
    
    if [ -f "backend/pom.xml" ]; then
        if command -v mvn &> /dev/null; then
            check_info "Maven 依赖文件存在"
            check_info "提示: 建议运行 'mvn dependency-check:check' 检查依赖漏洞"
        else
            check_warn "Maven 未安装，无法检查依赖版本"
        fi
    else
        check_fail "未找到 pom.xml 文件"
    fi
    
    echo ""
}

# =====================================================
# 10. 检查系统资源
# =====================================================
check_system_resources() {
    echo -e "${BOLD}【10/10】检查系统资源...${NC}"
    
    # 检查磁盘空间（至少需要 10GB）
    if command -v df &> /dev/null; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            AVAILABLE=$(df -g . | tail -1 | awk '{print $4}')
        else
            # Linux
            AVAILABLE=$(df -BG . | tail -1 | awk '{print $4}' | sed 's/G//')
        fi
        if [ "$AVAILABLE" -lt 10 ]; then
            check_warn "可用磁盘空间不足 10GB（当前: ${AVAILABLE}GB）"
        else
            check_pass "磁盘空间充足（可用: ${AVAILABLE}GB）"
        fi
    fi
    
    # 检查内存（建议至少 4GB）
    if command -v free &> /dev/null; then
        TOTAL_MEM=$(free -g | awk '/^Mem:/{print $2}')
        if [ "$TOTAL_MEM" -lt 4 ]; then
            check_warn "系统内存不足 4GB（当前: ${TOTAL_MEM}GB），可能影响性能"
        else
            check_pass "系统内存充足（${TOTAL_MEM}GB）"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        TOTAL_MEM=$(sysctl -n hw.memsize | awk '{print int($1/1024/1024/1024)}')
        if [ "$TOTAL_MEM" -lt 4 ]; then
            check_warn "系统内存不足 4GB（当前: ${TOTAL_MEM}GB），可能影响性能"
        else
            check_pass "系统内存充足（${TOTAL_MEM}GB）"
        fi
    fi
    
    echo ""
}

# =====================================================
# 主程序
# =====================================================
main() {
    check_docker
    check_env_config
    check_config_files
    check_database_scripts
    check_backup_config
    check_sensitive_info
    check_documentation
    check_security_features
    check_dependencies
    check_system_resources
    
    # 总结
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}    ${BOLD}检查完成${NC}                                            ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
        echo -e "${GREEN}✓ 所有检查通过，可以部署生产环境${NC}"
        echo ""
        echo "下一步："
        echo "  运行部署脚本: ./scripts/deploy.sh 或 ./scripts/deploy/deploy.sh"
        exit 0
    elif [ $ERRORS -eq 0 ]; then
        echo -e "${YELLOW}⚠ 发现 $WARNINGS 个警告，建议修复后再部署${NC}"
        echo ""
        echo "建议："
        echo "  1. 查看警告项并修复"
        echo "  2. 参考文档: docs/PRODUCTION_DEPLOYMENT_CHECKLIST.md"
        echo "  3. 修复后重新运行检查: ./scripts/pre-deploy-check.sh"
        exit 0
    else
        echo -e "${RED}✗ 发现 $ERRORS 个错误和 $WARNINGS 个警告，必须修复后才能部署${NC}"
        echo ""
        echo "必须修复的错误："
        echo "  1. 检查上述错误项"
        echo "  2. 参考文档: docs/PRODUCTION_DEPLOYMENT_CHECKLIST.md"
        echo "  3. 修复后重新运行检查: ./scripts/pre-deploy-check.sh"
        exit 1
    fi
}

main "$@"
