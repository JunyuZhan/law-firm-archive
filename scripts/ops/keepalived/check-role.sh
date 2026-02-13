#!/bin/bash
# =====================================================
# 检查当前服务器的主从角色
# =====================================================
# 用途：快速识别当前服务器是主服务器还是从服务器
# =====================================================

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}==========================================${NC}"
echo -e "${CYAN}主从角色检查${NC}"
echo -e "${CYAN}==========================================${NC}"
echo ""

# 1. 检查 Keepalived 状态
echo -e "${BLUE}[1] Keepalived 状态：${NC}"
if systemctl is-active --quiet keepalived 2>/dev/null; then
    KEEPALIVED_STATE=$(systemctl show keepalived --property=ActiveState --value)
    if [ "$KEEPALIVED_STATE" = "active" ]; then
        # 从 Keepalived 配置文件读取虚拟IP
        VIP=""
        if [ -f /etc/keepalived/keepalived.conf ]; then
            VIP=$(grep -A 1 "virtual_ipaddress" /etc/keepalived/keepalived.conf 2>/dev/null | \
                grep -oE '([0-9]{1,3}\.){3}[0-9]{1,3}' | head -1 || echo "")
        fi
        
        # 检查虚拟IP是否绑定（如果配置了虚拟IP）
        if [ -n "$VIP" ]; then
            if ip addr show 2>/dev/null | grep -q "$VIP"; then
                echo -e "  ${GREEN}✓ Keepalived 运行中，虚拟IP已绑定: $VIP${NC}"
                echo -e "  ${GREEN}  角色: Keepalived MASTER（当前主服务器）${NC}"
                KEEPALIVED_ROLE="MASTER"
            else
                echo -e "  ${YELLOW}✓ Keepalived 运行中，虚拟IP未绑定: $VIP${NC}"
                echo -e "  ${YELLOW}  角色: Keepalived BACKUP（当前从服务器）${NC}"
                KEEPALIVED_ROLE="BACKUP"
            fi
        else
            # 如果没有配置虚拟IP，检查是否有任何 secondary IP（可能是虚拟IP）
            SECONDARY_IPS=$(ip addr show 2>/dev/null | grep "secondary" | grep -oE 'inet ([0-9]{1,3}\.){3}[0-9]{1,3}' | awk '{print $2}' || echo "")
            if [ -n "$SECONDARY_IPS" ]; then
                echo -e "  ${GREEN}✓ Keepalived 运行中，检测到虚拟IP: $SECONDARY_IPS${NC}"
                echo -e "  ${GREEN}  角色: Keepalived MASTER（当前主服务器）${NC}"
                KEEPALIVED_ROLE="MASTER"
            else
                echo -e "  ${YELLOW}✓ Keepalived 运行中，虚拟IP未绑定${NC}"
                echo -e "  ${YELLOW}  角色: Keepalived BACKUP（当前从服务器）${NC}"
                KEEPALIVED_ROLE="BACKUP"
            fi
        fi
    else
        echo -e "  ${RED}✗ Keepalived 未运行${NC}"
        KEEPALIVED_ROLE="UNKNOWN"
    fi
else
    echo -e "  ${RED}✗ Keepalived 未安装或未运行${NC}"
    KEEPALIVED_ROLE="UNKNOWN"
fi
echo ""

# 2. 检查 PostgreSQL 容器
echo -e "${BLUE}[2] PostgreSQL 容器状态：${NC}"
MASTER_CONTAINER=$(docker ps --format "{{.Names}}" | grep "^law-firm-postgres-master$" || echo "")
SLAVE_CONTAINER=$(docker ps --format "{{.Names}}" | grep "^law-firm-postgres-slave$" || echo "")

if [ -n "$MASTER_CONTAINER" ]; then
    echo -e "  ${GREEN}✓ 主库容器运行中: $MASTER_CONTAINER${NC}"
    MASTER_EXISTS=true
else
    echo -e "  ${YELLOW}⚠ 主库容器未运行${NC}"
    MASTER_EXISTS=false
fi

if [ -n "$SLAVE_CONTAINER" ]; then
    echo -e "  ${GREEN}✓ 从库容器运行中: $SLAVE_CONTAINER${NC}"
    SLAVE_EXISTS=true
else
    echo -e "  ${YELLOW}⚠ 从库容器未运行${NC}"
    SLAVE_EXISTS=false
fi
echo ""

# 3. 检查 PostgreSQL 角色
echo -e "${BLUE}[3] PostgreSQL 数据库角色：${NC}"
if [ "$MASTER_EXISTS" = "true" ]; then
    IN_RECOVERY=$(docker exec "$MASTER_CONTAINER" \
        psql -U law_admin -d law_firm -tAc \
        "SELECT pg_is_in_recovery();" 2>/dev/null || echo "unknown")
    
    if [ "$IN_RECOVERY" = "f" ] || [ "$IN_RECOVERY" = "false" ]; then
        echo -e "  ${GREEN}✓ 主库容器是主库（可写）${NC}"
        PG_ROLE_MASTER="MASTER"
    else
        echo -e "  ${YELLOW}⚠ 主库容器在恢复模式（只读）${NC}"
        PG_ROLE_MASTER="SLAVE"
    fi
