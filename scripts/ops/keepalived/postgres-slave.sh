#!/bin/bash
# =====================================================
# Keepalived 切换脚本：当服务器成为 Backup 时执行
# =====================================================
# 用途：记录状态变化，通常不需要操作
# 调用时机：Keepalived 检测到主服务器恢复，切换虚拟IP回主服务器时
# =====================================================

LOG_FILE="/var/log/postgres-failover.log"

log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "${timestamp} [${level}] ${message}" | tee -a "$LOG_FILE"
}

log_info() {
    log "INFO" "$@"
}

log_info "=========================================="
log_info "Keepalived: Server became BACKUP"
log_info "=========================================="
log_info "No action needed, keeping slave role"
log_info "=========================================="

exit 0
