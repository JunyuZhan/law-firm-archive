#!/bin/sh
# MinIO 初始化脚本 - 开发环境
# 创建 bucket 并设置公开下载权限

# 等待 MinIO 启动
sleep 5

# 配置 mc 客户端
mc alias set local http://localhost:9000 minioadmin minioadmin

# 创建 bucket（如果不存在）
mc mb local/law-firm --ignore-existing

# 设置 bucket 为公开下载（开发环境）
mc anonymous set download local/law-firm

echo "MinIO bucket 'law-firm' 初始化完成，已设置公开下载权限"
