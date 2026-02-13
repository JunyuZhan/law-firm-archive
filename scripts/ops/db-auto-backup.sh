#!/bin/bash
# =====================================================
# PostgreSQL 自动备份脚本（简化版）
# =====================================================
# 专注于数据库备份，MinIO 使用分布式或云存储
# 用法:
#   ./db-auto-backup.sh              # 执行备份
#   ./db-auto-backup.sh --schedule   # 设置定时任务
#   ./db-auto-backup.sh --status     # 查看备份状态
# =====================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$SCRIPTS_DIR")"

# =====================================================
# 配置区域（根据需要修改）
# =====================================================
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups/db}"
CONTAINER_DB="law-firm-postgres"
DB_NAME="law_firm"
DB_USER="law_admin"

# 保留策略
KEEP_DAILY=7      # 保留最近 7 天的每日备份
KEEP_WEEKLY=4     # 保留最近 4 周的周备份
KEEP_MONTHLY=12   # 保留最近 12 个月的月备份

# 远程备份（可选）
REMOTE_BACKUP_ENABLED=false
REMOTE_PATH=""  # 如: user@nas:/backup/law-firm/db/

# =====================================================
# 颜色输出
# =====================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') $1"; }

# =====================================================
# 备份执行
# =====================================================
do_backup() {
    local backup_type=$1  # daily, weekly, monthly
    local date_str=$(date +%Y-%m-%d)
    local time_str=$(date +%H%M%S)
    
    # 创建目录
    local type_dir="$BACKUP_DIR/$backup_type"
    mkdir -p "$type_dir"
    
    local backup_file="$type_dir/${date_str}_${time_str}.sql.gz"
    
    log_info "开始 $backup_type 备份..."
    
    # 检查容器
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_DB}$"; then
        log_error "数据库容器 $CONTAINER_DB 未运行"
        return 1
    fi
    
    # 执行备份
    local start_time=$(date +%s)
    
    docker exec "$CONTAINER_DB" pg_dump -U "$DB_USER" -d "$DB_NAME" \
        --format=plain --no-owner --no-acl \
        | gzip > "$backup_file"
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    local size=$(du -h "$backup_file" | cut -f1)
    
    log_success "备份完成: $(basename "$backup_file") ($size) 耗时: ${duration}秒"
    
    # 远程备份
    if [ "$REMOTE_BACKUP_ENABLED" = true ] && [ -n "$REMOTE_PATH" ]; then
        log_info "同步到远程存储..."
        rsync -az "$backup_file" "${REMOTE_PATH}${backup_type}/" && \
            log_success "远程备份完成" || \
            log_warn "远程备份失败，本地备份已保存"
    fi
    
    return 0
}

# =====================================================
# 清理旧备份
# =====================================================
cleanup_old_backups() {
    log_info "清理旧备份..."
    
    local daily_dir="$BACKUP_DIR/daily"
    local weekly_dir="$BACKUP_DIR/weekly"
    local monthly_dir="$BACKUP_DIR/monthly"
    
    # 清理每日备份
    if [ -d "$daily_dir" ]; then
        local daily_count=$(find "$daily_dir" -name "*.sql.gz" -mtime +$KEEP_DAILY | wc -l)
        find "$daily_dir" -name "*.sql.gz" -mtime +$KEEP_DAILY -delete
        [ "$daily_count" -gt 0 ] && log_info "清理 $daily_count 个每日备份"
    fi
    
    # 清理每周备份（7天 = 1周）
    if [ -d "$weekly_dir" ]; then
        local weekly_days=$((KEEP_WEEKLY * 7))
        local weekly_count=$(find "$weekly_dir" -name "*.sql.gz" -mtime +$weekly_days | wc -l)
        find "$weekly_dir" -name "*.sql.gz" -mtime +$weekly_days -delete
        [ "$weekly_count" -gt 0 ] && log_info "清理 $weekly_count 个每周备份"
    fi
    
    # 清理每月备份（30天 = 1月）
    if [ -d "$monthly_dir" ]; then
        local monthly_days=$((KEEP_MONTHLY * 30))
        local monthly_count=$(find "$monthly_dir" -name "*.sql.gz" -mtime +$monthly_days | wc -l)
        find "$monthly_dir" -name "*.sql.gz" -mtime +$monthly_days -delete
        [ "$monthly_count" -gt 0 ] && log_info "清理 $monthly_count 个每月备份"
    fi
}

