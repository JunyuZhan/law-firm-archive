#!/bin/sh
# =====================================================
# 前端容器启动脚本
# SSL 证书可选：有证书用 HTTPS，没证书用纯 HTTP
# =====================================================

set -e

SSL_DIR="/etc/nginx/ssl"
CERT_FILE="$SSL_DIR/fullchain.pem"
KEY_FILE="$SSL_DIR/privkey.pem"
NGINX_CONF="/etc/nginx/nginx.conf"

# 确保 SSL 目录存在
mkdir -p "$SSL_DIR"

# 检查是否有 SSL 证书
if [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ]; then
    echo "[SSL] 检测到 SSL 证书，启用 HTTPS"
    echo "[SSL]   - 证书: $CERT_FILE"
    echo "[SSL]   - 私钥: $KEY_FILE"
    echo "[SSL] HTTP: 8080, HTTPS: 8443"
else
    echo "[SSL] 未检测到 SSL 证书，仅使用 HTTP 模式"
    echo "[SSL] HTTP: 8080"
    echo ""
    echo "[提示] 如需启用 HTTPS，请挂载证书到容器："
    echo "       - $CERT_FILE"
    echo "       - $KEY_FILE"
    echo ""
    
    # 注释掉 HTTPS server 块，避免 nginx 启动失败
    # 使用 sed 将 HTTPS server 块注释掉
    if grep -q "listen 8443 ssl" "$NGINX_CONF"; then
        echo "[SSL] 正在禁用 HTTPS 配置..."
        # 删除整个 HTTPS server 块（从 "# HTTPS 服务器" 到对应的结束大括号）
        sed -i '/# HTTPS 服务器/,/^  }$/d' "$NGINX_CONF"
        echo "[SSL] HTTPS 配置已禁用"
    fi
fi

# 启动 nginx
echo "[Nginx] 启动 nginx..."
exec nginx -g "daemon off;"
