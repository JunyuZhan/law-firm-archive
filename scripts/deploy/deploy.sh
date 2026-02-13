#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 引导式部署脚本
# =====================================================
# 用法:
#   ./deploy.sh              # 引导式部署（交互选择）
#   ./deploy.sh --quick      # 快速部署（单机模式，跳过引导）
#   ./deploy.sh --mode=swarm # 指定部署模式
# =====================================================

set -e

# =====================================================
# 基础配置
# =====================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$SCRIPTS_DIR")"
DOCKER_DIR="$PROJECT_ROOT/docker"

# 从前端配置读取律所名称
FRONTEND_ENV="$PROJECT_ROOT/frontend/apps/web-antd/.env"
if [ -f "$FRONTEND_ENV" ]; then
    FIRM_NAME=$(grep "^VITE_APP_TITLE=" "$FRONTEND_ENV" | cut -d'=' -f2)
else
    FIRM_NAME="律师事务所"
fi

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# 部署模式
DEPLOY_MODE=""
INTERACTIVE=true
NAS_IP=""
NAS_PATH=""
INIT_DEMO_DATA=false
DEMO_DATA_LEVEL="full"
NO_CACHE=false

# =====================================================
# 工具函数
# =====================================================
log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }

# 检测服务器 IP 地址（优先获取局域网 IP）
detect_server_ip() {
    local ip=""
    # Linux: 尝试获取默认网关对应的 IP
    if command -v ip &> /dev/null; then
        ip=$(ip route get 1.1.1.1 2>/dev/null | grep -oP 'src \K[\d.]+' | head -1)
    fi
    # macOS: 使用 route 命令
    if [ -z "$ip" ] && command -v route &> /dev/null; then
        local iface=$(route -n get default 2>/dev/null | grep 'interface:' | awk '{print $2}')
        if [ -n "$iface" ]; then
            ip=$(ifconfig "$iface" 2>/dev/null | grep 'inet ' | awk '{print $2}')
        fi
    fi
    # 备选方案：hostname -I
    if [ -z "$ip" ] && command -v hostname &> /dev/null; then
        ip=$(hostname -I 2>/dev/null | awk '{print $1}')
    fi
    # 最后备选：localhost
    if [ -z "$ip" ]; then
        ip="localhost"
    fi
    echo "$ip"
}

print_banner() {
    clear
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}    ${BOLD}${FIRM_NAME}${NC}                                  ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}    ${DIM}律师事务所管理系统 - 部署向导${NC}                          ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_divider() {
    echo -e "${DIM}──────────────────────────────────────────────────────────────────${NC}"
}

# 显示部署模式选择菜单
show_deploy_menu() {
    print_banner
    echo -e "${BOLD}请选择部署方式：${NC}"
    echo ""
    echo -e "  ${GREEN}1)${NC} ${BOLD}单机部署${NC} ${DIM}(推荐小型律所/开发测试)${NC}"
    echo -e "     ${DIM}└─ 所有服务运行在一台服务器，最简单的部署方式${NC}"
    echo ""
    echo -e "  ${MAGENTA}2)${NC} ${BOLD}Docker Swarm 分布式部署${NC} ${DIM}(推荐大型律所)${NC}"
    echo -e "     ${DIM}└─ 多节点集群，支持高可用和水平扩展${NC}"
    echo ""
    print_divider
    echo -e "  ${DIM}0) 退出${NC}"
    echo ""
    
    read -p "请输入选项 [1-2]: " choice
    
    case $choice in
        1) DEPLOY_MODE="standalone" ;;
        2) DEPLOY_MODE="swarm" ;;
        0) echo "已取消部署"; exit 0 ;;
        *) 
            log_error "无效选项，请重新选择"
            sleep 1
            show_deploy_menu
            ;;
    esac
}

# 显示部署确认信息
show_deploy_summary() {
    print_banner
    echo -e "${BOLD}部署配置确认：${NC}"
    echo ""
    
    case $DEPLOY_MODE in
        standalone)
            echo -e "  ${GREEN}●${NC} 部署模式：${BOLD}单机部署${NC}"
            echo -e "  ${GREEN}●${NC} 配置文件：docker-compose.yml（安全模式）"
            echo -e "  ${GREEN}●${NC} 服务列表："
            echo -e "     - Frontend (Nginx + Vue)"
            echo -e "     - Backend (Spring Boot)"
            echo -e "     - PostgreSQL 数据库"
            echo -e "     - Redis 缓存"
            echo -e "     - MinIO 对象存储"
            echo -e "     - PaddleOCR 服务"
            echo -e "     - OnlyOffice 文档预览"
            ;;
        swarm)
            echo -e "  ${MAGENTA}●${NC} 部署模式：${BOLD}Docker Swarm 分布式${NC}"
            echo -e "  ${MAGENTA}●${NC} 配置文件：examples/docker-compose.swarm.yml"
            echo -e "  ${MAGENTA}●${NC} 集群特性："
            echo -e "     - 多节点高可用"
            echo -e "     - 服务自动负载均衡"
            echo -e "     - 支持滚动更新"
            echo -e "     - 动态扩缩容"
            ;;
    esac
    
    echo ""
    print_divider
    echo ""
    read -p "确认开始部署？(Y/n) " confirm
    if [[ "$confirm" =~ ^[Nn]$ ]]; then
        show_deploy_menu
    fi
}

# =====================================================
# 密钥生成函数
# =====================================================
generate_secret() {
    openssl rand -base64 32 2>/dev/null || head -c 32 /dev/urandom | base64
}

