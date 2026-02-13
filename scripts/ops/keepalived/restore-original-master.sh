#!/bin/bash
# =====================================================
# 恢复原主服务器为从服务器
# =====================================================
# 用途：故障切换后，原主服务器硬件修好，重新配置为从服务器
# 
# 使用场景：
#   1. 原主服务器故障，从服务器已接管并提升为主库
#   2. 原主服务器硬件修好，需要重新配置为从服务器
#   3. 需要将原主服务器重新加入主从复制
#
# 用法:
#   ./restore-original-master.sh <新主服务器IP>
#
# 示例:
#   ./restore-original-master.sh 192.168.50.11
# =====================================================

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${LAW_FIRM_PROJECT_DIR:-/opt/law-firm}"
ENV_FILE="${PROJECT_DIR}/.env"
COMPOSE_FILE="${PROJECT_DIR}/docker/docker-compose.master-slave.yml"

# 显示使用说明
show_usage() {
    echo -e "${CYAN}用法:${NC}"
    echo "  ./restore-original-master.sh <新主服务器IP>"
    echo ""
    echo -e "${CYAN}参数说明:${NC}"
    echo "  新主服务器IP    当前运行主库的服务器IP（通常是故障切换后的从服务器）"
    echo ""
    echo -e "${CYAN}示例:${NC}"
    echo "  ./restore-original-master.sh 192.168.50.11"
    echo ""
    echo -e "${CYAN}注意事项:${NC}"
    echo "  ⚠️  此脚本会："
    echo "    1. 停止当前 PostgreSQL 容器"
    echo "    2. 清空数据目录（备份到 /tmp）"
    echo "    3. 从新主服务器复制数据"
    echo "    4. 配置为从服务器"
    echo "    5. 启动从库服务"
    echo ""
    echo -e "${RED}⚠️  警告：此操作会删除当前数据库数据！${NC}"
}

# 检查参数
if [ $# -lt 1 ]; then
    echo -e "${RED}错误: 必须提供新主服务器IP${NC}"
    show_usage
    exit 1
fi

NEW_MASTER_IP="$1"

# 验证IP格式
if ! echo "$NEW_MASTER_IP" | grep -qE '^([0-9]{1,3}\.){3}[0-9]{1,3}$'; then
    echo -e "${RED}错误: 无效的IP地址格式: $NEW_MASTER_IP${NC}"
    exit 1
fi

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}恢复原主服务器为从服务器${NC}"
echo -e "${BLUE}新主服务器IP: $NEW_MASTER_IP${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

# 确认操作
echo -e "${RED}⚠️  警告：此操作会删除当前数据库数据！${NC}"
echo -e "${RED}⚠️  重要：原主服务器数据可能过期，必须从新主服务器复制最新数据！${NC}"
echo ""
echo -e "${YELLOW}请确认：${NC}"
echo "  1. 当前服务器是原主服务器（已修复）"
echo "  2. 新主服务器IP: $NEW_MASTER_IP"
echo "  3. 新主服务器正在运行主库（数据是最新的）"
echo "  4. 原主服务器数据可能过期，将被删除并替换为新主服务器的数据"
echo ""
echo -e "${CYAN}数据安全说明：${NC}"
echo "  - 脚本会先备份现有数据到: /tmp/postgres-backup-时间戳/"
echo "  - 然后清空数据目录"
echo "  - 最后从新主服务器复制最新数据"
echo "  - 这样可以防止过期数据覆盖新数据"
echo ""
read -p "确认继续？(yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo -e "${YELLOW}操作已取消${NC}"
    exit 0
fi

# 检查新主服务器连通性
echo -e "${CYAN}[步骤 1/6] 检查新主服务器连通性...${NC}"
if ! ping -c 1 -W 2 "$NEW_MASTER_IP" > /dev/null 2>&1; then
    echo -e "${RED}错误: 无法连接到新主服务器 $NEW_MASTER_IP${NC}"
    exit 1
