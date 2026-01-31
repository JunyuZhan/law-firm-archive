#!/bin/bash
# =====================================================
# 快速部署到服务器脚本
# =====================================================
# 用法:
#   ./deploy-to-server.sh <服务器IP> [用户名]
#   例如: ./deploy-to-server.sh 192.168.1.100 root
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查参数
if [ -z "$1" ]; then
    log_error "请提供服务器IP地址"
    echo "用法: $0 <服务器IP> [用户名]"
    echo "例如: $0 192.168.1.100 root"
    exit 1
fi

SERVER_IP="$1"
SERVER_USER="${2:-root}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

log_info "准备部署到服务器: ${SERVER_USER}@${SERVER_IP}"

# 检查 SSH 连接
log_info "检查 SSH 连接..."
if ! ssh -o ConnectTimeout=5 "${SERVER_USER}@${SERVER_IP}" "echo 'SSH连接成功'" &>/dev/null; then
    log_error "无法连接到服务器，请检查："
    echo "  1. 服务器IP是否正确: ${SERVER_IP}"
    echo "  2. SSH服务是否运行"
    echo "  3. 防火墙是否开放22端口"
    echo "  4. SSH密钥是否配置"
    exit 1
fi
log_success "SSH 连接正常"

# 检查本地代码是否有未提交的更改
if [ -d "$PROJECT_ROOT/.git" ]; then
    if ! git -C "$PROJECT_ROOT" diff --quiet || ! git -C "$PROJECT_ROOT" diff --cached --quiet; then
        log_warn "检测到未提交的更改"
        read -p "是否继续部署？(y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "部署已取消"
            exit 0
        fi
    fi
fi

# 上传代码
log_info "上传代码到服务器..."
rsync -avz --progress \
    --exclude 'node_modules' \
    --exclude '.git' \
    --exclude 'backend/target' \
    --exclude 'frontend/dist' \
    --exclude 'frontend/node_modules' \
    --exclude '.env' \
    --exclude 'docker/.env' \
    --exclude '*.log' \
    "${PROJECT_ROOT}/" "${SERVER_USER}@${SERVER_IP}:/opt/law-firm/"

log_success "代码上传完成"

# 在服务器上执行部署
log_info "在服务器上执行部署..."
ssh "${SERVER_USER}@${SERVER_IP}" << 'ENDSSH'
    set -e
    
    cd /opt/law-firm
    
    # 检查 Docker
    if ! command -v docker &> /dev/null; then
        echo "正在安装 Docker..."
        curl -fsSL https://get.docker.com | sh
        systemctl start docker
        systemctl enable docker
    fi
    
    # 检查 Docker Compose
    if ! docker compose version &> /dev/null; then
        echo "正在安装 Docker Compose..."
        apt-get update
        apt-get install -y docker-compose-plugin || yum install -y docker-compose-plugin
    fi
    
    # 配置环境变量（如果不存在）
    if [ ! -f docker/.env ]; then
        echo "创建环境变量文件..."
        cp docker/env.example docker/.env
        echo "⚠️  请编辑 docker/.env 文件配置密码后重新运行部署"
        echo "可以使用: ssh ${SERVER_USER}@${SERVER_IP} 'vim /opt/law-firm/docker/.env'"
    fi
    
    # 配置 SSL 证书（如果存在）
    if [ -f docker/ssl/ca.crt ] && [ -f docker/ssl/ca.key ]; then
        echo "配置 SSL 证书..."
        cd docker/ssl
        ln -sf ca.crt fullchain.pem 2>/dev/null || true
        ln -sf ca.key privkey.pem 2>/dev/null || true
        chmod 644 fullchain.pem 2>/dev/null || true
        chmod 600 privkey.pem 2>/dev/null || true
        cd ../..
    fi
    
    # 给脚本执行权限（递归所有子目录）
    find scripts -type f -name "*.sh" -exec chmod +x {} \;
    
    # 执行部署
    echo "开始部署..."
    cd docker
    docker compose -f docker-compose.prod.yml up -d --build
    
    echo ""
    echo "✅ 部署完成！"
    echo ""
    echo "访问地址:"
    echo "  - HTTP:  http://${SERVER_IP}/"
    echo "  - HTTPS: https://${SERVER_IP}/ (如果配置了SSL)"
    echo ""
    echo "查看日志:"
    echo "  docker compose -f docker-compose.prod.yml logs -f"
    echo ""
    echo "默认账号: admin / admin123"
ENDSSH

log_success "部署完成！"
log_info "访问地址: http://${SERVER_IP}/"
log_info "查看日志: ssh ${SERVER_USER}@${SERVER_IP} 'cd /opt/law-firm/docker && docker compose -f docker-compose.prod.yml logs -f'"
