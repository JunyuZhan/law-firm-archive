#!/bin/bash
# =====================================================
# 主从服务器全自动部署脚本
# =====================================================
# 用法:
#   主服务器: ./deploy-master-slave.sh master [--init-db]
#   从服务器: ./deploy-master-slave.sh slave <主服务器IP> [--init-db]
#
# 选项:
#   --init-db    初始化数据库（仅主服务器需要，从服务器会自动复制）
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
COMPOSE_FILE="$DOCKER_DIR/docker-compose.master-slave.yml"

# 显示使用说明
show_usage() {
    echo -e "${CYAN}用法:${NC}"
    echo "  ./deploy-master-slave.sh master [--init-db]                    # 全自动部署主服务器"
    echo "  ./deploy-master-slave.sh slave <主服务器IP> [--init-db]       # 全自动部署从服务器"
    echo ""
    echo -e "${CYAN}选项:${NC}"
    echo "  --init-db    初始化数据库（主服务器首次部署需要）"
    echo ""
    echo -e "${CYAN}示例:${NC}"
    echo "  ./deploy-master-slave.sh master --init-db"
    echo "  ./deploy-master-slave.sh slave 192.168.50.10"
}

# 检查参数
if [ $# -lt 1 ]; then
    echo -e "${RED}错误: 必须指定角色 (master|slave)${NC}"
    show_usage
    exit 1
fi

ROLE="$1"
MASTER_IP="${2:-}"
INIT_DB=false

# 解析选项
for arg in "$@"; do
    case $arg in
        --init-db)
            INIT_DB=true
            ;;
        master|slave)
            # 忽略角色参数
            ;;
        *)
            if [[ ! "$arg" =~ ^-- ]] && [ -n "$arg" ] && [ "$arg" != "$ROLE" ]; then
                MASTER_IP="$arg"
            fi
            ;;
    esac
done

# 如果从服务器未提供主服务器IP，尝试从环境变量或 .env 文件读取
if [ "$ROLE" == "slave" ] && [ -z "$MASTER_IP" ]; then
    if [ -f "$ENV_FILE" ]; then
        MASTER_IP=$(grep "^MASTER_IP=" "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 | tr -d '"' | tr -d "'" | head -1)
    fi
    if [ -z "$MASTER_IP" ]; then
        echo -e "${RED}错误: 从服务器必须提供主服务器IP${NC}"
        echo -e "${YELLOW}用法: ./deploy-master-slave.sh slave <主服务器IP>${NC}"
        exit 1
    fi
fi

# 验证角色
if [[ ! "$ROLE" =~ ^(master|slave)$ ]]; then
    echo -e "${RED}错误: 无效的角色 '$ROLE'，必须是 master 或 slave${NC}"
    exit 1
fi

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}主从服务器全自动部署${NC}"
echo -e "${BLUE}角色: $ROLE${NC}"
if [ "$ROLE" == "slave" ]; then
    echo -e "${BLUE}主服务器IP: $MASTER_IP${NC}"
fi
echo -e "${BLUE}==========================================${NC}"
echo ""

# 检查 Docker 是否运行
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker 未运行，请先启动 Docker${NC}"
    exit 1
fi

# 检查 Docker Compose 是否可用
if ! command -v docker compose > /dev/null 2>&1 && ! command -v docker-compose > /dev/null 2>&1; then
    echo -e "${RED}错误: Docker Compose 未安装${NC}"
    exit 1
fi

# 使用 docker compose 或 docker-compose
if command -v docker compose > /dev/null 2>&1; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

# 步骤1：运行配置脚本
echo -e "${CYAN}[步骤 1/5] 运行配置脚本...${NC}"
if [ "$ROLE" == "master" ]; then
    "$SCRIPT_DIR/setup-master-slave.sh" master
else
    "$SCRIPT_DIR/setup-master-slave.sh" slave "$MASTER_IP"
fi
echo -e "${GREEN}✓ 配置完成${NC}"
echo ""

