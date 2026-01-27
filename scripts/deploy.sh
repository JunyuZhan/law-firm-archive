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
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
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
    echo -e "  ${YELLOW}2)${NC} ${BOLD}NAS 存储分离部署${NC} ${DIM}(推荐中型律所)${NC}"
    echo -e "     ${DIM}└─ 应用服务器 + NAS 存储，利用 RAID 保护数据${NC}"
    echo ""
    echo -e "  ${MAGENTA}3)${NC} ${BOLD}Docker Swarm 分布式部署${NC} ${DIM}(推荐大型律所)${NC}"
    echo -e "     ${DIM}└─ 多节点集群，支持高可用和水平扩展${NC}"
    echo ""
    echo -e "  ${CYAN}4)${NC} ${BOLD}MinIO 分布式存储集群${NC} ${DIM}(企业级数据保护)${NC}"
    echo -e "     ${DIM}└─ 4 节点 MinIO 集群，纠删码冗余${NC}"
    echo ""
    print_divider
    echo -e "  ${DIM}0) 退出${NC}"
    echo ""
    
    read -p "请输入选项 [1-4]: " choice
    
    case $choice in
        1) DEPLOY_MODE="standalone" ;;
        2) DEPLOY_MODE="nas" ;;
        3) DEPLOY_MODE="swarm" ;;
        4) DEPLOY_MODE="minio-cluster" ;;
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
            echo -e "  ${GREEN}●${NC} 配置文件：docker-compose.prod.yml"
            echo -e "  ${GREEN}●${NC} 服务列表："
            echo -e "     - Frontend (Nginx + Vue)"
            echo -e "     - Backend (Spring Boot)"
            echo -e "     - PostgreSQL 数据库"
            echo -e "     - Redis 缓存"
            echo -e "     - MinIO 对象存储"
            echo -e "     - PaddleOCR 服务"
            echo -e "     - OnlyOffice 文档预览"
            ;;
        nas)
            echo -e "  ${YELLOW}●${NC} 部署模式：${BOLD}NAS 存储分离部署${NC}"
            echo -e "  ${YELLOW}●${NC} 配置文件：docker-compose.nas.yml"
            echo -e "  ${YELLOW}●${NC} NAS 地址：${NAS_IP:-未配置}"
            echo -e "  ${YELLOW}●${NC} 架构说明："
            echo -e "     - 应用服务器：运行所有应用服务"
            echo -e "     - NAS 服务器：运行 MinIO 存储文件"
            ;;
        swarm)
            echo -e "  ${MAGENTA}●${NC} 部署模式：${BOLD}Docker Swarm 分布式${NC}"
            echo -e "  ${MAGENTA}●${NC} 配置文件：docker-compose.swarm.yml"
            echo -e "  ${MAGENTA}●${NC} 集群特性："
            echo -e "     - 多节点高可用"
            echo -e "     - 服务自动负载均衡"
            echo -e "     - 支持滚动更新"
            echo -e "     - 动态扩缩容"
            ;;
        minio-cluster)
            echo -e "  ${CYAN}●${NC} 部署模式：${BOLD}MinIO 分布式存储集群${NC}"
            echo -e "  ${CYAN}●${NC} 配置文件：docker-compose.minio-cluster.yml"
            echo -e "  ${CYAN}●${NC} 集群配置："
            echo -e "     - 4 个 MinIO 节点"
            echo -e "     - Nginx 负载均衡"
            echo -e "     - 纠删码数据保护"
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
    # 优先查找项目根目录的 env.example，如果没有则查找 docker/env.example
    if [ -f ".env.example" ]; then
        ENV_EXAMPLE=".env.example"
    elif [ -f "docker/env.example" ]; then
        ENV_EXAMPLE="docker/env.example"
    else
        ENV_EXAMPLE=".env.example"
    fi
    FIRST_TIME=false

    if [ ! -f "$ENV_FILE" ]; then
        log_warn "未找到 .env 文件，正在自动创建..."
        cp "$ENV_EXAMPLE" "$ENV_FILE"
        FIRST_TIME=true
    fi

    source "$ENV_FILE"

    if [ "$FIRST_TIME" = true ]; then
        echo ""
        log_info "首次部署，自动生成安全密钥..."
        
        # 生成所有密钥
        NEW_JWT_SECRET=$(generate_secret)
        NEW_DB_PASSWORD=$(generate_password)
        NEW_MINIO_ACCESS_KEY="lawfirm_$(openssl rand -hex 8 2>/dev/null || head -c 8 /dev/urandom | xxd -p)"
        NEW_MINIO_SECRET_KEY=$(generate_password)
        NEW_REDIS_PASSWORD=$(generate_password)
        NEW_ONLYOFFICE_SECRET=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | xxd -p)
        NEW_OCR_API_KEY=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | xxd -p)
        NEW_DOCS_PASSWORD=$(generate_password)
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
            # Grafana 使用默认密码 admin，不需要在 .env 中配置
        fi
        
        # 自动检测服务器 IP 并配置外部访问地址
        SERVER_IP=$(detect_server_ip)
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # 配置 MINIO_BROWSER_ENDPOINT（缩略图浏览器访问）
            if ! grep -q "^MINIO_BROWSER_ENDPOINT=" "$ENV_FILE"; then
                echo "MINIO_BROWSER_ENDPOINT=http://${SERVER_IP}:9000" >> "$ENV_FILE"
            else
                sed -i '' "s|MINIO_BROWSER_ENDPOINT=.*|MINIO_BROWSER_ENDPOINT=http://${SERVER_IP}:9000|" "$ENV_FILE"
            fi
            # 配置 ONLYOFFICE_URL（浏览器访问 OnlyOffice）
            if ! grep -q "^ONLYOFFICE_URL=" "$ENV_FILE"; then
                echo "ONLYOFFICE_URL=http://${SERVER_IP}/onlyoffice" >> "$ENV_FILE"
            else
                sed -i '' "s|ONLYOFFICE_URL=.*|ONLYOFFICE_URL=http://${SERVER_IP}/onlyoffice|" "$ENV_FILE"
            fi
        else
            # 配置 MINIO_BROWSER_ENDPOINT（缩略图浏览器访问）
            if ! grep -q "^MINIO_BROWSER_ENDPOINT=" "$ENV_FILE"; then
                echo "MINIO_BROWSER_ENDPOINT=http://${SERVER_IP}:9000" >> "$ENV_FILE"
            else
                sed -i "s|MINIO_BROWSER_ENDPOINT=.*|MINIO_BROWSER_ENDPOINT=http://${SERVER_IP}:9000|" "$ENV_FILE"
            fi
            # 配置 ONLYOFFICE_URL（浏览器访问 OnlyOffice）
            if ! grep -q "^ONLYOFFICE_URL=" "$ENV_FILE"; then
                echo "ONLYOFFICE_URL=http://${SERVER_IP}/onlyoffice" >> "$ENV_FILE"
            else
                sed -i "s|ONLYOFFICE_URL=.*|ONLYOFFICE_URL=http://${SERVER_IP}/onlyoffice|" "$ENV_FILE"
            fi
        fi
        
        echo -e "  ${GREEN}✅${NC} JWT_SECRET"
        echo -e "  ${GREEN}✅${NC} DB_PASSWORD"
        echo -e "  ${GREEN}✅${NC} MINIO_ACCESS_KEY"
        echo -e "  ${GREEN}✅${NC} MINIO_SECRET_KEY"
        echo -e "  ${GREEN}✅${NC} MINIO_BROWSER_ENDPOINT=http://${SERVER_IP}:9000"
        echo -e "  ${GREEN}✅${NC} REDIS_PASSWORD"
        echo -e "  ${GREEN}✅${NC} ONLYOFFICE_JWT_SECRET"
        echo -e "  ${GREEN}✅${NC} ONLYOFFICE_JWT_ENABLED=true"
        echo -e "  ${GREEN}✅${NC} ONLYOFFICE_CALLBACK_URL=http://backend:8080/api"
        echo -e "  ${GREEN}✅${NC} ONLYOFFICE_URL=http://${SERVER_IP}/onlyoffice"
        echo -e "  ${GREEN}✅${NC} OCR_API_KEY"
        echo -e "  ${GREEN}✅${NC} DOCS_PASSWORD"
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
# NAS 部署配置
# =====================================================
configure_nas() {
    print_banner
    echo -e "${BOLD}NAS 存储配置：${NC}"
    echo ""
    echo -e "${DIM}请确保 NAS 已安装 Docker 并运行 MinIO${NC}"
    echo -e "${DIM}参考文档：docker/DEPLOY-NAS.md${NC}"
    echo ""
    print_divider
    echo ""
    
    read -p "请输入 NAS IP 地址: " NAS_IP
    
    if [ -z "$NAS_IP" ]; then
        log_error "NAS IP 不能为空"
        configure_nas
        return
    fi
    
    # 验证 IP 格式
    if ! [[ "$NAS_IP" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        log_error "无效的 IP 地址格式"
        configure_nas
        return
    fi
    
    echo ""
    log_info "测试 NAS 连接..."
    
    if ping -c 1 "$NAS_IP" &> /dev/null; then
        log_success "NAS 网络连接正常"
    else
        log_warn "无法 ping 通 NAS，请确认网络配置"
        read -p "是否继续？(y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            configure_nas
            return
        fi
    fi
    
    # 更新 .env 文件中的 NAS 配置
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s|NAS_IP=.*|NAS_IP=$NAS_IP|" "$PROJECT_ROOT/.env" 2>/dev/null || \
        echo "NAS_IP=$NAS_IP" >> "$PROJECT_ROOT/.env"
    else
        sed -i "s|NAS_IP=.*|NAS_IP=$NAS_IP|" "$PROJECT_ROOT/.env" 2>/dev/null || \
        echo "NAS_IP=$NAS_IP" >> "$PROJECT_ROOT/.env"
    fi
    
    log_success "NAS 配置已保存"
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
# MinIO 集群部署配置
# =====================================================
configure_minio_cluster() {
    print_banner
    echo -e "${BOLD}MinIO 分布式存储集群配置：${NC}"
    echo ""
    echo -e "${DIM}此模式需要至少 4 个存储驱动器（可跨 2-4 台服务器）${NC}"
    echo ""
    print_divider
    echo ""
    echo -e "${YELLOW}⚠️  注意事项：${NC}"
    echo -e "  1. 需要在所有节点上运行相同的配置"
    echo -e "  2. 所有节点必须网络互通"
    echo -e "  3. 建议配合 Docker Swarm 使用"
    echo ""
    
    read -p "是否继续部署 MinIO 集群？(y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        show_deploy_menu
        return
    fi
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
        elif [ -f "$SCRIPT_DIR/check-production-ready.sh" ]; then
            if ! bash "$SCRIPT_DIR/check-production-ready.sh"; then
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
    
    # 先构建镜像（避免 BuildKit 问题）
    # 如果指定了 --no-cache 或者不是首次部署，则强制重新构建
    if [ "$NO_CACHE" = true ]; then
        log_info "构建 Docker 镜像（强制重新构建，不使用缓存）..."
        DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.prod.yml build --no-cache
    else
        log_info "构建 Docker 镜像..."
        DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.prod.yml build
    fi
    
    # 启动服务（每个项目运行自己的容器）
    # 默认启动核心服务 + 文档服务 + 监控服务
    log_info "启动服务..."
    docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.prod.yml --profile docs --profile monitoring up -d
    
    echo ""
    log_info "等待服务启动..."
    sleep 10
    
    # 检查服务状态
    echo ""
    log_info "服务状态："
    docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.prod.yml ps
    
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
        
        if [ -f "$SCRIPT_DIR/init-demo-data.sh" ]; then
            bash "$SCRIPT_DIR/init-demo-data.sh" --docker --$DEMO_DATA_LEVEL
        else
            log_warn "示例数据脚本不存在: $SCRIPT_DIR/init-demo-data.sh"
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
            
            if [ -f "$SCRIPT_DIR/init-demo-data.sh" ]; then
                bash "$SCRIPT_DIR/init-demo-data.sh" --docker --$DEMO_DATA_LEVEL
            fi
        fi
    fi
}

# =====================================================
# 执行 NAS 部署
# =====================================================
deploy_nas() {
    configure_nas
    
    echo ""
    log_info "开始 NAS 存储分离部署..."
    echo ""
    
    cd "$DOCKER_DIR"
    
    # 先构建镜像（不包含 MinIO，因为 MinIO 在 NAS 上）
    log_info "构建 Docker 镜像..."
    DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.nas.yml build
    
    # 再启动服务
    log_info "启动应用服务..."
    docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.nas.yml up -d
    
    echo ""
    log_info "等待服务启动..."
    sleep 10
    
    docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.nas.yml ps
    
    echo ""
    print_divider
    echo ""
    echo -e "${YELLOW}📌 NAS 端配置提示：${NC}"
    echo ""
    echo -e "请在 NAS (${NAS_IP}) 上运行以下命令部署 MinIO："
    echo ""
    echo -e "  ${DIM}cd /path/to/law-firm${NC}"
    echo -e "  ${DIM}docker-compose -f docker/docker-compose.minio-nas.yml up -d${NC}"
    echo ""
    
    show_success_banner
}

# =====================================================
# 执行 MinIO 集群部署
# =====================================================
deploy_minio_cluster() {
    configure_minio_cluster
    
    echo ""
    log_info "开始 MinIO 集群部署..."
    echo ""
    
    cd "$DOCKER_DIR"
    
    # 先构建镜像
    DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.minio-cluster.yml build
    # 再启动服务
    docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.minio-cluster.yml up -d
    
    echo ""
    log_info "等待 MinIO 集群启动..."
    sleep 15
    
    docker compose --env-file "$PROJECT_ROOT/.env" -f docker-compose.minio-cluster.yml ps
    
    echo ""
    log_success "MinIO 集群部署完成！"
    echo ""
    echo -e "📦 MinIO 控制台：http://localhost:9001 (默认密码: minioadmin/minioadmin)"
    echo ""
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
    echo -e "  📋 查看日志：${DIM}cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml logs -f${NC}"
    echo -e "  🛑 停止服务：${DIM}cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml down${NC}"
    echo -e "  🔄 重启服务：${DIM}cd docker && docker compose --env-file ../.env -f docker-compose.prod.yml restart${NC}"
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
        nas)
            deploy_nas
            ;;
        swarm)
            configure_swarm
            ;;
        minio-cluster)
            deploy_minio_cluster
            ;;
        *)
            log_error "未知的部署模式: $DEPLOY_MODE"
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
