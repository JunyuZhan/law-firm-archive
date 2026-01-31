#!/bin/bash
# =====================================================
# SSL 证书上传脚本
# =====================================================
# 用法:
#   ./upload-ssl-certs.sh <服务器IP> [用户名] [证书路径]
#   例如: ./upload-ssl-certs.sh 192.168.1.100 root
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
    echo "用法: $0 <服务器IP> [用户名] [证书目录]"
    echo "例如: $0 192.168.1.100 root"
    echo "      $0 192.168.1.100 root /path/to/certs"
    exit 1
fi

SERVER_IP="$1"
SERVER_USER="${2:-root}"
CERT_DIR="${3:-docker/ssl}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOCAL_CERT_DIR="$PROJECT_ROOT/$CERT_DIR"

log_info "准备上传 SSL 证书到服务器: ${SERVER_USER}@${SERVER_IP}"

# 检查本地证书目录
if [ ! -d "$LOCAL_CERT_DIR" ]; then
    log_error "证书目录不存在: $LOCAL_CERT_DIR"
    exit 1
fi

# 检查证书文件
CERT_FILES=()
if [ -f "$LOCAL_CERT_DIR/ca.crt" ] && [ -f "$LOCAL_CERT_DIR/ca.key" ]; then
    CERT_FILES=("ca.crt" "ca.key")
    log_info "找到证书文件: ca.crt, ca.key"
elif [ -f "$LOCAL_CERT_DIR/fullchain.pem" ] && [ -f "$LOCAL_CERT_DIR/privkey.pem" ]; then
    CERT_FILES=("fullchain.pem" "privkey.pem")
    log_info "找到证书文件: fullchain.pem, privkey.pem"
else
    log_error "未找到证书文件！"
    echo "请确保以下文件之一存在："
    echo "  - $LOCAL_CERT_DIR/ca.crt 和 ca.key"
    echo "  - $LOCAL_CERT_DIR/fullchain.pem 和 privkey.pem"
    exit 1
fi

# 检查 SSH 连接
log_info "检查 SSH 连接..."
if ! ssh -o ConnectTimeout=5 "${SERVER_USER}@${SERVER_IP}" "echo 'SSH连接成功'" &>/dev/null; then
    log_error "无法连接到服务器"
    exit 1
fi
log_success "SSH 连接正常"

# 在服务器上创建证书目录
log_info "在服务器上创建证书目录..."
ssh "${SERVER_USER}@${SERVER_IP}" "mkdir -p /opt/law-firm/docker/ssl"

# 上传证书文件
log_info "上传证书文件..."
for cert_file in "${CERT_FILES[@]}"; do
    if [ -f "$LOCAL_CERT_DIR/$cert_file" ]; then
        log_info "上传 $cert_file..."
        scp "$LOCAL_CERT_DIR/$cert_file" "${SERVER_USER}@${SERVER_IP}:/opt/law-firm/docker/ssl/"
    fi
done

# 在服务器上配置证书
log_info "在服务器上配置证书..."
ssh "${SERVER_USER}@${SERVER_IP}" << 'ENDSSH'
    cd /opt/law-firm/docker/ssl
    
    # 如果使用 ca.crt，创建符号链接
    if [ -f ca.crt ] && [ -f ca.key ]; then
        echo "创建符号链接..."
        ln -sf ca.crt fullchain.pem 2>/dev/null || true
        ln -sf ca.key privkey.pem 2>/dev/null || true
    fi
    
    # 设置文件权限
    echo "设置文件权限..."
    chmod 644 fullchain.pem 2>/dev/null || chmod 644 ca.crt 2>/dev/null || true
    chmod 600 privkey.pem 2>/dev/null || chmod 600 ca.key 2>/dev/null || true
    
    # 显示证书信息
    echo ""
    echo "证书文件列表:"
    ls -lh
    
    # 验证证书（如果可用）
    if command -v openssl &> /dev/null; then
        if [ -f fullchain.pem ]; then
            echo ""
            echo "证书信息:"
            openssl x509 -in fullchain.pem -text -noout 2>/dev/null | grep -E "(Subject:|Issuer:|Not Before|Not After)" || true
        fi
    fi
ENDSSH

log_success "证书上传完成！"
log_info "证书位置: /opt/law-firm/docker/ssl/"
log_info ""
log_info "下一步："
log_info "1. 确保 docker-compose.prod.yml 中已配置证书挂载"
log_info "2. 重新部署前端服务以启用 HTTPS"
log_info "3. 访问 https://${SERVER_IP} 测试"
