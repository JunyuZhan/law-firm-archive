#!/bin/bash
# =====================================================
# Keepalived 切换脚本：当服务器成为 Master 时执行
# =====================================================
# 用途：自动提升从库为主库，更新配置，重启服务
# 调用时机：Keepalived 检测到故障，切换虚拟IP到本服务器时
# =====================================================

set -euo pipefail  # 严格模式：遇到错误立即退出

LOG_FILE="/var/log/postgres-failover.log"
PROJECT_DIR="${LAW_FIRM_PROJECT_DIR:-/opt/law-firm}"  # 项目目录，可通过环境变量配置
COMPOSE_FILE="${PROJECT_DIR}/docker/docker-compose.master-slave.yml"
ENV_FILE="${PROJECT_DIR}/.env"
MAX_RETRIES=3
RETRY_DELAY=5

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${timestamp} [${level}] ${message}" | tee -a "$LOG_FILE"
}

log_info() {
    log "INFO" "$@"
}

log_warn() {
    log "WARN" "$@"
}

log_error() {
    log "ERROR" "$@"
}

# 检查函数是否成功
check_result() {
    local cmd="$1"
    local description="$2"
    if eval "$cmd"; then
        log_info "${description} ✓"
        return 0
    else
        log_error "${description} ✗"
        return 1
    fi
}

# 等待 PostgreSQL 就绪
wait_postgres_ready() {
    local container_name="$1"
    local max_wait="${2:-30}"
    local waited=0
    
    log_info "Waiting for PostgreSQL ${container_name} to be ready..."
    while [ $waited -lt $max_wait ]; do
        if docker exec "$container_name" pg_isready -U law_admin -d law_firm > /dev/null 2>&1; then
            log_info "PostgreSQL ${container_name} is ready"
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
    done
    
    log_error "PostgreSQL ${container_name} not ready after ${max_wait} seconds"
    return 1
}

# 检查当前角色
check_current_role() {
    # 先检查从库容器是否存在
    local slave_container="law-firm-postgres-slave"
    local master_container="law-firm-postgres-master"
    
    # 如果从库容器不存在，检查主库容器
    if ! docker ps --format "{{.Names}}" | grep -q "^${slave_container}$"; then
        if docker ps --format "{{.Names}}" | grep -q "^${master_container}$"; then
            log_info "No slave container found, master container exists - already master"
            return 1  # 返回1表示已经是主库
        else
            log_warn "Neither slave nor master container found, may need manual intervention"
            return 1
        fi
    fi
    
    # 从库容器存在，检查是否在恢复模式
    local in_recovery=$(docker exec "$slave_container" \
        psql -U law_admin -d law_firm -tAc \
        "SELECT pg_is_in_recovery();" 2>/dev/null || echo "unknown")
    
    if [ "$in_recovery" != "t" ] && [ "$in_recovery" != "true" ]; then
        log_info "PostgreSQL is not in recovery mode, already promoted"
        return 1  # 返回1表示已经提升过了
    fi
    
    return 0  # 返回0表示需要提升
}

# 提升从库为主库
promote_slave() {
    local container_name="law-firm-postgres-slave"
    local retries=0
    
    # 先检查容器是否存在
    if ! docker ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
        log_error "Slave container ${container_name} not found, cannot promote"
        return 1
    fi
    
    log_info "Promoting slave PostgreSQL to master..."
    
    while [ $retries -lt $MAX_RETRIES ]; do
        if docker exec "$container_name" \
            psql -U law_admin -d law_firm -c "SELECT pg_promote();" >> "$LOG_FILE" 2>&1; then
            log_info "PostgreSQL promoted successfully"
            
            # 等待提升完成
            sleep 3
            
            # 验证提升是否成功
            local in_recovery=$(docker exec "$container_name" \
                psql -U law_admin -d law_firm -tAc \
                "SELECT pg_is_in_recovery();" 2>/dev/null || echo "unknown")
            
            if [ "$in_recovery" = "f" ] || [ "$in_recovery" = "false" ]; then
                log_info "Promotion verified: PostgreSQL is now master"
                return 0
            else
                log_warn "Promotion may not be complete, still in recovery mode"
            fi
        else
            retries=$((retries + 1))
            log_warn "Promotion failed, retry ${retries}/${MAX_RETRIES}..."
            sleep $RETRY_DELAY
        fi
    done
    
    log_error "Failed to promote PostgreSQL after ${MAX_RETRIES} retries"
    return 1
}

