#!/bin/bash
# =====================================================
# 下载 CA 证书脚本
# =====================================================
# 用法: ./download-ca-cert.sh [服务器IP] [用户名]
# =====================================================

SERVER_IP="${1:-192.168.50.10}"
SERVER_USER="${2:-root}"
CERT_PATH="/opt/law-firm/docker/ssl/ca.crt"
DOWNLOAD_DIR="$HOME/Downloads"

echo "正在从 ${SERVER_USER}@${SERVER_IP} 下载 CA 证书..."
scp ${SERVER_USER}@${SERVER_IP}:${CERT_PATH} ${DOWNLOAD_DIR}/law-firm-ca.crt

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 证书已下载到: ${DOWNLOAD_DIR}/law-firm-ca.crt"
    echo ""
    echo "下一步操作："
    echo ""
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macOS:"
        echo "  1. 双击证书文件打开钥匙串访问"
        echo "  2. 找到导入的证书，双击打开"
        echo "  3. 展开'信任'，将'使用此证书时'设置为'始终信任'"
        echo "  4. 重启浏览器"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "Linux:"
        echo "  1. sudo cp ${DOWNLOAD_DIR}/law-firm-ca.crt /usr/local/share/ca-certificates/"
        echo "  2. sudo update-ca-certificates"
        echo "  3. 重启浏览器"
    else
        echo "Windows:"
        echo "  1. 双击证书文件"
        echo "  2. 点击'安装证书'"
        echo "  3. 选择'本地计算机' → 下一步"
        echo "  4. 选择'将所有证书都放入下列存储' → 浏览"
        echo "  5. 选择'受信任的根证书颁发机构' → 确定 → 完成"
        echo "  6. 重启浏览器"
    fi
else
    echo "❌ 下载失败，请检查："
    echo "  - SSH 连接是否正常"
    echo "  - 证书路径是否正确"
    exit 1
fi
