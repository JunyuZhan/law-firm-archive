#!/bin/bash
# OnlyOffice 中文字体安装/更新脚本
# 用于在运行中的 OnlyOffice 容器中安装或更新中文字体

set -e

CONTAINER_NAME="law-firm-onlyoffice"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FONTS_DIR="$SCRIPT_DIR/onlyoffice/fonts"

echo "=========================================="
echo "  OnlyOffice 中文字体安装/更新"
echo "=========================================="

# 检查容器是否运行
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "❌ 错误: 容器 ${CONTAINER_NAME} 未运行"
    echo "请先启动 OnlyOffice 容器: docker-compose up -d onlyoffice"
    exit 1
fi

echo "1. 复制自定义字体到容器..."
for font in "$FONTS_DIR"/*.TTF "$FONTS_DIR"/*.ttf; do
    if [ -f "$font" ]; then
        filename=$(basename "$font")
        echo "   复制: $filename"
        docker cp "$font" "${CONTAINER_NAME}:/usr/share/fonts/truetype/custom/"
    fi
done

echo ""
echo "2. 在容器中安装字体并生成字体列表..."
docker exec -i "$CONTAINER_NAME" bash -c '
    # 确保目录存在
    mkdir -p /usr/share/fonts/truetype/custom
    
    echo "   安装中文字体包..."
    apt-get update -qq
    apt-get install -y --no-install-recommends \
        fonts-wqy-zenhei \
        fonts-wqy-microhei \
        fonts-noto-cjk \
        fontconfig \
        2>/dev/null || true

    echo "   刷新字体缓存..."
    fc-cache -fv

    echo "   生成 OnlyOffice 字体列表..."
    cd /var/www/onlyoffice/documentserver/server/tools
    ./allfontsgen

    echo ""
    echo "   已安装的中文字体:"
    fc-list :lang=zh | head -20 || echo "   (字体列表获取中...)"
'

echo ""
echo "=========================================="
echo "  ✅ 安装完成！"
echo "=========================================="
echo ""
echo "提示: 请刷新浏览器页面，新字体将在文档编辑器中可用。"
echo ""
echo "已安装的自定义字体:"
echo "  - 仿宋_GB2312 (法律文书常用)"
echo "  - 楷体_GB2312 (法律文书常用)"
echo "  - 方正小标宋简 (公文标题常用)"
echo ""