generate_password() {
    openssl rand -base64 16 2>/dev/null || head -c 16 /dev/urandom | base64
}

# =====================================================
# 检查 Docker 环境
# =====================================================
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        echo ""
        echo "安装命令："
        echo "  curl -fsSL https://get.docker.com | sh"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi
    
    log_success "Docker 环境检查通过"
}

# =====================================================
# 检查并创建 .env 文件
# =====================================================
setup_env() {
    cd "$PROJECT_ROOT"
    
    ENV_FILE=".env"
    SECRETS_DIR="docker/secrets"
    # 优先查找项目根目录的 env.example，如果没有则查找 docker/env.example
    if [ -f ".env.example" ]; then
        ENV_EXAMPLE=".env.example"
    elif [ -f "docker/env.example" ]; then
        ENV_EXAMPLE="docker/env.example"
    else
        ENV_EXAMPLE=".env.example"
    fi
    FIRST_TIME=false
    USE_SECRETS=false

    # 检测是否已有 secrets 文件（表示之前使用 Secrets 方式）
    if [ -f "$SECRETS_DIR/db_password" ] && [ -f "$SECRETS_DIR/jwt_secret" ]; then
        USE_SECRETS=true
        log_info "检测到 Docker Secrets 配置，使用安全模式"
    fi

    if [ ! -f "$ENV_FILE" ]; then
        log_warn "未找到 .env 文件，正在自动创建..."
        cp "$ENV_EXAMPLE" "$ENV_FILE"
        FIRST_TIME=true
    fi

    source "$ENV_FILE"

    # 首次部署：让用户选择密钥管理方式
    if [ "$FIRST_TIME" = true ] && [ "$USE_SECRETS" = false ]; then
        echo ""
        echo "======================================"
        echo "🔐 选择密钥管理方式"
        echo "======================================"
        echo ""
        echo "  1) Docker Secrets（推荐，更安全）"
        echo "     - 密钥存储在文件中，docker inspect 无法查看"
        echo "     - 需要运行 init-secrets.sh 初始化"
        echo ""
        echo "  2) 环境变量（传统方式）"
        echo "     - 密钥存储在 .env 文件中"
        echo "     - docker inspect 可以看到密钥"
        echo ""
        read -p "请选择 [1/2，默认 1]: " secret_choice
        secret_choice=${secret_choice:-1}
        
        if [ "$secret_choice" = "1" ]; then
            USE_SECRETS=true
            log_info "使用 Docker Secrets 方式..."
            
            # 运行 init-secrets.sh
            if [ -f "$SECRETS_DIR/init-secrets.sh" ]; then
                chmod +x "$SECRETS_DIR/init-secrets.sh"
                cd "$SECRETS_DIR"
                ./init-secrets.sh
                cd "$PROJECT_ROOT"
                log_success "Docker Secrets 初始化完成"
            else
                log_error "找不到 $SECRETS_DIR/init-secrets.sh"
                exit 1
            fi
        else
            log_info "使用传统环境变量方式..."
        fi
    fi

    # 如果使用 Secrets 方式，跳过 .env 密钥生成
    if [ "$USE_SECRETS" = true ]; then
        log_info "密钥由 Docker Secrets 管理，跳过 .env 密钥生成"
        return
    fi

    if [ "$FIRST_TIME" = true ]; then
        echo ""
        log_info "首次部署，自动生成安全密钥到 .env..."
        
        # 生成所有密钥
        NEW_JWT_SECRET=$(generate_secret)
        NEW_DB_PASSWORD=$(generate_password)
        NEW_MINIO_ACCESS_KEY="lawfirm_$(openssl rand -hex 8 2>/dev/null || head -c 8 /dev/urandom | xxd -p)"
        NEW_MINIO_SECRET_KEY=$(generate_password)
        NEW_REDIS_PASSWORD=$(generate_password)
        NEW_ONLYOFFICE_SECRET=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | xxd -p)
        NEW_OCR_API_KEY=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | xxd -p)
        NEW_DOCS_PASSWORD=$(generate_password)
        NEW_DOCUMENT_TOKEN_SECRET=$(generate_secret)
        # Grafana 使用默认密码 admin（可共享）
        
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "s|JWT_SECRET=.*|JWT_SECRET=$NEW_JWT_SECRET|" "$ENV_FILE"
            sed -i '' "s|DB_PASSWORD=.*|DB_PASSWORD=$NEW_DB_PASSWORD|" "$ENV_FILE"
            # 如果 MINIO_ACCESS_KEY 不存在，添加它；如果存在，更新它
            if ! grep -q "^MINIO_ACCESS_KEY=" "$ENV_FILE"; then
                echo "MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS_KEY" >> "$ENV_FILE"
            else
                sed -i '' "s|MINIO_ACCESS_KEY=.*|MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS_KEY|" "$ENV_FILE"
            fi
            # 如果 MINIO_SECRET_KEY 不存在，添加它；如果存在，更新它
            if ! grep -q "^MINIO_SECRET_KEY=" "$ENV_FILE"; then
                echo "MINIO_SECRET_KEY=$NEW_MINIO_SECRET_KEY" >> "$ENV_FILE"
            else
                sed -i '' "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=$NEW_MINIO_SECRET_KEY|" "$ENV_FILE"
            fi
            sed -i '' "s|REDIS_PASSWORD=.*|REDIS_PASSWORD=$NEW_REDIS_PASSWORD|" "$ENV_FILE"
            sed -i '' "s|ONLYOFFICE_JWT_SECRET=.*|ONLYOFFICE_JWT_SECRET=$NEW_ONLYOFFICE_SECRET|" "$ENV_FILE"
            # 如果 ONLYOFFICE_JWT_ENABLED 不存在，添加它；如果存在，更新它
            if ! grep -q "^ONLYOFFICE_JWT_ENABLED=" "$ENV_FILE"; then
                echo "ONLYOFFICE_JWT_ENABLED=true" >> "$ENV_FILE"
            else
                sed -i '' "s|ONLYOFFICE_JWT_ENABLED=.*|ONLYOFFICE_JWT_ENABLED=true|" "$ENV_FILE"
            fi
            # 确保 ONLYOFFICE_CALLBACK_URL 存在（Docker 内部服务名）
            if ! grep -q "^ONLYOFFICE_CALLBACK_URL=" "$ENV_FILE"; then
                echo "ONLYOFFICE_CALLBACK_URL=http://backend:8080/api" >> "$ENV_FILE"
            else
                sed -i '' "s|ONLYOFFICE_CALLBACK_URL=.*|ONLYOFFICE_CALLBACK_URL=http://backend:8080/api|" "$ENV_FILE"
            fi
            sed -i '' "s|OCR_API_KEY=.*|OCR_API_KEY=$NEW_OCR_API_KEY|" "$ENV_FILE"
            # 如果 DOCS_USERNAME 不存在，添加它；如果存在，更新它
            if ! grep -q "^DOCS_USERNAME=" "$ENV_FILE"; then
                echo "DOCS_USERNAME=admin" >> "$ENV_FILE"
            fi
            # 如果 DOCS_PASSWORD 不存在，添加它；如果存在，更新它
            if ! grep -q "^DOCS_PASSWORD=" "$ENV_FILE"; then
                echo "DOCS_PASSWORD=$NEW_DOCS_PASSWORD" >> "$ENV_FILE"
            else
                sed -i '' "s|DOCS_PASSWORD=.*|DOCS_PASSWORD=$NEW_DOCS_PASSWORD|" "$ENV_FILE"
            fi
            # 如果 DOCUMENT_TOKEN_SECRET 不存在，添加它；如果存在，更新它
            if ! grep -q "^DOCUMENT_TOKEN_SECRET=" "$ENV_FILE"; then
                echo "DOCUMENT_TOKEN_SECRET=$NEW_DOCUMENT_TOKEN_SECRET" >> "$ENV_FILE"
            else
                sed -i '' "s|DOCUMENT_TOKEN_SECRET=.*|DOCUMENT_TOKEN_SECRET=$NEW_DOCUMENT_TOKEN_SECRET|" "$ENV_FILE"
            fi
            # Grafana 使用默认密码 admin，不需要在 .env 中配置
        else
            sed -i "s|JWT_SECRET=.*|JWT_SECRET=$NEW_JWT_SECRET|" "$ENV_FILE"
            sed -i "s|DB_PASSWORD=.*|DB_PASSWORD=$NEW_DB_PASSWORD|" "$ENV_FILE"
            # 如果 MINIO_ACCESS_KEY 不存在，添加它；如果存在，更新它
            if ! grep -q "^MINIO_ACCESS_KEY=" "$ENV_FILE"; then
                echo "MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS_KEY" >> "$ENV_FILE"
            else
                sed -i "s|MINIO_ACCESS_KEY=.*|MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS_KEY|" "$ENV_FILE"
            fi
            # 如果 MINIO_SECRET_KEY 不存在，添加它；如果存在，更新它
            if ! grep -q "^MINIO_SECRET_KEY=" "$ENV_FILE"; then
                echo "MINIO_SECRET_KEY=$NEW_MINIO_SECRET_KEY" >> "$ENV_FILE"
            else
                sed -i "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=$NEW_MINIO_SECRET_KEY|" "$ENV_FILE"
            fi
            sed -i "s|REDIS_PASSWORD=.*|REDIS_PASSWORD=$NEW_REDIS_PASSWORD|" "$ENV_FILE"
            sed -i "s|ONLYOFFICE_JWT_SECRET=.*|ONLYOFFICE_JWT_SECRET=$NEW_ONLYOFFICE_SECRET|" "$ENV_FILE"
            # 如果 ONLYOFFICE_JWT_ENABLED 不存在，添加它；如果存在，更新它
            if ! grep -q "^ONLYOFFICE_JWT_ENABLED=" "$ENV_FILE"; then
                echo "ONLYOFFICE_JWT_ENABLED=true" >> "$ENV_FILE"
            else
                sed -i "s|ONLYOFFICE_JWT_ENABLED=.*|ONLYOFFICE_JWT_ENABLED=true|" "$ENV_FILE"
            fi
            # 确保 ONLYOFFICE_CALLBACK_URL 存在（Docker 内部服务名）
            if ! grep -q "^ONLYOFFICE_CALLBACK_URL=" "$ENV_FILE"; then
                echo "ONLYOFFICE_CALLBACK_URL=http://backend:8080/api" >> "$ENV_FILE"
            else
                sed -i "s|ONLYOFFICE_CALLBACK_URL=.*|ONLYOFFICE_CALLBACK_URL=http://backend:8080/api|" "$ENV_FILE"
            fi
            sed -i "s|OCR_API_KEY=.*|OCR_API_KEY=$NEW_OCR_API_KEY|" "$ENV_FILE"
            # 如果 DOCS_USERNAME 不存在，添加它；如果存在，更新它
            if ! grep -q "^DOCS_USERNAME=" "$ENV_FILE"; then
                echo "DOCS_USERNAME=admin" >> "$ENV_FILE"
            fi
            # 如果 DOCS_PASSWORD 不存在，添加它；如果存在，更新它
            if ! grep -q "^DOCS_PASSWORD=" "$ENV_FILE"; then
                echo "DOCS_PASSWORD=$NEW_DOCS_PASSWORD" >> "$ENV_FILE"
            else
                sed -i "s|DOCS_PASSWORD=.*|DOCS_PASSWORD=$NEW_DOCS_PASSWORD|" "$ENV_FILE"
            fi
            # 如果 DOCUMENT_TOKEN_SECRET 不存在，添加它；如果存在，更新它
            if ! grep -q "^DOCUMENT_TOKEN_SECRET=" "$ENV_FILE"; then
                echo "DOCUMENT_TOKEN_SECRET=$NEW_DOCUMENT_TOKEN_SECRET" >> "$ENV_FILE"
            else
                sed -i "s|DOCUMENT_TOKEN_SECRET=.*|DOCUMENT_TOKEN_SECRET=$NEW_DOCUMENT_TOKEN_SECRET|" "$ENV_FILE"
            fi
            # Grafana 使用默认密码 admin，不需要在 .env 中配置
        fi
        
        # 自动检测服务器 IP 并配置外部访问地址（无论首次部署还是重新部署都会执行）
        SERVER_IP=$(detect_server_ip)
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # 配置 MINIO_BROWSER_ENDPOINT（缩略图浏览器访问）
            # ⚠️ 单端口架构：使用相对路径 /minio，通过 Nginx 代理访问
            # 这样可以避免 Mixed Content 问题（HTTPS 页面加载 HTTP 资源）
            if ! grep -q "^MINIO_BROWSER_ENDPOINT=" "$ENV_FILE"; then
                echo "MINIO_BROWSER_ENDPOINT=/minio" >> "$ENV_FILE"
            else
                # 如果已存在但使用的是 IP 地址，更新为相对路径
                if grep -q "^MINIO_BROWSER_ENDPOINT=http://" "$ENV_FILE"; then
                    sed -i '' "s|MINIO_BROWSER_ENDPOINT=.*|MINIO_BROWSER_ENDPOINT=/minio|" "$ENV_FILE"
                fi
            fi
            # 配置 ONLYOFFICE_URL（浏览器访问 OnlyOffice）
            # ⚠️ 单端口架构：使用相对路径 /onlyoffice，通过 Nginx 代理访问
            # 这样可以避免 Mixed Content 问题（HTTPS 页面加载 HTTP 资源）
            if ! grep -q "^ONLYOFFICE_URL=" "$ENV_FILE"; then
                echo "ONLYOFFICE_URL=/onlyoffice" >> "$ENV_FILE"
            else
                # 如果已存在但使用的是 IP 地址，更新为相对路径
                if grep -q "^ONLYOFFICE_URL=http://" "$ENV_FILE"; then
                    sed -i '' "s|ONLYOFFICE_URL=.*|ONLYOFFICE_URL=/onlyoffice|" "$ENV_FILE"
                fi
            fi
        else
            # 配置 MINIO_BROWSER_ENDPOINT（缩略图浏览器访问）
            # ⚠️ 单端口架构：使用相对路径 /minio，通过 Nginx 代理访问
            # 这样可以避免 Mixed Content 问题（HTTPS 页面加载 HTTP 资源）
            if ! grep -q "^MINIO_BROWSER_ENDPOINT=" "$ENV_FILE"; then
                echo "MINIO_BROWSER_ENDPOINT=/minio" >> "$ENV_FILE"
            else
                # 如果已存在但使用的是 IP 地址，更新为相对路径
                if grep -q "^MINIO_BROWSER_ENDPOINT=http://" "$ENV_FILE"; then
                    sed -i "s|MINIO_BROWSER_ENDPOINT=.*|MINIO_BROWSER_ENDPOINT=/minio|" "$ENV_FILE"
                fi
            fi
            # 配置 ONLYOFFICE_URL（浏览器访问 OnlyOffice）
            # ⚠️ 单端口架构：使用相对路径 /onlyoffice，通过 Nginx 代理访问
            # 这样可以避免 Mixed Content 问题（HTTPS 页面加载 HTTP 资源）
            if ! grep -q "^ONLYOFFICE_URL=" "$ENV_FILE"; then
                echo "ONLYOFFICE_URL=/onlyoffice" >> "$ENV_FILE"
            else
                # 如果已存在但使用的是 IP 地址，更新为相对路径
                if grep -q "^ONLYOFFICE_URL=http://" "$ENV_FILE"; then
                    sed -i "s|ONLYOFFICE_URL=.*|ONLYOFFICE_URL=/onlyoffice|" "$ENV_FILE"
                fi
            fi
        fi
        
        echo -e "  ${GREEN}✅${NC} JWT_SECRET"
        echo -e "  ${GREEN}✅${NC} DB_PASSWORD"
        echo -e "  ${GREEN}✅${NC} MINIO_ACCESS_KEY"
        echo -e "  ${GREEN}✅${NC} MINIO_SECRET_KEY"
        echo -e "  ${GREEN}✅${NC} MINIO_BROWSER_ENDPOINT=/minio (相对路径，通过 Nginx 代理)"
        echo -e "  ${GREEN}✅${NC} REDIS_PASSWORD"
        echo -e "  ${GREEN}✅${NC} ONLYOFFICE_JWT_SECRET"
        echo -e "  ${GREEN}✅${NC} ONLYOFFICE_JWT_ENABLED=true"
        echo -e "  ${GREEN}✅${NC} ONLYOFFICE_CALLBACK_URL=http://backend:8080/api"
        echo -e "  ${GREEN}✅${NC} ONLYOFFICE_URL=/onlyoffice (相对路径，通过 Nginx 代理)"
        echo -e "  ${GREEN}✅${NC} OCR_API_KEY"
        echo -e "  ${GREEN}✅${NC} DOCS_PASSWORD"
        echo -e "  ${GREEN}✅${NC} DOCUMENT_TOKEN_SECRET"
        echo -e "  ${GREEN}✅${NC} Grafana 使用默认密码: admin"
        
        echo ""
        log_success "安全密钥已保存到 .env"
        log_warn "请妥善保管此文件！"
        log_info "检测到服务器 IP: ${SERVER_IP}"

        source "$ENV_FILE"
    else
        # 检查不安全的默认值
        HAS_UNSAFE=false
        
        # 如果 MinIO 密码未配置或使用默认值，自动生成
        if [ -z "${MINIO_ACCESS_KEY:-}" ] || [ "$MINIO_ACCESS_KEY" = "your-minio-access-key" ] || [ "$MINIO_ACCESS_KEY" = "minioadmin" ]; then
            log_info "检测到 MinIO 访问密钥未配置或使用默认值，自动生成..."
            NEW_MINIO_ACCESS_KEY="lawfirm_$(openssl rand -hex 8 2>/dev/null || head -c 8 /dev/urandom | xxd -p)"
            NEW_MINIO_SECRET_KEY=$(generate_password)
            if [[ "$OSTYPE" == "darwin"* ]]; then
                if ! grep -q "^MINIO_ACCESS_KEY=" "$ENV_FILE"; then
                    echo "MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS_KEY" >> "$ENV_FILE"
                else
                    sed -i '' "s|MINIO_ACCESS_KEY=.*|MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS_KEY|" "$ENV_FILE"
                fi
                if ! grep -q "^MINIO_SECRET_KEY=" "$ENV_FILE"; then
                    echo "MINIO_SECRET_KEY=$NEW_MINIO_SECRET_KEY" >> "$ENV_FILE"
                else
                    sed -i '' "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=$NEW_MINIO_SECRET_KEY|" "$ENV_FILE"
                fi
            else
                if ! grep -q "^MINIO_ACCESS_KEY=" "$ENV_FILE"; then
                    echo "MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS_KEY" >> "$ENV_FILE"
                else
                    sed -i "s|MINIO_ACCESS_KEY=.*|MINIO_ACCESS_KEY=$NEW_MINIO_ACCESS_KEY|" "$ENV_FILE"
                fi
                if ! grep -q "^MINIO_SECRET_KEY=" "$ENV_FILE"; then
                    echo "MINIO_SECRET_KEY=$NEW_MINIO_SECRET_KEY" >> "$ENV_FILE"
                else
                    sed -i "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=$NEW_MINIO_SECRET_KEY|" "$ENV_FILE"
                fi
            fi
            source "$ENV_FILE"
            log_success "MinIO 密码已生成"
        fi
        
        if [ "$DB_PASSWORD" = "your_secure_db_password_here" ] || [ -z "$DB_PASSWORD" ]; then
            log_warn "数据库密码未正确配置"
            HAS_UNSAFE=true
        fi

        # 确保 OnlyOffice JWT 配置完整和一致
        # 如果 ONLYOFFICE_JWT_SECRET 不存在或是默认值，自动生成
        if [ -z "${ONLYOFFICE_JWT_SECRET:-}" ] || [ "$ONLYOFFICE_JWT_SECRET" = "your-onlyoffice-jwt-secret-change-in-production" ] || [ "$ONLYOFFICE_JWT_SECRET" = "law-firm-onlyoffice-default-secret-2024" ]; then
            log_info "检测到 ONLYOFFICE_JWT_SECRET 未配置，自动生成..."
            NEW_ONLYOFFICE_SECRET=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | xxd -p)
            if [[ "$OSTYPE" == "darwin"* ]]; then
                if ! grep -q "^ONLYOFFICE_JWT_SECRET=" "$ENV_FILE"; then
                    echo "ONLYOFFICE_JWT_SECRET=$NEW_ONLYOFFICE_SECRET" >> "$ENV_FILE"
                else
                    sed -i '' "s|ONLYOFFICE_JWT_SECRET=.*|ONLYOFFICE_JWT_SECRET=$NEW_ONLYOFFICE_SECRET|" "$ENV_FILE"
                fi
            else
                if ! grep -q "^ONLYOFFICE_JWT_SECRET=" "$ENV_FILE"; then
                    echo "ONLYOFFICE_JWT_SECRET=$NEW_ONLYOFFICE_SECRET" >> "$ENV_FILE"
                else
                    sed -i "s|ONLYOFFICE_JWT_SECRET=.*|ONLYOFFICE_JWT_SECRET=$NEW_ONLYOFFICE_SECRET|" "$ENV_FILE"
                fi
            fi
            source "$ENV_FILE"
            log_success "OnlyOffice JWT 密钥已生成"
        fi
        
        # 如果 ONLYOFFICE_JWT_SECRET 存在但 ONLYOFFICE_JWT_ENABLED 未设置或为 false，自动启用
        if [ -n "${ONLYOFFICE_JWT_SECRET:-}" ] && [ "$ONLYOFFICE_JWT_SECRET" != "your-onlyoffice-jwt-secret-change-in-production" ]; then
            if [ -z "${ONLYOFFICE_JWT_ENABLED:-}" ] || [ "$ONLYOFFICE_JWT_ENABLED" != "true" ]; then
                log_info "检测到 ONLYOFFICE_JWT_SECRET 已配置，自动启用 JWT..."
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    if ! grep -q "^ONLYOFFICE_JWT_ENABLED=" "$ENV_FILE"; then
                        echo "ONLYOFFICE_JWT_ENABLED=true" >> "$ENV_FILE"
                    else
                        sed -i '' "s|ONLYOFFICE_JWT_ENABLED=.*|ONLYOFFICE_JWT_ENABLED=true|" "$ENV_FILE"
                    fi
                else
                    if ! grep -q "^ONLYOFFICE_JWT_ENABLED=" "$ENV_FILE"; then
                        echo "ONLYOFFICE_JWT_ENABLED=true" >> "$ENV_FILE"
                    else
                        sed -i "s|ONLYOFFICE_JWT_ENABLED=.*|ONLYOFFICE_JWT_ENABLED=true|" "$ENV_FILE"
                    fi
                fi
                source "$ENV_FILE"
                log_success "OnlyOffice JWT 已启用"
            fi
        fi

        # 确保 ONLYOFFICE_CALLBACK_URL 存在且正确（Docker 内部服务名）
        if [ -z "${ONLYOFFICE_CALLBACK_URL:-}" ] || [ "$ONLYOFFICE_CALLBACK_URL" != "http://backend:8080/api" ]; then
            log_info "配置 OnlyOffice 回调 URL（Docker 内部地址）..."
            if [[ "$OSTYPE" == "darwin"* ]]; then
                if ! grep -q "^ONLYOFFICE_CALLBACK_URL=" "$ENV_FILE"; then
                    echo "ONLYOFFICE_CALLBACK_URL=http://backend:8080/api" >> "$ENV_FILE"
                else
                    sed -i '' "s|ONLYOFFICE_CALLBACK_URL=.*|ONLYOFFICE_CALLBACK_URL=http://backend:8080/api|" "$ENV_FILE"
                fi
            else
                if ! grep -q "^ONLYOFFICE_CALLBACK_URL=" "$ENV_FILE"; then
                    echo "ONLYOFFICE_CALLBACK_URL=http://backend:8080/api" >> "$ENV_FILE"
                else
                    sed -i "s|ONLYOFFICE_CALLBACK_URL=.*|ONLYOFFICE_CALLBACK_URL=http://backend:8080/api|" "$ENV_FILE"
                fi
            fi
            source "$ENV_FILE"
            log_success "OnlyOffice 回调 URL 已配置"
        fi

        # 如果 DOCS_USERNAME 不存在，自动添加
        if [ -z "${DOCS_USERNAME:-}" ]; then
            log_info "检测到 DOCS_USERNAME 未配置，自动添加..."
            if [[ "$OSTYPE" == "darwin"* ]]; then
                if ! grep -q "^DOCS_USERNAME=" "$ENV_FILE"; then
                    echo "DOCS_USERNAME=admin" >> "$ENV_FILE"
                fi
            else
                if ! grep -q "^DOCS_USERNAME=" "$ENV_FILE"; then
                    echo "DOCS_USERNAME=admin" >> "$ENV_FILE"
                fi
            fi
            source "$ENV_FILE"
            log_success "文档站点用户名已配置"
        fi

        # 如果 DOCS_PASSWORD 不存在，自动生成
        if [ -z "${DOCS_PASSWORD:-}" ] || [ "$DOCS_PASSWORD" = "your_docs_password_here" ] || [ "$DOCS_PASSWORD" = "your-docs-password-here" ]; then
            log_info "检测到 DOCS_PASSWORD 未配置，自动生成..."
            NEW_DOCS_PASSWORD=$(generate_password)
            if [[ "$OSTYPE" == "darwin"* ]]; then
                if ! grep -q "^DOCS_PASSWORD=" "$ENV_FILE"; then
                    echo "DOCS_PASSWORD=$NEW_DOCS_PASSWORD" >> "$ENV_FILE"
                else
                    sed -i '' "s|DOCS_PASSWORD=.*|DOCS_PASSWORD=$NEW_DOCS_PASSWORD|" "$ENV_FILE"
                fi
            else
                if ! grep -q "^DOCS_PASSWORD=" "$ENV_FILE"; then
                    echo "DOCS_PASSWORD=$NEW_DOCS_PASSWORD" >> "$ENV_FILE"
                else
                    sed -i "s|DOCS_PASSWORD=.*|DOCS_PASSWORD=$NEW_DOCS_PASSWORD|" "$ENV_FILE"
                fi
            fi
            source "$ENV_FILE"
            log_success "文档站点密码已生成"
        fi

        # Grafana 使用默认密码 admin（可共享，便于统一管理）
        # 不需要在 .env 中配置，docker-compose 中已设置为 admin
        
        if [ "$HAS_UNSAFE" = true ]; then
            echo ""
            log_warn "请手动编辑 .env 文件修改不安全的配置"
            read -p "是否继续部署？(y/N) " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                exit 1
            fi
        else
            log_success "使用现有的 .env 配置"
        fi
    fi
}