# 步骤2：等待配置完成
sleep 2

# 步骤3：启动服务（从服务器需要先检查数据）
echo -e "${CYAN}[步骤 2/5] 启动 Docker 服务...${NC}"
cd "$PROJECT_ROOT"

if [ "$ROLE" == "master" ]; then
    echo -e "${YELLOW}  启动主服务器服务...${NC}"
    $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile master up -d
    echo -e "${GREEN}✓ 服务启动完成${NC}"
else
    # 从服务器：先检查是否需要复制数据
    echo -e "${YELLOW}  检查从服务器数据...${NC}"
    
    # 检查 volume 是否存在且有数据
    VOLUME_EXISTS=$(docker volume ls 2>/dev/null | grep -q "law-firm-master-slave_postgres_slave_data" && echo "1" || echo "0")
    HAS_DATA="0"
    
    if [ "$VOLUME_EXISTS" = "1" ]; then
        # Volume 存在，检查是否有数据（启动临时容器检查）
        HAS_DATA=$(docker run --rm -v law-firm-master-slave_postgres_slave_data:/data alpine sh -c "test -d /data/base && echo 1 || echo 0" 2>/dev/null || echo "0")
    fi
    
    if [ "$HAS_DATA" = "1" ]; then
        echo -e "${GREEN}  ✓ 从库数据已存在，直接启动所有服务${NC}"
        $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile slave up -d
        echo -e "${GREEN}✓ 服务启动完成${NC}"
        SLAVE_NEEDS_DATA_COPY=false
    else
        echo -e "${YELLOW}  ⚠️  从库数据不存在，将在步骤4中从主服务器复制数据${NC}"
        echo -e "${YELLOW}  ⚠️  现在只启动不依赖数据库的服务...${NC}"
        # 只启动 redis-slave 和 minio（不启动 frontend、backend 和 postgres-slave）
        # frontend 依赖 backend，backend 依赖 postgres-slave，所以都不启动
        $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile slave up -d --no-deps redis-slave minio 2>/dev/null || true
        echo -e "${GREEN}✓ 部分服务启动完成（frontend、backend 和 postgres-slave 将在数据复制后启动）${NC}"
        SLAVE_NEEDS_DATA_COPY=true
    fi
fi
echo ""

# 步骤4：等待服务就绪
echo -e "${CYAN}[步骤 3/5] 等待服务就绪...${NC}"
if [ "$ROLE" == "master" ]; then
    echo -e "${YELLOW}  等待 PostgreSQL 主库就绪...${NC}"
    timeout=60
    while [ $timeout -gt 0 ]; do
        if docker exec law-firm-postgres-master pg_isready -U law_admin -d law_firm > /dev/null 2>&1; then
            echo -e "${GREEN}  ✓ PostgreSQL 主库已就绪${NC}"
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done
    
    if [ $timeout -le 0 ]; then
        echo -e "${RED}  ✗ PostgreSQL 主库启动超时${NC}"
        exit 1
    fi
    
    # 创建复制用户（如果不存在）
    echo -e "${YELLOW}  创建 PostgreSQL 复制用户...${NC}"
    docker exec law-firm-postgres-master psql -U law_admin -d law_firm << EOF || true
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'replicator') THEN
        CREATE USER replicator WITH REPLICATION PASSWORD 'replicator_password';
    END IF;
END
\$\$;
EOF
    echo -e "${GREEN}  ✓ 复制用户已就绪${NC}"
else
    # 从服务器：如果数据已存在，等待从库就绪；如果需要复制数据，跳过等待
    if [ "${SLAVE_NEEDS_DATA_COPY:-false}" = "false" ]; then
        echo -e "${YELLOW}  等待 PostgreSQL 从库就绪...${NC}"
        timeout=60
        while [ $timeout -gt 0 ]; do
            if docker exec law-firm-postgres-slave pg_isready -U law_admin -d law_firm > /dev/null 2>&1; then
                echo -e "${GREEN}  ✓ PostgreSQL 从库已就绪${NC}"
                break
            fi
            sleep 2
            timeout=$((timeout - 2))
        done
        
        if [ $timeout -le 0 ]; then
            echo -e "${RED}  ✗ PostgreSQL 从库启动超时${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}  ⚠️  跳过等待 PostgreSQL 从库（将在数据复制后启动）${NC}"
    fi
