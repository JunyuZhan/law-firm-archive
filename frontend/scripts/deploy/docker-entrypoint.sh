#!/bin/sh
# =====================================================
# 前端容器启动脚本
# 自动处理 SSL 证书（如果未挂载则生成自签名证书）
# =====================================================

set -e

SSL_DIR="/etc/nginx/ssl"
CERT_FILE="$SSL_DIR/fullchain.pem"
KEY_FILE="$SSL_DIR/privkey.pem"

# 确保 SSL 目录存在
mkdir -p "$SSL_DIR"

# 检查是否需要生成自签名证书
if [ ! -f "$CERT_FILE" ] || [ ! -f "$KEY_FILE" ]; then
    echo "[SSL] 未检测到 SSL 证书，正在生成自签名证书..."
    
    # 检测服务器 IP（用于证书 SAN）
    SERVER_IP=$(hostname -i 2>/dev/null | awk '{print $1}' || echo "127.0.0.1")
    
    # 生成自签名证书（有效期 365 天）
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$KEY_FILE" \
        -out "$CERT_FILE" \
        -subj "/C=CN/ST=Beijing/L=Beijing/O=LawFirm/CN=localhost" \
        -addext "subjectAltName=DNS:localhost,IP:127.0.0.1,IP:$SERVER_IP" \
        2>/dev/null
    
    chmod 644 "$CERT_FILE"
    chmod 600 "$KEY_FILE"
    
    echo "[SSL] 自签名证书已生成"
    echo "[SSL]   - 证书: $CERT_FILE"
    echo "[SSL]   - 私钥: $KEY_FILE"
    echo "[SSL]   - 有效期: 365 天"
    echo "[SSL]   - SAN: localhost, 127.0.0.1, $SERVER_IP"
    echo "[SSL] 提示: 生产环境请使用正式证书替换"
else
    echo "[SSL] 检测到已存在的 SSL 证书"
fi

# 启动 nginx
echo "[Nginx] 启动 nginx..."
exec nginx -g "daemon off;"
