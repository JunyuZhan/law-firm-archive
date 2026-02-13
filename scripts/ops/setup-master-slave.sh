#!/bin/bash
# =====================================================
# 主从服务器部署配置脚本
# =====================================================
# 用法:
#   主服务器: ./setup-master-slave.sh master
#   从服务器: ./setup-master-slave.sh slave <主服务器IP>
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
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_DIR="$PROJECT_ROOT/docker"
ENV_FILE="$PROJECT_ROOT/.env"

# 显示使用说明
show_usage() {
    echo -e "${CYAN}用法:${NC}"
    echo "  ./setup-master-slave.sh master                    # 配置主服务器"
    echo "  ./setup-master-slave.sh slave <主服务器IP>       # 配置从服务器"
    echo ""
    echo -e "${CYAN}示例:${NC}"
    echo "  ./setup-master-slave.sh master"
    echo "  ./setup-master-slave.sh slave <主服务器IP>"
    echo ""
    echo -e "${CYAN}说明:${NC}"
    echo "  - 主服务器IP可以通过参数提供，或在 .env 文件中设置 MASTER_IP"
    echo "  - 从服务器IP需要在 .env 文件中设置 SLAVE_IP"
}

# 检查参数
if [ $# -lt 1 ]; then
    echo -e "${RED}错误: 必须指定角色 (master|slave)${NC}"
    show_usage
    exit 1
fi

ROLE="$1"
MASTER_IP="${2:-}"

# 如果从服务器未提供主服务器IP，尝试从环境变量或 .env 文件读取
if [ "$ROLE" == "slave" ] && [ -z "$MASTER_IP" ]; then
    if [ -f "$ENV_FILE" ]; then
        MASTER_IP=$(grep "^MASTER_IP=" "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 | tr -d '"' | tr -d "'" | head -1)
    fi
    if [ -z "$MASTER_IP" ]; then
        echo -e "${RED}错误: 从服务器必须提供主服务器IP${NC}"
        echo -e "${YELLOW}用法: ./setup-master-slave.sh slave <主服务器IP>${NC}"
        echo -e "${YELLOW}或在 .env 文件中设置 MASTER_IP${NC}"
        exit 1
    fi
fi

# 验证角色
if [[ ! "$ROLE" =~ ^(master|slave)$ ]]; then
    echo -e "${RED}错误: 无效的角色 '$ROLE'，必须是 master 或 slave${NC}"
    exit 1
fi

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}主从服务器配置脚本${NC}"
echo -e "${BLUE}角色: $ROLE${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

# 检查环境变量文件（项目根目录）
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}警告: 未找到 .env 文件，正在从模板创建...${NC}"
    if [ -f "$PROJECT_ROOT/env.example" ]; then
        cp "$PROJECT_ROOT/env.example" "$ENV_FILE"
    elif [ -f "$DOCKER_DIR/env.example" ]; then
        cp "$DOCKER_DIR/env.example" "$ENV_FILE"
    else
        echo -e "${RED}错误: 未找到 env.example 模板文件${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ 已创建 .env 文件，请编辑设置密码${NC}"
fi