fi
echo ""

# 步骤5：数据库初始化或数据复制
if [ "$ROLE" == "master" ]; then
    if [ "$INIT_DB" = true ]; then
        echo -e "${CYAN}[步骤 4/5] 初始化数据库...${NC}"
        echo -e "${YELLOW}  等待数据库完全就绪...${NC}"
        sleep 5
        
        # 检查数据库是否已初始化
        DB_EXISTS=$(docker exec law-firm-postgres-master psql -U law_admin -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='law_firm'" 2>/dev/null || echo "0")
        
        if [ "$DB_EXISTS" != "1" ]; then
            echo -e "${YELLOW}  数据库不存在，开始初始化...${NC}"
            "$SCRIPT_DIR/reset-db.sh" --prod
            echo -e "${GREEN}  ✓ 数据库初始化完成${NC}"
        else
            echo -e "${YELLOW}  ⚠️  数据库已存在，跳过初始化${NC}"
        fi
    else
        echo -e "${CYAN}[步骤 4/5] 跳过数据库初始化（未指定 --init-db）${NC}"
    fi
else
    echo -e "${CYAN}[步骤 4/5] 从主服务器复制数据...${NC}"
    
    # 检查主服务器连接
    echo -e "${YELLOW}  检查主服务器连接...${NC}"
    if ! ping -c 1 -W 2 "$MASTER_IP" > /dev/null 2>&1; then
        echo -e "${RED}  ✗ 无法连接到主服务器 $MASTER_IP${NC}"
        echo -e "${YELLOW}  ⚠️  请检查网络连接，稍后可以手动执行数据复制${NC}"
        echo -e "${YELLOW}  ⚠️  现在启动 PostgreSQL 从库、backend 和 frontend（可能无法连接主库）${NC}"
        $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile slave up -d postgres-slave backend frontend
    else
        echo -e "${GREEN}  ✓ 主服务器连接正常${NC}"
        
        # 检查从库 volume 是否有数据
        VOLUME_EXISTS=$(docker volume ls | grep -q "law-firm-master-slave_postgres_slave_data" && echo "1" || echo "0")
        HAS_DATA="0"
        
        if [ "$VOLUME_EXISTS" = "1" ]; then
            # 检查 volume 中是否有数据文件
            HAS_DATA=$(docker run --rm -v law-firm-master-slave_postgres_slave_data:/data alpine sh -c "test -d /data/base && echo 1 || echo 0" 2>/dev/null || echo "0")
        fi
        
        if [ "$HAS_DATA" != "1" ]; then
            echo -e "${YELLOW}  从库数据不存在，开始从主服务器复制...${NC}"
            
            # 确保从库未运行
            docker stop law-firm-postgres-slave 2>/dev/null || true
            sleep 2
            
            # 等待主服务器 PostgreSQL 就绪
            echo -e "${YELLOW}  等待主服务器 PostgreSQL 就绪...${NC}"
            timeout=60
            while [ $timeout -gt 0 ]; do
                if docker run --rm --network host postgres:15-alpine pg_isready -h "$MASTER_IP" -p 5432 -U replicator > /dev/null 2>&1; then
                    echo -e "${GREEN}  ✓ 主服务器 PostgreSQL 已就绪${NC}"
                    break
                fi
                sleep 2
                timeout=$((timeout - 2))
            done
            
            if [ $timeout -le 0 ]; then
                echo -e "${RED}  ✗ 主服务器 PostgreSQL 启动超时${NC}"
                echo -e "${YELLOW}  ⚠️  请确保主服务器已启动并配置好复制用户${NC}"
                echo -e "${YELLOW}  ⚠️  现在启动从库、backend 和 frontend（可能无法连接主库）${NC}"
                $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile slave up -d postgres-slave backend frontend
            else
                # 从主服务器复制数据
                echo -e "${YELLOW}  执行 pg_basebackup（这可能需要几分钟）...${NC}"
                if docker run --rm \
                    -v law-firm-master-slave_postgres_slave_data:/data \
                    -e PGHOST="$MASTER_IP" \
                    -e PGPORT=5432 \
                    -e PGUSER=replicator \
                    -e PGPASSWORD=replicator_password \
                    postgres:15-alpine \
                    pg_basebackup -D /data -R -X stream -P -U replicator -v; then
                    echo -e "${GREEN}  ✓ 数据复制完成${NC}"
                    
                    # 启动从库、backend 和 frontend（按依赖顺序）
                    echo -e "${YELLOW}  启动 PostgreSQL 从库、backend 和 frontend...${NC}"
                    $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile slave up -d postgres-slave backend frontend
                    
                    # 等待从库就绪
                    timeout=60
                    while [ $timeout -gt 0 ]; do
                        if docker exec law-firm-postgres-slave pg_isready -U law_admin -d law_firm > /dev/null 2>&1; then
                            echo -e "${GREEN}  ✓ 从库已就绪并开始复制${NC}"
                            break
                        fi
                        sleep 2
                        timeout=$((timeout - 2))
                    done
                else
                    echo -e "${RED}  ✗ 数据复制失败${NC}"
                    echo -e "${YELLOW}  ⚠️  请检查主服务器是否已启动并配置好复制用户${NC}"
                    echo -e "${YELLOW}  ⚠️  现在启动从库、backend 和 frontend（可能无法连接主库）${NC}"
                    $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile slave up -d postgres-slave backend frontend
                fi
            fi
        else
            echo -e "${YELLOW}  ⚠️  从库数据已存在，启动从库、backend 和 frontend 服务...${NC}"
            # 如果数据已存在，启动从库、backend 和 frontend
            $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile slave up -d postgres-slave backend frontend
            
            # 等待从库就绪
            timeout=60
            while [ $timeout -gt 0 ]; do
                if docker exec law-firm-postgres-slave pg_isready -U law_admin -d law_firm > /dev/null 2>&1; then
                    echo -e "${GREEN}  ✓ 从库已就绪${NC}"
                    break
                fi
                sleep 2
                timeout=$((timeout - 2))
            done
        fi
    fi
fi
echo ""

# 步骤6：等待所有服务就绪
echo -e "${CYAN}[步骤 5/5] 检查所有服务状态...${NC}"
sleep 5

echo -e "${YELLOW}  服务状态：${NC}"
if [ "$ROLE" == "master" ]; then
    $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile master ps
else
    $DOCKER_COMPOSE --env-file "$ENV_FILE" -f "$COMPOSE_FILE" --profile slave ps
fi
echo ""

# 完成
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}✓ 部署完成！${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""

if [ "$ROLE" == "master" ]; then
    echo -e "${CYAN}主服务器部署完成！${NC}"
    echo ""
    echo -e "${CYAN}下一步：${NC}"
    echo "  1. 检查服务状态: docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile master ps"
    echo "  2. 查看日志: docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile master logs -f"
    echo "  3. 部署从服务器: ./scripts/ops/deploy-master-slave.sh slave <主服务器IP>"
else
    echo -e "${CYAN}从服务器部署完成！${NC}"
    echo ""
    echo -e "${CYAN}下一步：${NC}"
    echo "  1. 检查服务状态: docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile slave ps"
    echo "  2. 查看日志: docker compose --env-file .env -f docker/docker-compose.master-slave.yml --profile slave logs -f"
    echo "  3. 检查主从复制状态: docker exec law-firm-postgres-slave psql -U law_admin -d law_firm -c \"SELECT * FROM pg_stat_wal_receiver;\""
fi
echo ""