# =====================================================
# Swarm 部署配置
# =====================================================
configure_swarm() {
    print_banner
    echo -e "${BOLD}Docker Swarm 分布式部署：${NC}"
    echo ""
    echo -e "${DIM}此模式需要多台服务器组成集群${NC}"
    echo ""
    print_divider
    echo ""
    echo -e "  ${GREEN}1)${NC} 初始化新集群（当前节点为 Manager）"
    echo -e "  ${YELLOW}2)${NC} 加入现有集群（当前节点为 Worker）"
    echo -e "  ${CYAN}3)${NC} 部署到现有 Swarm 集群"
    echo ""
    echo -e "  ${DIM}0) 返回上一级${NC}"
    echo ""
    
    read -p "请选择操作 [1-3]: " swarm_choice
    
    case $swarm_choice in
        1)
            log_info "初始化 Swarm 集群..."
            exec "$SCRIPT_DIR/deploy-swarm.sh" init
            ;;
        2)
            echo ""
            read -p "请输入 Manager 节点提供的 join token: " token
            read -p "请输入 Manager 节点 IP: " manager_ip
            exec "$SCRIPT_DIR/deploy-swarm.sh" join "$token" "$manager_ip"
            ;;
        3)
            # 检查是否在 Swarm 中
            if ! docker info 2>/dev/null | grep -q "Swarm: active"; then
                log_error "当前节点不在 Swarm 集群中"
                log_info "请先选择选项 1 初始化集群或选项 2 加入集群"
                sleep 2
                configure_swarm
                return
            fi
            exec "$SCRIPT_DIR/deploy-swarm.sh" deploy
            ;;
        0)
            show_deploy_menu
            ;;
        *)
            log_error "无效选项"
            sleep 1
            configure_swarm
            ;;
    esac
}

