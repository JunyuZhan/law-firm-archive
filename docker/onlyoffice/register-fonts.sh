#!/bin/bash
set -e

FONTSGEN_TOOL="/var/www/onlyoffice/documentserver/server/tools/allfontsgen"

echo "=========================================="
echo "OnlyOffice 字体注册脚本"
echo "=========================================="

# 等待 OnlyOffice 服务启动
echo "等待 OnlyOffice 服务启动..."
MAX_ATTEMPTS=60
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -sf http://localhost/healthcheck > /dev/null 2>&1; then
        echo "✓ OnlyOffice 服务已启动"
        break
    fi
    
    ATTEMPT=$((ATTEMPT + 1))
    if [ $((ATTEMPT % 5)) -eq 0 ]; then
        echo "等待中... ($ATTEMPT/$MAX_ATTEMPTS)"
    fi
    sleep 2
done

if [ $ATTEMPT -ge $MAX_ATTEMPTS ]; then
    echo "⚠ 服务启动超时，字体将在下次重启时注册"
    exit 0
fi

# 额外等待几秒，确保服务完全就绪
sleep 5

# 运行字体生成工具
echo "开始注册字体..."
if [ -f "$FONTSGEN_TOOL" ]; then
    cd /var/www/onlyoffice/documentserver/server/tools
    
    # 运行字体生成工具
    if ./allfontsgen; then
        echo "✓ 字体注册完成"
    else
        echo "⚠ 字体注册失败，将在下次重启时重试"
        exit 0
    fi
else
    echo "⚠ 字体生成工具不存在: $FONTSGEN_TOOL"
    exit 0
fi

echo "=========================================="
