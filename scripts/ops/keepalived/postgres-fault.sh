#!/bin/bash
# =====================================================
# Keepalived 故障脚本：当服务器进入故障状态时执行
# =====================================================
# 用途：记录故障状态，可以发送告警
# 调用时机：Keepalived 检测到本地故障
# =====================================================

LOG_FILE="/var/log/postgres-failover.log"

log() {
    local level="$1"
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "${timestamp} [${level}] ${message}" | tee -a "$LOG_FILE"
}

log_error() {
    log "ERROR" "$@"
}

log_error "=========================================="
log_error "Keepalived: Server entered FAULT state"
log_error "=========================================="
log_error "Local fault detected, may need manual intervention"
log_error "=========================================="

# 可以在这里添加告警通知（邮件、短信、Webhook等）
# 例如：
# curl -X POST https://your-webhook-url \
#   -d '{"text": "Keepalived fault detected on '$(hostname)'"}'

exit 0
