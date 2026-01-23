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

# OnlyOffice CORS 说明
# 生产环境：通过前端 Nginx 反向代理，所有请求在同一域名下，无跨域问题
# 开发环境：前端(localhost:5666)直接访问 OnlyOffice(127.0.0.1:8088)，需要手动配置 CORS
# 开发环境 CORS 配置方法：docker exec law-firm-onlyoffice 执行脚本添加 CORS 头
echo "✓ OnlyOffice 启动完成（CORS 由前端代理处理）"

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
