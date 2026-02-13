#!/bin/bash
# =====================================================
# PostgreSQL 健康检查脚本（Keepalived 使用）
# =====================================================
# 用途：检查 PostgreSQL 容器是否健康
# 返回值：
#   0 = 健康
#   1 = 不健康
# =====================================================

LOG_FILE="/var/log/keepalived-postgres-check.log"
TIMEOUT=5  # 超时时间（秒）

# 日志函数
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [CHECK] $1" >> "$LOG_FILE"
}

# 检查 Docker 是否运行（通用方法）
if ! docker info > /dev/null 2>&1; then
    log "ERROR: Docker is not running or not accessible"
    exit 1
fi

# 判断是主服务器还是从服务器
# 方法1：检查容器是否存在（优先）
# 方法2：检查环境变量或配置文件
# 方法3：检查主机名（最后备选）

CONTAINER_NAME=""
ROLE=""

# 优先检查容器是否存在
if docker ps --format "{{.Names}}" | grep -q "^law-firm-postgres-master$"; then
    CONTAINER_NAME="law-firm-postgres-master"
    ROLE="master"
elif docker ps --format "{{.Names}}" | grep -q "^law-firm-postgres-slave$"; then
    CONTAINER_NAME="law-firm-postgres-slave"
    ROLE="slave"
else
    # 容器都不存在，尝试从环境变量或配置文件判断
    # 支持环境变量配置项目目录，默认 /opt/law-firm
    PROJECT_DIR="${LAW_FIRM_PROJECT_DIR:-/opt/law-firm}"
    ENV_FILE="${PROJECT_DIR}/.env"
    
    if [ -f "$ENV_FILE" ]; then
        NODE_ROLE=$(grep "^NODE_ROLE=" "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 | tr -d '"' | tr -d "'" | head -1)
        if [ "$NODE_ROLE" = "master" ]; then
            CONTAINER_NAME="law-firm-postgres-master"
            ROLE="master"
        elif [ "$NODE_ROLE" = "slave" ]; then
            CONTAINER_NAME="law-firm-postgres-slave"
            ROLE="slave"
        fi
    fi
    
    # 如果还是无法判断，使用主机名（最后备选）
    if [ -z "$CONTAINER_NAME" ]; then
        HOSTNAME=$(hostname)
        if [[ "$HOSTNAME" == *"master"* ]] || [[ "$HOSTNAME" == *"主"* ]]; then
            CONTAINER_NAME="law-firm-postgres-master"
            ROLE="master"
        else
            CONTAINER_NAME="law-firm-postgres-slave"
            ROLE="slave"
        fi
    fi
fi

# 如果还是无法确定，报错
if [ -z "$CONTAINER_NAME" ]; then
    log "ERROR: Cannot determine server role (master/slave)"
    exit 1
fi

log "INFO: Detected role: ${ROLE}, container: ${CONTAINER_NAME}"

# 检查容器是否存在
if ! docker ps --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
    log "ERROR: Container ${CONTAINER_NAME} is not running"
    exit 1
fi

# 检查容器状态（必须是 running）
CONTAINER_STATUS=$(docker inspect --format='{{.State.Status}}' "${CONTAINER_NAME}" 2>/dev/null)
if [ "$CONTAINER_STATUS" != "running" ]; then
    log "ERROR: Container ${CONTAINER_NAME} status is ${CONTAINER_STATUS}"
    exit 1
fi

# 检查 PostgreSQL 是否可连接（带超时）
if ! timeout "$TIMEOUT" docker exec "${CONTAINER_NAME}" pg_isready -U law_admin -d law_firm > /dev/null 2>&1; then
    log "ERROR: PostgreSQL in ${CONTAINER_NAME} is not ready"
    exit 1
fi

# 主服务器：检查主从复制状态（是否有从库连接）
if [ "$ROLE" = "master" ]; then
    REPLICATION_COUNT=$(timeout "$TIMEOUT" docker exec "${CONTAINER_NAME}" \
        psql -U law_admin -d law_firm -tAc \
        "SELECT COUNT(*) FROM pg_stat_replication;" 2>/dev/null || echo "0")
    
    # 主库可以没有从库（从库可能故障），所以只要有主库健康就 OK
    log "INFO: Master PostgreSQL is healthy (replication connections: ${REPLICATION_COUNT})"
    exit 0
fi

# 从服务器：检查复制延迟（可选，如果延迟太大可能有问题）
if [ "$ROLE" = "slave" ]; then
    # 检查是否还在恢复模式（从库应该处于恢复模式）
    IN_RECOVERY=$(timeout "$TIMEOUT" docker exec "${CONTAINER_NAME}" \
        psql -U law_admin -d law_firm -tAc \
        "SELECT pg_is_in_recovery();" 2>/dev/null || echo "unknown")
    
    if [ "$IN_RECOVERY" != "t" ] && [ "$IN_RECOVERY" != "true" ]; then
        # 从库不在恢复模式，可能已经提升为主库了
        log "WARN: Slave PostgreSQL is not in recovery mode (may have been promoted)"
    fi
    
    # 检查复制延迟（可选）
    REPLICATION_LAG=$(timeout "$TIMEOUT" docker exec "${CONTAINER_NAME}" \
        psql -U law_admin -d law_firm -tAc \
        "SELECT EXTRACT(EPOCH FROM (NOW() - pg_last_xact_replay_timestamp()))::int;" 2>/dev/null || echo "999999")
    
    # 如果延迟超过 60 秒，认为有问题（但不算致命，因为可能是网络问题）
    if [ "$REPLICATION_LAG" -gt 60 ]; then
        log "WARN: Replication lag is ${REPLICATION_LAG} seconds (high but not fatal)"
    fi
    
    log "INFO: Slave PostgreSQL is healthy (recovery: ${IN_RECOVERY}, lag: ${REPLICATION_LAG}s)"
    exit 0
fi

# 默认返回健康
log "INFO: PostgreSQL check passed"
exit 0