# =====================================================
# 智能备份（根据日期自动判断类型）
# =====================================================
smart_backup() {
    local day_of_week=$(date +%u)   # 1-7, 1=周一
    local day_of_month=$(date +%d)  # 01-31
    
    # 每月1日：月备份
    if [ "$day_of_month" = "01" ]; then
        do_backup "monthly"
    fi
    
    # 每周日：周备份
    if [ "$day_of_week" = "7" ]; then
        do_backup "weekly"
    fi
    
    # 每日备份
    do_backup "daily"
    
    # 清理旧备份
    cleanup_old_backups
}

# =====================================================
# 设置定时任务
# =====================================================
setup_schedule() {
    local script_path="$(realpath "$0")"
    local log_file="/var/log/law-firm-db-backup.log"
    
    # 每天凌晨 3 点执行
    local cron_job="0 3 * * * $script_path >> $log_file 2>&1"
    
    if crontab -l 2>/dev/null | grep -q "$script_path"; then
        log_warn "定时任务已存在"
        crontab -l | grep "$script_path"
        return
    fi
    
    (crontab -l 2>/dev/null; echo "$cron_job") | crontab -
    
    log_success "定时备份已设置！"
    echo ""
    echo "执行时间: 每天凌晨 3:00"
    echo "日志文件: $log_file"
    echo "备份目录: $BACKUP_DIR"
    echo ""
    echo "备份策略:"
    echo "  - 每日备份: 保留 $KEEP_DAILY 天"
    echo "  - 每周备份: 保留 $KEEP_WEEKLY 周（周日）"
    echo "  - 每月备份: 保留 $KEEP_MONTHLY 个月（1日）"
}

# =====================================================
# 查看状态
# =====================================================
show_status() {
    echo "=============================================="
    echo "数据库备份状态"
    echo "=============================================="
    echo ""
    
    echo "📂 备份目录: $BACKUP_DIR"
    echo ""
    
    for type in daily weekly monthly; do
        local type_dir="$BACKUP_DIR/$type"
        if [ -d "$type_dir" ]; then
            local count=$(ls -1 "$type_dir"/*.sql.gz 2>/dev/null | wc -l)
            local size=$(du -sh "$type_dir" 2>/dev/null | cut -f1)
            local latest=$(ls -1t "$type_dir"/*.sql.gz 2>/dev/null | head -1)
            
            echo "📦 $type 备份: $count 个, 总大小: $size"
            if [ -n "$latest" ]; then
                echo "   └─ 最新: $(basename "$latest")"
            fi
        else
            echo "📦 $type 备份: 无"
        fi
    done
    
    echo ""
    echo "⏰ 定时任务:"
    if crontab -l 2>/dev/null | grep -q "db-auto-backup"; then
        crontab -l | grep "db-auto-backup"
    else
        echo "   未设置（运行 $0 --schedule 设置）"
    fi
}

# =====================================================
# 显示帮助
# =====================================================
show_help() {
    echo "PostgreSQL 自动备份脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  (无参数)     执行智能备份（自动判断日/周/月）"
    echo "  daily        执行每日备份"
    echo "  weekly       执行每周备份"
    echo "  monthly      执行每月备份"
    echo "  --schedule   设置每日定时备份"
    echo "  --status     查看备份状态"
    echo "  --help       显示帮助"
    echo ""
    echo "环境变量:"
    echo "  BACKUP_DIR         备份存储目录"
    echo "  KEEP_DAILY         保留每日备份天数（默认: 7）"
    echo "  KEEP_WEEKLY        保留每周备份周数（默认: 4）"
    echo "  KEEP_MONTHLY       保留每月备份月数（默认: 12）"
}

# =====================================================
# 主入口
# =====================================================
case "${1:-smart}" in
    daily)
        do_backup "daily"
        ;;
    weekly)
        do_backup "weekly"
        ;;
    monthly)
        do_backup "monthly"
        ;;
    smart|"")
        smart_backup
        ;;
    --schedule)
        setup_schedule
        ;;
    --status)
        show_status
        ;;
    --help|-h|help)
        show_help
        ;;
    *)
        log_error "未知命令: $1"
        show_help
        exit 1
        ;;
esac