# =====================================================
# 执行单机部署
# =====================================================
deploy_standalone() {
    echo ""
    log_info "开始单机部署..."
    echo ""
    
    cd "$DOCKER_DIR"
    
    # 运行生产环境检查
    if [ "${SKIP_CHECK:-}" != "true" ]; then
        log_info "运行生产环境检查..."
        
        # 优先使用统一的检查脚本
        if [ -f "$SCRIPT_DIR/pre-deploy-check.sh" ]; then
            if ! bash "$SCRIPT_DIR/pre-deploy-check.sh"; then
                echo ""
                log_warn "部分检查项未通过，但可以继续部署"
                read -p "是否继续？(y/N) " -n 1 -r
                echo
                if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                    exit 1
                fi
            fi
        fi
    fi
    
    # 选择配置文件：优先使用 Secrets 方式，回退到环境变量方式
    COMPOSE_FILE="docker-compose.yml"
    COMPOSE_OPTS=""
    
    if [ -f "secrets/db_password" ] && [ -f "secrets/jwt_secret" ]; then
        log_info "检测到 secrets 文件，使用安全模式（docker inspect 无法查看密钥）"
        COMPOSE_FILE="docker-compose.yml"
    elif [ -f "$PROJECT_ROOT/.env" ]; then
        log_warn "未找到 secrets 文件，回退到环境变量模式"
        log_warn "提示：运行 cd docker/secrets && ./init-secrets.sh 初始化密钥文件"
        COMPOSE_FILE="examples/docker-compose.env-vars.yml"
        COMPOSE_OPTS="--env-file $PROJECT_ROOT/.env"
    else
        log_error "请先初始化配置：cd docker/secrets && ./init-secrets.sh"
        exit 1
    fi
    
    # 先构建镜像（避免 BuildKit 问题）
    # 如果指定了 --no-cache 或者不是首次部署，则强制重新构建
    if [ "$NO_CACHE" = true ]; then
        log_info "构建 Docker 镜像（强制重新构建，不使用缓存）..."
        DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker compose $COMPOSE_OPTS -f $COMPOSE_FILE build --no-cache
    else
        log_info "构建 Docker 镜像..."
        DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker compose $COMPOSE_OPTS -f $COMPOSE_FILE build
    fi
    
    # 启动服务（每个项目运行自己的容器）
    # 默认启动核心服务 + 文档服务 + 监控服务
    log_info "启动服务..."
    docker compose $COMPOSE_OPTS -f $COMPOSE_FILE --profile docs --profile monitoring up -d
    
    echo ""
    log_info "等待服务启动..."
    sleep 10
    
    # 检查服务状态
    echo ""
    log_info "服务状态："
    docker compose $COMPOSE_OPTS -f $COMPOSE_FILE ps
    
    show_success_banner
    
    # 初始化示例数据
    init_demo_data_if_needed
}

