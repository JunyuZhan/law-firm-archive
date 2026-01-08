#!/bin/bash
# OnlyOffice 中文字体安装脚本
# 在 OnlyOffice 容器中执行此脚本来安装中文字体

echo "=========================================="
echo "  OnlyOffice 中文字体安装"
echo "=========================================="

# 进入容器执行安装
docker exec -it law-onlyoffice bash -c '
    echo "1. 更新软件源..."
    apt-get update -qq

    echo "2. 安装中文字体包..."
    apt-get install -y --no-install-recommends \
        fonts-wqy-zenhei \
        fonts-wqy-microhei \
        fonts-noto-cjk \
        fonts-noto-cjk-extra \
        fontconfig \
        2>/dev/null

    echo "3. 刷新字体缓存..."
    fc-cache -fv

    echo "4. 生成 OnlyOffice 字体列表..."
    cd /var/www/onlyoffice/documentserver/server/tools
    ./allfontsgen

    echo "5. 验证安装的中文字体..."
    fc-list :lang=zh | head -10 || echo "中文字体列表..."

    echo ""
    echo "✅ 中文字体安装完成！"
    echo "请刷新浏览器页面重新打开文档。"
'

echo ""
echo "=========================================="
echo "  安装完成！"
echo "=========================================="