if [ "$ROLE" == "master" ]; then
    echo -e "${CYAN}配置主服务器...${NC}"
    
    # 1. 创建 PostgreSQL 主库配置目录
    echo -e "${YELLOW}[1/4] 创建 PostgreSQL 主库配置...${NC}"
    mkdir -p "$DOCKER_DIR/postgres/master"
    
    # 2. 更新 pg_hba.conf（如果不存在）
    if [ ! -f "$DOCKER_DIR/postgres/master/pg_hba.conf" ]; then
        echo -e "${YELLOW}  创建 pg_hba.conf...${NC}"
        # 尝试从 .env 读取从服务器IP
        SLAVE_IP_FOR_HBA=""
        if [ -f "$ENV_FILE" ]; then
            SLAVE_IP_FOR_HBA=$(grep "^SLAVE_IP=" "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 | tr -d '"' | tr -d "'" | head -1)
        fi
        # 如果未找到，使用默认网段
        if [ -z "$SLAVE_IP_FOR_HBA" ]; then
            SLAVE_IP_FOR_HBA="0.0.0.0/0"  # 允许所有IP，用户需要根据实际情况修改
        fi
        
        cat > "$DOCKER_DIR/postgres/master/pg_hba.conf" << EOF
# PostgreSQL 主库访问控制配置
local   all             all                                     trust
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust
# 允许从服务器复制（请根据实际IP修改）
host    replication     replicator      ${SLAVE_IP_FOR_HBA}     md5
# 允许应用连接（请根据实际网络修改）
host    all             law_admin       0.0.0.0/0               md5
EOF
        echo -e "${GREEN}  ✓ 已创建 pg_hba.conf${NC}"
        echo -e "${YELLOW}  ⚠️  请根据实际网络修改 IP 地址（当前允许所有IP，建议限制）${NC}"
    fi
    
    # 3. 创建复制用户（如果 PostgreSQL 已运行）
    if docker ps --format '{{.Names}}' | grep -q "law-firm-postgres-master"; then
        echo -e "${YELLOW}[2/4] 创建 PostgreSQL 复制用户...${NC}"
        docker exec -it law-firm-postgres-master psql -U law_admin -d law_firm << EOF || true
CREATE USER replicator WITH REPLICATION PASSWORD 'replicator_password';
\q
EOF
        echo -e "${GREEN}  ✓ 复制用户已创建${NC}"
    else
        echo -e "${YELLOW}[2/4] PostgreSQL 未运行，跳过复制用户创建${NC}"
    fi
    
    # 4. 设置环境变量
    echo -e "${YELLOW}[3/4] 设置环境变量...${NC}"
    if ! grep -q "NODE_ROLE=master" "$ENV_FILE"; then
        CURRENT_IP=$(hostname -I | awk '{print $1}')
        echo "" >> "$ENV_FILE"
        echo "# 主从服务器配置" >> "$ENV_FILE"
        echo "NODE_ROLE=master" >> "$ENV_FILE"
        echo "MASTER_IP=${CURRENT_IP}" >> "$ENV_FILE"
        echo "# SLAVE_IP=请设置从服务器IP地址" >> "$ENV_FILE"
        echo "POSTGRES_REPLICATION_USER=replicator" >> "$ENV_FILE"
        echo "POSTGRES_REPLICATION_PASSWORD=replicator_password" >> "$ENV_FILE"
        echo -e "${GREEN}  ✓ 环境变量已设置（MASTER_IP=${CURRENT_IP}）${NC}"
        echo -e "${YELLOW}  ⚠️  请在 .env 文件中设置 SLAVE_IP${NC}"
    else
        echo -e "${YELLOW}  ⚠️  环境变量已存在，跳过${NC}"
    fi
    
    # 5. 提示下一步
    echo -e "${YELLOW}[4/4] 配置完成${NC}"
    echo ""
    echo -e "${GREEN}✓ 主服务器配置完成！${NC}"
    echo ""
    echo -e "${CYAN}下一步：${NC}"
    echo "  1. 编辑 $ENV_FILE 设置密码"
    echo "  2. 编辑 $DOCKER_DIR/postgres/master/pg_hba.conf 设置从服务器IP"
    echo "  3. 启动服务: docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile master up -d"
    
elif [ "$ROLE" == "slave" ]; then
    echo -e "${CYAN}配置从服务器...${NC}"
    
    # 1. 创建 PostgreSQL 从库配置目录
    echo -e "${YELLOW}[1/5] 创建 PostgreSQL 从库配置...${NC}"
    mkdir -p "$DOCKER_DIR/postgres/slave"
    
    # 2. PostgreSQL 15 不再使用 recovery.conf，配置通过环境变量传递
    echo -e "${YELLOW}[2/5] PostgreSQL 15 配置说明...${NC}"
    echo -e "${GREEN}  ✓ PostgreSQL 15 使用 primary_conninfo 参数（在 docker-compose 中配置）${NC}"
    
    # 3. 设置环境变量
    echo -e "${YELLOW}[3/5] 设置环境变量...${NC}"
    if ! grep -q "NODE_ROLE=slave" "$ENV_FILE"; then
        echo "" >> "$ENV_FILE"
        echo "# 主从服务器配置" >> "$ENV_FILE"
        echo "NODE_ROLE=slave" >> "$ENV_FILE"
        echo "MASTER_IP=$MASTER_IP" >> "$ENV_FILE"
        echo "SLAVE_IP=$(hostname -I | awk '{print $1}')" >> "$ENV_FILE"
        echo "POSTGRES_MASTER_HOST=$MASTER_IP" >> "$ENV_FILE"
        echo "POSTGRES_MASTER_PORT=5432" >> "$ENV_FILE"
        echo "POSTGRES_REPLICATION_USER=replicator" >> "$ENV_FILE"
        echo "POSTGRES_REPLICATION_PASSWORD=replicator_password" >> "$ENV_FILE"
        echo "REDIS_MASTER_HOST=$MASTER_IP" >> "$ENV_FILE"
        echo "REDIS_MASTER_PORT=6379" >> "$ENV_FILE"
        echo -e "${GREEN}  ✓ 环境变量已设置${NC}"
    else
        echo -e "${YELLOW}  ⚠️  环境变量已存在，跳过${NC}"
    fi
    
    # 4. 测试主服务器连接
    echo -e "${YELLOW}[4/5] 测试主服务器连接...${NC}"
    if ping -c 1 -W 2 "$MASTER_IP" > /dev/null 2>&1; then
        echo -e "${GREEN}  ✓ 主服务器连接正常${NC}"
    else
        echo -e "${RED}  ✗ 无法连接到主服务器 $MASTER_IP${NC}"
        echo -e "${YELLOW}  ⚠️  请检查网络连接和防火墙设置${NC}"
    fi
    
    # 5. 提示下一步
    echo -e "${YELLOW}[5/5] 配置完成${NC}"
    echo ""
    echo -e "${GREEN}✓ 从服务器配置完成！${NC}"
    echo ""
    echo -e "${CYAN}下一步：${NC}"
    echo "  1. 确保主服务器已启动并配置好复制用户"
    echo "  2. 编辑 $ENV_FILE 设置密码（与主服务器一致）"
    echo "  3. 启动服务: docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile slave up -d"
fi

echo ""
echo -e "${BLUE}==========================================${NC}"
echo -e "${GREEN}配置完成！${NC}"
echo -e "${BLUE}==========================================${NC}"
