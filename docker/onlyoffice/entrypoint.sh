#!/bin/bash
set -e

# 自定义字体目录
FONT_DIR="/usr/share/fonts/truetype/custom"
CACHE_DIR="/var/www/onlyoffice/documentserver/.cache"

echo "=========================================="
echo "OnlyOffice 启动脚本 (法律事务管理系统)"
echo "=========================================="

# 修复 .cache 目录权限（运行时修复，以防 volume 覆盖）
echo "修复缓存目录权限..."
mkdir -p "$CACHE_DIR"
chown -R ds:ds "$CACHE_DIR" 2>/dev/null || true
chmod -R 755 "$CACHE_DIR" 2>/dev/null || true
echo "✓ 缓存目录权限已修复"

# 确保自定义字体目录存在
if [ -d "$FONT_DIR" ]; then
    echo "检查自定义字体文件..."
    FONT_COUNT=$(find "$FONT_DIR" -type f \( -name "*.TTF" -o -name "*.ttf" \) 2>/dev/null | wc -l)
    echo "找到 $FONT_COUNT 个字体文件"
    
    if [ "$FONT_COUNT" -gt 0 ]; then
        # 设置字体文件权限
        chmod 644 "$FONT_DIR"/*.{TTF,ttf} 2>/dev/null || true
        echo "✓ 字体文件权限已设置"
        
        # 刷新系统字体缓存
        fc-cache -fv 2>/dev/null || true
        echo "✓ 系统字体缓存已刷新"
    fi
else
    echo "⚠ 自定义字体目录不存在: $FONT_DIR"
fi

# 在后台启动字体注册脚本（等待服务启动后运行）
nohup /app/register-fonts.sh > /var/log/onlyoffice/register-fonts.log 2>&1 &

# 配置 OnlyOffice Nginx CORS 支持
# 虽然前端代理会处理 CORS，但 OnlyOffice 内部资源（如 Editor.bin）可能直接访问 127.0.0.1:8088
# 因此需要在 OnlyOffice 容器内部也配置 CORS
echo "配置 OnlyOffice CORS 支持..."
DOCSERVICE_CONF="/etc/onlyoffice/documentserver/nginx/includes/ds-docservice.conf"

# 在 ds-docservice.conf 的 cache/files location 块中添加 CORS 头
if [ -f "$DOCSERVICE_CONF" ]; then
    # 检查是否已配置 CORS
    if ! grep -q "Access-Control-Allow-Origin" "$DOCSERVICE_CONF"; then
        # 在 add_header Content-Disposition 之后添加 CORS 头
        sed -i '/add_header Content-Disposition/a\
  # CORS 头（由 entrypoint.sh 自动添加）\
  add_header Access-Control-Allow-Origin * always;\
  add_header Access-Control-Allow-Methods "GET, POST, OPTIONS, PUT, DELETE" always;\
  add_header Access-Control-Allow-Headers "Authorization, Content-Type, X-Requested-With" always;\
  add_header Access-Control-Allow-Credentials true always;' "$DOCSERVICE_CONF"
        echo "✓ CORS 配置已添加到 ds-docservice.conf"
    else
        echo "✓ CORS 配置已存在"
    fi
else
    echo "⚠ 未找到 ds-docservice.conf 配置文件: $DOCSERVICE_CONF"
fi
echo "✓ OnlyOffice 启动完成"

# 查找 OnlyOffice 的原始 entrypoint
# OnlyOffice 8.x/9.x 使用不同的路径
ORIGINAL_ENTRYPOINT=""
POSSIBLE_PATHS=(
    "/app/ds/run-document-server.sh"
    "/usr/bin/documentserver-generate-allfonts.sh"
)

for path in "${POSSIBLE_PATHS[@]}"; do
    if [ -f "$path" ] && [ -x "$path" ]; then
        ORIGINAL_ENTRYPOINT="$path"
        break
    fi
done

# 如果还没找到，搜索
if [ -z "$ORIGINAL_ENTRYPOINT" ]; then
    ORIGINAL_ENTRYPOINT=$(find /app -name "run-document-server.sh" -type f 2>/dev/null | head -n 1)
fi

if [ -z "$ORIGINAL_ENTRYPOINT" ] || [ ! -f "$ORIGINAL_ENTRYPOINT" ]; then
    # 如果找不到原始 entrypoint，使用 supervisord 启动
    echo "使用 supervisord 方式启动..."
    
    # 查找 supervisord 配置文件
    SUPERVISOR_CONF=""
    if [ -f "/etc/supervisor/supervisord.conf" ]; then
        SUPERVISOR_CONF="/etc/supervisor/supervisord.conf"
    elif [ -f "/etc/supervisor/conf.d/supervisord.conf" ]; then
        SUPERVISOR_CONF="/etc/supervisor/conf.d/supervisord.conf"
    elif [ -f "/etc/supervisord.conf" ]; then
        SUPERVISOR_CONF="/etc/supervisord.conf"
    fi
    
    if [ -n "$SUPERVISOR_CONF" ]; then
        echo "使用配置文件: $SUPERVISOR_CONF"
        exec /usr/bin/supervisord -n -c "$SUPERVISOR_CONF"
    else
        echo "⚠ 未找到 supervisord 配置，尝试无配置启动..."
        exec /usr/bin/supervisord -n
    fi
else
    echo "使用原始 entrypoint: $ORIGINAL_ENTRYPOINT"
    # 启动 OnlyOffice 服务的原始 entrypoint
    # 使用 exec 执行原 entrypoint，以便正确处理信号
    exec "$ORIGINAL_ENTRYPOINT" "$@"
fi