# =====================================================
# 初始化示例数据
# =====================================================
init_demo_data_if_needed() {
    if [ "$INIT_DEMO_DATA" = true ]; then
        echo ""
        log_info "开始初始化示例数据..."
        
        # 等待数据库完全就绪
        log_info "等待数据库就绪..."
        sleep 15
        
        if [ -f "$SCRIPTS_DIR/ops/init-demo-data.sh" ]; then
            bash "$SCRIPTS_DIR/ops/init-demo-data.sh" --docker --$DEMO_DATA_LEVEL
        else
            log_warn "示例数据脚本不存在: $SCRIPTS_DIR/ops/init-demo-data.sh"
        fi
    elif [ "$INTERACTIVE" = true ]; then
        echo ""
        read -p "是否初始化示例数据？(用于演示/测试) (y/N) " init_demo
        if [[ "$init_demo" =~ ^[Yy]$ ]]; then
            echo ""
            echo -e "  ${GREEN}1)${NC} 完整示例数据 ${DIM}(推荐，包含所有模块数据)${NC}"
            echo -e "  ${YELLOW}2)${NC} 最小示例数据 ${DIM}(仅基础客户、项目)${NC}"
            echo ""
            read -p "请选择 [1-2]: " demo_choice
            
            DEMO_DATA_LEVEL="full"
            if [ "$demo_choice" = "2" ]; then
                DEMO_DATA_LEVEL="minimal"
            fi
            
            log_info "等待数据库就绪..."
            sleep 15
            
            if [ -f "$SCRIPTS_DIR/ops/init-demo-data.sh" ]; then
                bash "$SCRIPTS_DIR/ops/init-demo-data.sh" --docker --$DEMO_DATA_LEVEL
            fi
        fi
    fi
}