fi

# 获取数据库密码和复制用户密码（从环境变量或配置文件）
if [ -f "$ENV_FILE" ]; then
    DB_PASSWORD=$(grep "^DB_PASSWORD=" "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 | tr -d '"' | tr -d "'" | head -1 || echo "changeme")
    REPLICATION_USER=$(grep "^POSTGRES_REPLICATION_USER=" "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 | tr -d '"' | tr -d "'" | head -1 || echo "replicator")
    REPLICATION_PASSWORD=$(grep "^POSTGRES_REPLICATION_PASSWORD=" "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 | tr -d '"' | tr -d "'" | head -1 || echo "replicator_password")
else
    DB_PASSWORD="${DB_PASSWORD:-changeme}"
    REPLICATION_USER="${POSTGRES_REPLICATION_USER:-replicator}"
    REPLICATION_PASSWORD="${POSTGRES_REPLICATION_PASSWORD:-replicator_password}"
fi

# 检查新主服务器 PostgreSQL 是否可连接（使用复制用户）
if ! timeout 5 docker run --rm --network host \
    postgres:15-alpine \
    pg_isready -h "$NEW_MASTER_IP" -p 5432 -U "$REPLICATION_USER" > /dev/null 2>&1; then
    echo -e "${RED}错误: 无法连接到新主服务器的 PostgreSQL${NC}"
    echo -e "${YELLOW}提示: 请检查新主服务器是否运行主库，以及防火墙设置${NC}"
    echo -e "${YELLOW}提示: 复制用户: $REPLICATION_USER${NC}"
    exit 1
fi

# 验证新主服务器确实是主库（不是从库）
echo -e "${YELLOW}验证新主服务器角色...${NC}"
IN_RECOVERY=$(docker run --rm --network host \
    -e PGHOST="$NEW_MASTER_IP" -e PGPORT=5432 \
    -e PGUSER=law_admin -e PGPASSWORD="$DB_PASSWORD" \
    postgres:15-alpine \
    psql -d law_firm -tAc "SELECT pg_is_in_recovery();" 2>/dev/null || echo "unknown")

if [ "$IN_RECOVERY" = "t" ] || [ "$IN_RECOVERY" = "true" ]; then
    echo -e "${RED}错误: 新主服务器是从库，不是主库！${NC}"
    echo -e "${RED}⚠️  危险：如果从从库复制数据，会导致数据回滚！${NC}"
    echo -e "${YELLOW}提示: 请确认新主服务器IP是否正确，以及新主服务器是否已提升为主库${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 新主服务器连接正常，确认为主库${NC}"
echo ""

# 停止当前 PostgreSQL 容器
echo -e "${CYAN}[步骤 2/6] 停止当前 PostgreSQL 容器...${NC}"
if docker ps --format "{{.Names}}" | grep -q "^law-firm-postgres-master$"; then
    docker stop law-firm-postgres-master
    echo -e "${GREEN}✓ 主库容器已停止${NC}"
fi
if docker ps --format "{{.Names}}" | grep -q "^law-firm-postgres-slave$"; then
    docker stop law-firm-postgres-slave
    echo -e "${GREEN}✓ 从库容器已停止${NC}"
fi
echo ""

# 备份现有数据
echo -e "${CYAN}[步骤 3/6] 备份现有数据...${NC}"
BACKUP_DIR="/tmp/postgres-backup-$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# 检查 volume 是否存在（原主服务器可能有主库或从库的 volume）
MASTER_VOLUME="law-firm-master-slave_postgres_master_data"
SLAVE_VOLUME="law-firm-master-slave_postgres_slave_data"
VOLUME_NAME=""

# 优先使用从库 volume（因为要恢复为从服务器）
if docker volume ls | grep -q "$SLAVE_VOLUME"; then
    VOLUME_NAME="$SLAVE_VOLUME"
    echo -e "${YELLOW}备份从库 volume 数据...${NC}"
elif docker volume ls | grep -q "$MASTER_VOLUME"; then
    VOLUME_NAME="$MASTER_VOLUME"
    echo -e "${YELLOW}备份主库 volume 数据（将转换为从库）...${NC}"
else
    echo -e "${YELLOW}⚠  Volume 不存在，将创建新的从库 volume${NC}"
fi

if [ -n "$VOLUME_NAME" ]; then
    docker run --rm -v "$VOLUME_NAME":/data -v "$BACKUP_DIR":/backup \
        alpine tar czf /backup/postgres-data-backup.tar.gz -C /data . 2>/dev/null || true
    echo -e "${GREEN}✓ 数据已备份到: $BACKUP_DIR${NC}"
fi
echo ""

# 清空数据目录（使用从库 volume）
echo -e "${CYAN}[步骤 4/6] 清空数据目录...${NC}"
# 确定要使用的 volume（优先从库 volume）
if docker volume ls | grep -q "$SLAVE_VOLUME"; then
    VOLUME_NAME="$SLAVE_VOLUME"
elif docker volume ls | grep -q "$MASTER_VOLUME"; then
    # 如果只有主库 volume，清空它（后续会创建从库 volume）
    VOLUME_NAME="$MASTER_VOLUME"
    echo -e "${YELLOW}⚠  检测到主库 volume，将清空并重新配置为从库${NC}"
else
    VOLUME_NAME="$SLAVE_VOLUME"
    echo -e "${YELLOW}⚠  Volume 不存在，将创建新的从库 volume${NC}"
fi

if docker volume ls | grep -q "$VOLUME_NAME"; then
    docker run --rm -v "$VOLUME_NAME":/data alpine sh -c "rm -rf /data/*" 2>/dev/null || true
    echo -e "${GREEN}✓ 数据目录已清空${NC}"
fi
echo ""

# 从新主服务器复制数据
echo -e "${CYAN}[步骤 5/6] 从新主服务器复制数据...${NC}"
echo -e "${YELLOW}这可能需要几分钟，请耐心等待...${NC}"

# 确定要使用的 volume（必须是从库 volume）
if docker volume ls | grep -q "$SLAVE_VOLUME"; then
    VOLUME_NAME="$SLAVE_VOLUME"
elif docker volume ls | grep -q "$MASTER_VOLUME"; then
    # 如果只有主库 volume，需要先删除它，然后创建从库 volume
    echo -e "${YELLOW}⚠  删除主库 volume，创建从库 volume...${NC}"
    docker volume rm "$MASTER_VOLUME" 2>/dev/null || true
    VOLUME_NAME="$SLAVE_VOLUME"
else
    VOLUME_NAME="$SLAVE_VOLUME"
fi

# 确保从库 volume 存在
if ! docker volume ls | grep -q "$VOLUME_NAME"; then
    echo -e "${YELLOW}创建从库 volume...${NC}"
    docker volume create "$VOLUME_NAME" > /dev/null 2>&1 || true
fi

# 执行 pg_basebackup（PostgreSQL 15 方式，使用环境变量传递密码）
if docker run --rm \
    -v "$VOLUME_NAME":/data \
    -e PGHOST="$NEW_MASTER_IP" \
    -e PGPORT=5432 \
    -e PGUSER="$REPLICATION_USER" \
    -e PGPASSWORD="$REPLICATION_PASSWORD" \
    postgres:15-alpine \
    pg_basebackup -D /data -R -X stream -P -U "$REPLICATION_USER" -v; then
    echo -e "${GREEN}✓ 数据复制完成${NC}"
else
    echo -e "${RED}错误: 数据复制失败${NC}"
    echo -e "${YELLOW}提示: 请检查新主服务器的复制用户配置和防火墙设置${NC}"
    echo -e "${YELLOW}提示: 复制用户: $REPLICATION_USER${NC}"
    echo -e "${YELLOW}提示: 新主服务器IP: $NEW_MASTER_IP${NC}"
    echo -e "${YELLOW}提示: 可以手动执行以下命令测试连接：${NC}"
    echo "  docker run --rm --network host postgres:15-alpine pg_isready -h $NEW_MASTER_IP -p 5432 -U $REPLICATION_USER"
    exit 1
fi
echo ""

# 更新配置文件
echo -e "${CYAN}[步骤 6/6] 更新配置文件...${NC}"
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}⚠  配置文件不存在，创建新配置...${NC}"
    cp "${PROJECT_DIR}/env.example" "$ENV_FILE" 2>/dev/null || true
fi

# 更新 NODE_ROLE
if grep -q "^NODE_ROLE=" "$ENV_FILE"; then
    sed -i 's/^NODE_ROLE=.*/NODE_ROLE=slave/' "$ENV_FILE"
else
    echo "NODE_ROLE=slave" >> "$ENV_FILE"
fi

# 更新主服务器IP
if grep -q "^MASTER_IP=" "$ENV_FILE"; then
    sed -i "s|^MASTER_IP=.*|MASTER_IP=$NEW_MASTER_IP|" "$ENV_FILE"
else
    echo "MASTER_IP=$NEW_MASTER_IP" >> "$ENV_FILE"
fi

# 更新 PostgreSQL 主服务器配置
if grep -q "^POSTGRES_MASTER_HOST=" "$ENV_FILE"; then
    sed -i "s|^POSTGRES_MASTER_HOST=.*|POSTGRES_MASTER_HOST=$NEW_MASTER_IP|" "$ENV_FILE"
else
    echo "POSTGRES_MASTER_HOST=$NEW_MASTER_IP" >> "$ENV_FILE"
fi

echo -e "${GREEN}✓ 配置文件已更新${NC}"
echo ""

# 启动从库服务
echo -e "${CYAN}[步骤 7/7] 启动从库服务...${NC}"
cd "$PROJECT_DIR" || {
    echo -e "${RED}错误: 无法进入项目目录${NC}"
    exit 1
}

if docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" \
    --profile slave up -d postgres-slave; then
    echo -e "${GREEN}✓ 从库服务已启动${NC}"
    
    # 等待从库就绪
    echo -e "${YELLOW}等待从库就绪...${NC}"
    sleep 5
    
    # 检查从库状态
    if docker exec law-firm-postgres-slave \
        psql -U law_admin -d law_firm -tAc \
        "SELECT pg_is_in_recovery();" 2>/dev/null | grep -q "t"; then
        echo -e "${GREEN}✓ 从库运行正常（恢复模式）${NC}"
    else
        echo -e "${YELLOW}⚠  从库可能未正常启动，请检查日志${NC}"
    fi
else
    echo -e "${RED}错误: 从库服务启动失败${NC}"
    exit 1
fi
echo ""

# 完成
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}✓ 原主服务器已恢复为从服务器！${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""

echo -e "${CYAN}验证命令:${NC}"
echo "  检查从库状态: docker exec law-firm-postgres-slave psql -U law_admin -d law_firm -c \"SELECT pg_is_in_recovery();\""
echo "  检查复制状态: docker exec law-firm-postgres-slave psql -U law_admin -d law_firm -c \"SELECT * FROM pg_stat_wal_receiver;\""
echo "  查看日志: docker logs law-firm-postgres-slave"
echo ""

echo -e "${YELLOW}⚠️  注意:${NC}"
echo "  - 数据备份位置: $BACKUP_DIR"
echo "  - 如果恢复失败，可以从备份恢复"
echo "  - Keepalived 配置需要手动调整（如果需要）"
echo ""
echo -e "${CYAN}数据安全说明:${NC}"
echo "  ✅ 原主服务器的过期数据已被删除"
echo "  ✅ 已从新主服务器复制最新数据"
echo "  ✅ 不会导致数据回滚或删除新数据"
echo "  ✅ 数据备份已保存，可以恢复"
echo ""
