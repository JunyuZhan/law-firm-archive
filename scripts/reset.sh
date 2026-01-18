#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 环境重置脚本
# =====================================================
# ⚠️ 警告：此脚本会删除所有数据！仅用于开发重置或从头部署。
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo -e "${RED}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║                       DANGER ZONE                            ║${NC}"
echo -e "${RED}╠══════════════════════════════════════════════════════════════╣${NC}"
echo -e "${RED}║  此操作将执行以下清理：                                      ║${NC}"
echo -e "${RED}║  1. 停止并删除所有 Law Firm 相关的 Docker 容器               ║${NC}"
echo -e "${RED}║  2. 删除所有 Docker 数据卷 (❌ 数据库数据将永久丢失!)        ║${NC}"
echo -e "${RED}║  3. 重命名现有的 .env 配置文件 (将强制生成新密码)            ║${NC}"
echo -e "${RED}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}请确认您完全理解此操作的后果。${NC}"
read -p "输入 'delete' 确认重置: " confirm

if [ "$confirm" != "delete" ]; then
    echo "操作已取消。"
    exit 1
fi

echo ""
echo -e "${YELLOW}[1/3] 正在停止服务并删除容器...${NC}"
cd "$PROJECT_ROOT/docker"
docker compose -f docker-compose.prod.yml down -v --remove-orphans

echo ""
echo -e "${YELLOW}[2/3] 正在深度清理数据卷...${NC}"
# 尝试查找并删除可能残留的项目卷
VOLUMES=$(docker volume ls -q | grep "law-firm" || true)
if [ -n "$VOLUMES" ]; then
    echo "发现残留卷: $VOLUMES"
    docker volume rm $VOLUMES || echo "部分卷可能已被删除或正被占用"
else
    echo "没有发现残留卷。"
fi

echo ""
echo -e "${YELLOW}[3/3] 处理配置文件...${NC}"
cd "$PROJECT_ROOT"
if [ -f ".env" ]; then
    BACKUP_NAME=".env.backup.$(date +%Y%m%d_%H%M%S)"
    mv .env "$BACKUP_NAME"
    echo -e "${GREEN}原 .env 文件已备份为 $BACKUP_NAME${NC}"
else
    echo ".env 文件不存在，跳过。"
fi

echo ""
echo -e "${GREEN}✅ 环境重置完成！${NC}"
echo -e "现在您可以运行 ${BOLD}./deploy.sh${NC} 进行全新的部署。"
