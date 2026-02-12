#!/bin/bash
# =====================================================
# SSL 证书检查脚本
# 检查证书是否在 Nginx 中正常工作
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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# 脚本在 scripts/ssl/ 目录下，需要向上两级到项目根目录
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SSL_DIR="$PROJECT_ROOT/docker/ssl"
CERT_FILE="$SSL_DIR/fullchain.pem"
KEY_FILE="$SSL_DIR/privkey.pem"
CONTAINER_NAME="law-firm-frontend"

echo "=========================================="
echo "SSL 证书检查"
echo "=========================================="
echo ""

# 1. 检查本地证书文件
log_info "1. 检查本地证书文件..."
if [ -f "$CERT_FILE" ]; then
    log_success "证书文件存在: $CERT_FILE"
else
    log_error "证书文件不存在: $CERT_FILE"
    exit 1
fi

if [ -f "$KEY_FILE" ]; then
    log_success "私钥文件存在: $KEY_FILE"
else
    log_error "私钥文件不存在: $KEY_FILE"
    exit 1
fi

# 2. 检查证书和私钥是否匹配
log_info ""
log_info "2. 检查证书和私钥是否匹配..."
CERT_MD5=$(openssl x509 -noout -modulus -in "$CERT_FILE" 2>/dev/null | openssl md5)
KEY_MD5=$(openssl rsa -noout -modulus -in "$KEY_FILE" 2>/dev/null | openssl md5 || openssl ec -noout -pubout -in "$KEY_FILE" 2>/dev/null | openssl md5 || openssl pkey -noout -pubout -in "$KEY_FILE" 2>/dev/null | openssl md5)

if [ "$CERT_MD5" = "$KEY_MD5" ]; then
    log_success "证书和私钥匹配 ✓"
else
    log_error "证书和私钥不匹配 ✗"
    log_warn "证书 MD5: $CERT_MD5"
    log_warn "私钥 MD5: $KEY_MD5"
fi

# 3. 检查证书信息
log_info ""
log_info "3. 证书信息..."
echo "   Subject: $(openssl x509 -in "$CERT_FILE" -noout -subject 2>/dev/null | sed 's/subject=//')"
echo "   Issuer:  $(openssl x509 -in "$CERT_FILE" -noout -issuer 2>/dev/null | sed 's/issuer=//')"
echo "   有效期:  $(openssl x509 -in "$CERT_FILE" -noout -dates 2>/dev/null | grep notAfter | sed 's/notAfter=//')"

# 4. 检查容器是否运行
log_info ""
log_info "4. 检查容器状态..."
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    log_success "容器正在运行: $CONTAINER_NAME"
else
    log_error "容器未运行: $CONTAINER_NAME"
    log_warn "请先启动容器: cd docker && docker compose -f docker-compose.yml up -d"
    exit 1
fi

# 5. 检查容器内的证书文件
log_info ""
log_info "5. 检查容器内的证书文件..."
CONTAINER_CERT="/etc/nginx/ssl/fullchain.pem"
CONTAINER_KEY="/etc/nginx/ssl/privkey.pem"

if docker exec "$CONTAINER_NAME" test -f "$CONTAINER_CERT" 2>/dev/null; then
    log_success "容器内证书文件存在: $CONTAINER_CERT"
else
    log_error "容器内证书文件不存在: $CONTAINER_CERT"
    log_warn "请检查 docker-compose.yml 中的 volume 挂载配置"
fi

if docker exec "$CONTAINER_NAME" test -f "$CONTAINER_KEY" 2>/dev/null; then
    log_success "容器内私钥文件存在: $CONTAINER_KEY"
else
    log_error "容器内私钥文件不存在: $CONTAINER_KEY"
    log_warn "请检查 docker-compose.yml 中的 volume 挂载配置"
fi

# 6. 检查 Nginx 配置
log_info ""
log_info "6. 检查 Nginx 配置..."
if docker exec "$CONTAINER_NAME" nginx -t 2>&1 | grep -q "successful"; then
    log_success "Nginx 配置正确 ✓"
    docker exec "$CONTAINER_NAME" nginx -t 2>&1 | grep -E "(test is successful|syntax is ok)"
else
    log_error "Nginx 配置有错误 ✗"
    docker exec "$CONTAINER_NAME" nginx -t 2>&1
    exit 1
fi

# 7. 检查 Nginx 是否监听 HTTPS 端口
log_info ""
log_info "7. 检查 Nginx 端口监听..."
if docker exec "$CONTAINER_NAME" netstat -tlnp 2>/dev/null | grep -q ":8443" || docker exec "$CONTAINER_NAME" ss -tlnp 2>/dev/null | grep -q ":8443"; then
    log_success "Nginx 正在监听 HTTPS 端口 8443 ✓"
else
    log_warn "Nginx 未监听 HTTPS 端口 8443"
    log_warn "可能原因：未检测到证书，使用了纯 HTTP 模式"
fi

if docker exec "$CONTAINER_NAME" netstat -tlnp 2>/dev/null | grep -q ":8080" || docker exec "$CONTAINER_NAME" ss -tlnp 2>/dev/null | grep -q ":8080"; then
    log_success "Nginx 正在监听 HTTP 端口 8080 ✓"
fi

# 8. 检查启动日志
log_info ""
log_info "8. 检查容器启动日志（SSL 相关）..."
SSL_LOG=$(docker logs "$CONTAINER_NAME" 2>&1 | grep -i "ssl\|certificate" | tail -5)
if [ -n "$SSL_LOG" ]; then
    echo "$SSL_LOG"
else
    log_warn "未找到 SSL 相关日志"
fi

# 9. 测试 HTTPS 连接（如果可能）
log_info ""
log_info "9. 测试 HTTPS 连接..."
HOST_IP=$(docker inspect "$CONTAINER_NAME" --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' 2>/dev/null | head -1)
if [ -n "$HOST_IP" ]; then
    log_info "容器 IP: $HOST_IP"
    if docker exec "$CONTAINER_NAME" curl -k -I https://localhost:8443 2>/dev/null | head -1 | grep -q "HTTP"; then
        log_success "HTTPS 连接测试成功 ✓"
        docker exec "$CONTAINER_NAME" curl -k -I https://localhost:8443 2>/dev/null | head -1
    else
        log_warn "HTTPS 连接测试失败（可能是证书问题或服务未启动）"
    fi
else
    log_warn "无法获取容器 IP，跳过连接测试"
fi

# 10. 总结
echo ""
echo "=========================================="
log_info "检查完成！"
echo "=========================================="
echo ""
log_info "如果所有检查都通过，说明证书配置正确。"
log_info "如果发现问题，请根据上述提示进行修复。"
echo ""
log_info "访问测试："
echo "  - HTTP:  http://localhost 或 http://192.168.50.10"
echo "  - HTTPS: https://localhost 或 https://192.168.50.10"
echo ""
log_warn "注意：自签名证书在浏览器中会显示警告，需要导入 CA 证书才能消除警告。"