# =====================================================
# 显示成功信息
# =====================================================
show_success_banner() {
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║${NC}                                                              ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}    ${BOLD}✅ ${FIRM_NAME} - 部署完成！${NC}                  ${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}                                                              ${GREEN}║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BOLD}访问地址：${NC}"
    echo -e "  🌐 主应用：      http://localhost"
    echo -e "  📚 文档站点：    http://localhost/docs/"
    echo -e "  🔧 API 地址：    http://localhost/api"
    echo -e "  📦 MinIO 控制台：http://localhost:9001 (默认密码: minioadmin/minioadmin)"
    echo -e "  📊 Grafana 监控：http://localhost:3000 (默认密码: admin)"
    echo ""
    echo -e "${BOLD}主应用账号（密码统一为 admin123）：${NC}"
    echo -e "  admin / director / lawyer1 / leader / finance / staff / trainee"
    echo ""
    echo -e "${BOLD}文档站点账号：${NC}"
    echo -e "  用户名：${DOCS_USERNAME:-admin}"
    # 从 .env 文件读取 DOCS_PASSWORD 并显示
    local docs_pwd=$(grep "^DOCS_PASSWORD=" .env 2>/dev/null | cut -d'=' -f2)
    echo -e "  密码：${docs_pwd:-查看 .env 文件中的 DOCS_PASSWORD}"
    echo ""
    print_divider
    echo ""
    echo -e "${BOLD}常用命令：${NC}"
    echo -e "  📋 查看日志：${DIM}cd docker && docker compose -f docker-compose.yml logs -f${NC}"
    echo -e "  🛑 停止服务：${DIM}cd docker && docker compose -f docker-compose.yml down${NC}"
    echo -e "  🔄 重启服务：${DIM}cd docker && docker compose -f docker-compose.yml restart${NC}"
    echo ""
}

