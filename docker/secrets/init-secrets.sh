#!/usr/bin/env bash
set -euo pipefail

# 与 Dist Center 安装器约定一致：在 docker/secrets 下准备占位文件（若后续启用 Nginx basic auth 可写入 htpasswd）。
touch htpasswd
