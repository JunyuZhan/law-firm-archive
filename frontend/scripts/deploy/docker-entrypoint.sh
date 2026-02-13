#!/bin/sh
# =====================================================
# 前端容器启动脚本
# SSL 证书可选：有证书用 HTTPS，没证书用纯 HTTP
# 支持非 root 用户运行（nginx-unprivileged 镜像）
# =====================================================

set -e

SSL_DIR="/etc/nginx/ssl"
CERT_FILE="$SSL_DIR/fullchain.pem"
KEY_FILE="$SSL_DIR/privkey.pem"

# 使用临时目录存放运行时配置（非 root 用户可写）
NGINX_CONF="/tmp/nginx.conf"
NGINX_CONF_ORIG="/etc/nginx/nginx.conf"
NGINX_HTTP_CONF="/etc/nginx/nginx-http.conf"

# 复制配置文件到可写目录
cp "$NGINX_CONF_ORIG" "$NGINX_CONF"

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
    
    # 如果没有证书，使用纯 HTTP 配置文件
    if [ -f "$NGINX_HTTP_CONF" ]; then
        echo "[SSL] 使用纯 HTTP 配置文件（无 SSL 证书）..."
        cp "$NGINX_HTTP_CONF" "$NGINX_CONF"
    else
        echo "[SSL] 警告：未找到 nginx-http.conf，尝试修改当前配置..."
        # 删除 HTTP 到 HTTPS 的重定向
        sed -i '/return 301 https:\/\//d' "$NGINX_CONF"
        # 删除 HTTPS server 块
        if grep -q "listen 8443 ssl" "$NGINX_CONF"; then
            sed -i '/# HTTPS 服务器/,/^  }$/d' "$NGINX_CONF"
        fi
        echo "[SSL] 注意：请确保 HTTP server 块中有完整的服务配置"
    fi
fi

# 启动 nginx（使用临时配置文件）
echo "[Nginx] 启动 nginx..."
exec nginx -c "$NGINX_CONF" -g "daemon off;"
