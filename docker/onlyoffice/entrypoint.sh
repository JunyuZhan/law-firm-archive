#!/bin/bash
set -e

# 自定义字体目录
FONT_DIR="/usr/share/fonts/truetype/custom"

echo "=========================================="
echo "OnlyOffice 字体安装脚本"
echo "=========================================="

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
        fc-cache -fv
        echo "✓ 系统字体缓存已刷新"
    fi
else
    echo "⚠ 自定义字体目录不存在: $FONT_DIR"
fi

# 在后台启动字体注册脚本（等待服务启动后运行）
/app/register-fonts.sh &

# 查找 OnlyOffice 的原始 entrypoint
ORIGINAL_ENTRYPOINT="/app/ds/run-document-server.sh"

if [ ! -f "$ORIGINAL_ENTRYPOINT" ]; then
    # 如果标准路径不存在，尝试查找其他可能的路径
    ORIGINAL_ENTRYPOINT=$(find /app -name "run-document-server.sh" -type f 2>/dev/null | head -n 1)
fi

if [ -z "$ORIGINAL_ENTRYPOINT" ] || [ ! -f "$ORIGINAL_ENTRYPOINT" ]; then
    # 如果找不到原始 entrypoint，直接启动 OnlyOffice 的 systemd
    echo "⚠ 未找到原始 entrypoint，使用默认方式启动..."
    exec supervisord -c /etc/supervisor/conf.d/supervisord.conf
else
    echo "使用原始 entrypoint: $ORIGINAL_ENTRYPOINT"
    # 启动 OnlyOffice 服务的原始 entrypoint
    # 使用 exec 执行原 entrypoint，以便正确处理信号
    exec "$ORIGINAL_ENTRYPOINT" "$@"
fi