# =====================================================
# 显示帮助信息
# =====================================================
show_help() {
    echo ""
    echo -e "${BOLD}律师事务所管理系统 - 部署脚本${NC}"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项："
    echo "  --quick, -q          快速部署（单机模式，跳过引导）"
    echo "  --mode=MODE          指定部署模式"
    echo "                       可选值: standalone, nas, swarm, minio-cluster"
    echo "  --nas-ip=IP          NAS 部署时指定 NAS IP 地址"
    echo "  --skip-check         跳过生产环境检查"
    echo "  --no-cache           强制重新构建镜像（不使用缓存，用于代码更新后）"
    echo "  --with-demo          部署后初始化示例数据（完整）"
    echo "  --with-demo=minimal  部署后初始化最小示例数据"
    echo "  --help, -h           显示帮助信息"
    echo ""
    echo "示例："
    echo "  $0                   # 引导式部署（推荐）"
    echo "  $0 --quick           # 快速单机部署"
    echo "  $0 --no-cache        # 强制重新构建（代码更新后使用）"
    echo "  $0 --quick --with-demo  # 快速部署并初始化示例数据"
    echo "  $0 --mode=swarm      # Swarm 分布式部署"
    echo "  $0 --mode=nas --nas-ip=192.168.1.100"
    echo ""
}