# 更新配置文件
update_config() {
    if [ ! -f "$ENV_FILE" ]; then
        log_error "Environment file not found: ${ENV_FILE}"
        return 1
    fi
    
    log_info "Updating configuration file: ${ENV_FILE}"
    
    # 备份原配置
    cp "$ENV_FILE" "${ENV_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
    
    # 更新 NODE_ROLE
    if grep -q "^NODE_ROLE=" "$ENV_FILE"; then
        sed -i 's/^NODE_ROLE=.*/NODE_ROLE=master/' "$ENV_FILE"
    else
        echo "NODE_ROLE=master" >> "$ENV_FILE"
    fi
    
    # 更新 DB_HOST（如果需要）
    if grep -q "^DB_HOST=" "$ENV_FILE"; then
        sed -i 's/^DB_HOST=.*/DB_HOST=postgres-master/' "$ENV_FILE"
    fi
    
    log_info "Configuration updated"
    return 0
}

# 重启应用服务
restart_services() {
    if [ ! -f "$COMPOSE_FILE" ]; then
        log_error "Docker Compose file not found: ${COMPOSE_FILE}"
        return 1
    fi
    
    log_info "Restarting application services..."
    
    cd "$PROJECT_DIR" || {
        log_error "Cannot change to project directory: ${PROJECT_DIR}"
        return 1
    }
    
    # 重启 backend 和 frontend（使用 master profile）
    if docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" \
        --profile master up -d backend frontend >> "$LOG_FILE" 2>&1; then
        log_info "Services restarted successfully"
        
        # 等待服务就绪
        sleep 5
        
        # 验证服务状态（容器名称包含 NODE_ROLE，所以用 grep 匹配）
        if docker ps --format "{{.Names}}" | grep -qE "law-firm-backend-(master|slave)"; then
            log_info "Backend service is running"
        else
            log_warn "Backend service may not be running"
        fi
        
        if docker ps --format "{{.Names}}" | grep -qE "law-firm-frontend-(master|slave)"; then
            log_info "Frontend service is running"
        else
            log_warn "Frontend service may not be running"
        fi
        
        return 0
    else
        log_error "Failed to restart services"
        return 1
    fi
}

# 验证切换是否成功
verify_failover() {
    log_info "Verifying failover..."
    
    # 确定要检查的容器名称（优先检查从库容器，如果不存在则检查主库容器）
    local container_name=""
    if docker ps --format "{{.Names}}" | grep -q "^law-firm-postgres-slave$"; then
        container_name="law-firm-postgres-slave"
    elif docker ps --format "{{.Names}}" | grep -q "^law-firm-postgres-master$"; then
        container_name="law-firm-postgres-master"
    else
        log_error "No PostgreSQL container found for verification"
        return 1
    fi
    
    log_info "Verifying PostgreSQL container: ${container_name}"
    
    if docker exec "$container_name" \
        psql -U law_admin -d law_firm -c \
        "CREATE TABLE IF NOT EXISTS failover_test (id INT); DROP TABLE IF EXISTS failover_test;" \
        >> "$LOG_FILE" 2>&1; then
        log_info "Failover verification: PostgreSQL can write ✓"
        return 0
    else
        log_error "Failover verification: PostgreSQL cannot write ✗"
        return 1
    fi
}

# 主函数
main() {
    log_info "=========================================="
    log_info "Keepalived Failover: Server became MASTER"
    log_info "=========================================="
    
    # 检查是否需要提升
    if ! check_current_role; then
        log_info "No promotion needed, already master"
        return 0
    fi
    
    # 步骤1：提升从库为主库
    if ! promote_slave; then
        log_error "Failed to promote slave, aborting"
        return 1
    fi
    
    # 步骤2：更新配置
    if ! update_config; then
        log_warn "Failed to update config, continuing anyway"
    fi
    
    # 步骤3：重启服务
    if ! restart_services; then
        log_error "Failed to restart services"
        return 1
    fi
    
    # 步骤4：验证切换
    if ! verify_failover; then
        log_warn "Failover verification failed, but services are running"
    fi
    
    log_info "=========================================="
    log_info "Failover completed successfully!"
    log_info "=========================================="
    
    return 0
}

# 执行主函数
main "$@"
exit $?
