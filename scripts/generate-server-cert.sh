#!/bin/bash
# =====================================================
# 生成服务器证书脚本
# =====================================================
# 用法:
#   ./generate-server-cert.sh <服务器IP或域名> [CA证书路径] [CA私钥路径]
#   例如: ./generate-server-cert.sh 192.168.50.10
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
    log_error "请提供服务器IP或域名"
    echo "用法: $0 <服务器IP或域名> [CA证书路径] [CA私钥路径]"
    echo "例如: $0 192.168.50.10"
    exit 1
fi

SERVER_NAME="$1"
CA_CERT="${2:-docker/ssl/ca.crt}"
CA_KEY="${3:-docker/ssl/ca.key}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SSL_DIR="$PROJECT_ROOT/docker/ssl"

cd "$PROJECT_ROOT"

# 检查CA证书和私钥
if [ ! -f "$CA_CERT" ]; then
    log_error "CA证书不存在: $CA_CERT"
    exit 1
fi

if [ ! -f "$CA_KEY" ]; then
    log_error "CA私钥不存在: $CA_KEY"
    exit 1
fi

log_info "生成服务器证书..."
log_info "服务器名称: $SERVER_NAME"
log_info "CA证书: $CA_CERT"
log_info "CA私钥: $CA_KEY"

# 创建临时目录
TMP_DIR=$(mktemp -d)
trap "rm -rf $TMP_DIR" EXIT

# 生成服务器私钥
log_info "1. 生成服务器私钥..."
openssl genrsa -out "$TMP_DIR/server.key" 2048

# 生成服务器证书签名请求 (CSR)
log_info "2. 生成证书签名请求..."
openssl req -new -key "$TMP_DIR/server.key" -out "$TMP_DIR/server.csr" \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=LawFirm/CN=$SERVER_NAME" \
    -addext "subjectAltName=IP:$SERVER_NAME,DNS:$SERVER_NAME,DNS:localhost"

# 创建证书扩展配置文件
cat > "$TMP_DIR/server.ext" <<EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
IP.1 = $SERVER_NAME
DNS.1 = $SERVER_NAME
DNS.2 = localhost
EOF

# 使用CA证书签名服务器证书
log_info "3. 使用CA证书签名服务器证书..."
openssl x509 -req -in "$TMP_DIR/server.csr" \
    -CA "$CA_CERT" \
    -CAkey "$CA_KEY" \
    -CAcreateserial \
    -out "$TMP_DIR/server.crt" \
    -days 365 \
    -extfile "$TMP_DIR/server.ext"

# 创建完整证书链（服务器证书 + CA证书）
log_info "4. 创建完整证书链..."
cat "$TMP_DIR/server.crt" "$CA_CERT" > "$TMP_DIR/fullchain.pem"

# 复制到SSL目录
log_info "5. 保存证书文件..."
cp "$TMP_DIR/server.crt" "$SSL_DIR/server.crt"
cp "$TMP_DIR/server.key" "$SSL_DIR/server.key"
cp "$TMP_DIR/fullchain.pem" "$SSL_DIR/fullchain.pem"

# 设置文件权限
chmod 644 "$SSL_DIR/server.crt"
chmod 644 "$SSL_DIR/fullchain.pem"
chmod 600 "$SSL_DIR/server.key"

# 更新符号链接
cd "$SSL_DIR"
ln -sf server.crt fullchain.pem.new
ln -sf server.key privkey.pem.new

log_success "服务器证书生成完成！"
log_info ""
log_info "证书文件："
log_info "  - 服务器证书: $SSL_DIR/server.crt"
log_info "  - 服务器私钥: $SSL_DIR/server.key"
log_info "  - 完整证书链: $SSL_DIR/fullchain.pem"
log_info ""
log_info "下一步："
log_info "1. 备份现有证书（如果需要）"
log_info "2. 替换证书文件："
log_info "   cd $SSL_DIR"
log_info "   mv fullchain.pem fullchain.pem.backup"
log_info "   mv privkey.pem privkey.pem.backup"
log_info "   mv fullchain.pem.new fullchain.pem"
log_info "   mv privkey.pem.new privkey.pem"
log_info "3. 重启前端服务："
log_info "   cd $PROJECT_ROOT/docker"
log_info "   docker compose -f docker-compose.prod.yml restart frontend"