# =====================================================
# 解析命令行参数
# =====================================================
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --quick|-q)
                INTERACTIVE=false
                DEPLOY_MODE="standalone"
                shift
                ;;
            --mode=*)
                DEPLOY_MODE="${1#*=}"
                INTERACTIVE=false
                shift
                ;;
            --nas-ip=*)
                NAS_IP="${1#*=}"
                shift
                ;;
            --skip-check)
                export SKIP_CHECK=true
                shift
                ;;
            --with-demo)
                INIT_DEMO_DATA=true
                DEMO_DATA_LEVEL="full"
                shift
                ;;
            --with-demo=*)
                INIT_DEMO_DATA=true
                DEMO_DATA_LEVEL="${1#*=}"
                shift
                ;;
            --no-cache)
                NO_CACHE=true
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# =====================================================
# 主入口
# =====================================================
main() {
    parse_args "$@"
    
    # 检查 Docker 环境
    check_docker
    
    # 切换到项目根目录
    cd "$PROJECT_ROOT"
    
    # 交互式选择部署模式
    if [ "$INTERACTIVE" = true ]; then
        show_deploy_menu
        show_deploy_summary
    else
        # 非交互模式，显示简单的横幅
        echo ""
        echo "=============================================="
        echo "    ${FIRM_NAME} - Docker 部署"
        echo "=============================================="
        echo ""
    fi
    
    # 设置环境变量
    setup_env
    
    # 根据模式执行部署
    case $DEPLOY_MODE in
        standalone)
            deploy_standalone
            ;;
        swarm)
            configure_swarm
            ;;
        *)
            log_error "未知的部署模式: $DEPLOY_MODE"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