fi

if [ "$SLAVE_EXISTS" = "true" ]; then
    IN_RECOVERY=$(docker exec "$SLAVE_CONTAINER" \
        psql -U law_admin -d law_firm -tAc \
        "SELECT pg_is_in_recovery();" 2>/dev/null || echo "unknown")
    
    if [ "$IN_RECOVERY" = "f" ] || [ "$IN_RECOVERY" = "false" ]; then
        echo -e "  ${GREEN}✓ 从库容器已提升为主库（可写）${NC}"
        PG_ROLE_SLAVE="MASTER"
    else
        echo -e "  ${GREEN}✓ 从库容器是从库（只读）${NC}"
        PG_ROLE_SLAVE="SLAVE"
    fi
fi
echo ""

# 4. 检查配置文件
echo -e "${BLUE}[4] 配置文件角色：${NC}"
PROJECT_DIR="${LAW_FIRM_PROJECT_DIR:-/opt/law-firm}"
ENV_FILE="${PROJECT_DIR}/.env"

if [ -f "$ENV_FILE" ]; then
    NODE_ROLE=$(grep "^NODE_ROLE=" "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 | tr -d '"' | tr -d "'" | head -1)
    if [ -n "$NODE_ROLE" ]; then
        echo -e "  ${GREEN}✓ 配置角色: $NODE_ROLE${NC}"
        CONFIG_ROLE="$NODE_ROLE"
    else
        echo -e "  ${YELLOW}⚠ 配置文件中未设置 NODE_ROLE${NC}"
        CONFIG_ROLE="UNKNOWN"
    fi
else
    echo -e "  ${YELLOW}⚠ 配置文件不存在: $ENV_FILE${NC}"
    CONFIG_ROLE="UNKNOWN"
fi
echo ""

# 5. 综合判断
echo -e "${CYAN}==========================================${NC}"
echo -e "${CYAN}综合判断${NC}"
echo -e "${CYAN}==========================================${NC}"
echo ""

# 判断逻辑
if [ "$KEEPALIVED_ROLE" = "MASTER" ]; then
    echo -e "${GREEN}✓ 当前服务器是主服务器（Keepalived MASTER + 虚拟IP）${NC}"
    CURRENT_ROLE="MASTER"
elif [ "$SLAVE_EXISTS" = "true" ] && [ "$PG_ROLE_SLAVE" = "MASTER" ]; then
    echo -e "${GREEN}✓ 当前服务器是主服务器（从库已提升为主库）${NC}"
    CURRENT_ROLE="MASTER"
elif [ "$MASTER_EXISTS" = "true" ] && [ "$PG_ROLE_MASTER" = "MASTER" ]; then
    echo -e "${GREEN}✓ 当前服务器是主服务器（主库容器运行中）${NC}"
    CURRENT_ROLE="MASTER"
elif [ "$CONFIG_ROLE" = "master" ]; then
    echo -e "${YELLOW}⚠ 配置显示为主服务器，但需要验证实际状态${NC}"
    CURRENT_ROLE="MASTER"
elif [ "$KEEPALIVED_ROLE" = "BACKUP" ]; then
    echo -e "${YELLOW}⚠ 当前服务器是从服务器（Keepalived BACKUP）${NC}"
    CURRENT_ROLE="SLAVE"
elif [ "$SLAVE_EXISTS" = "true" ] && [ "$PG_ROLE_SLAVE" = "SLAVE" ]; then
    echo -e "${YELLOW}⚠ 当前服务器是从服务器（从库容器运行中）${NC}"
    CURRENT_ROLE="SLAVE"
elif [ "$CONFIG_ROLE" = "slave" ]; then
    echo -e "${YELLOW}⚠ 配置显示为从服务器，但需要验证实际状态${NC}"
    CURRENT_ROLE="SLAVE"
else
    echo -e "${RED}✗ 无法确定角色，请检查服务状态${NC}"
    CURRENT_ROLE="UNKNOWN"
fi
echo ""

# 6. 显示详细信息
if [ "$CURRENT_ROLE" = "MASTER" ]; then
    echo -e "${CYAN}详细信息：${NC}"
    echo "  - Keepalived 角色: ${KEEPALIVED_ROLE:-UNKNOWN}"
    echo "  - PostgreSQL 角色: 主库（可写）"
    echo "  - 配置角色: ${CONFIG_ROLE:-UNKNOWN}"
    echo ""
    echo -e "${YELLOW}提示：${NC}"
    echo "  - 如果这是故障切换后的主服务器，原主服务器恢复后需要重新配置为从服务器"
    echo "  - 使用脚本恢复原主服务器: ./restore-original-master.sh"
elif [ "$CURRENT_ROLE" = "SLAVE" ]; then
    echo -e "${CYAN}详细信息：${NC}"
    echo "  - Keepalived 角色: ${KEEPALIVED_ROLE:-UNKNOWN}"
    echo "  - PostgreSQL 角色: 从库（只读）"
    echo "  - 配置角色: ${CONFIG_ROLE:-UNKNOWN}"
    echo ""
    echo -e "${YELLOW}提示：${NC}"
    echo "  - 如果主服务器故障，此服务器会自动接管（如果配置了 Keepalived）"
fi

echo ""
