#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 数据备份脚本
# =====================================================
# 用法:
#   ./backup.sh                    # 完整备份（数据库 + 文件）
#   ./backup.sh db                 # 仅备份数据库
#   ./backup.sh files              # 仅备份文件存储
#   ./backup.sh --schedule         # 设置定时备份
# =====================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$SCRIPTS_DIR")"

# 配置
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
CONTAINER_DB="law-firm-postgres"
CONTAINER_MINIO="law-firm-minio"
DB_NAME="law_firm"
DB_USER="law_admin"
RETENTION_DAYS=30  # 保留最近30天的备份

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 创建备份目录
create_backup_dir() {
    local date_dir="$BACKUP_DIR/$(date +%Y-%m-%d)"
    mkdir -p "$date_dir"
    echo "$date_dir"
}

# 备份数据库
backup_database() {
    log_info "开始备份数据库..."
    
    local backup_dir=$(create_backup_dir)
    local timestamp=$(date +%H%M%S)
    local backup_file="$backup_dir/db_${timestamp}.sql.gz"
    
    # 检查容器是否运行
    if ! docker ps | grep -q "$CONTAINER_DB"; then
        log_error "数据库容器 $CONTAINER_DB 未运行"
        return 1
    fi
    
    # 执行备份（带压缩）
    docker exec "$CONTAINER_DB" pg_dump -U "$DB_USER" -d "$DB_NAME" \
        --format=plain --no-owner --no-acl | gzip > "$backup_file"
    
    local size=$(du -h "$backup_file" | cut -f1)
    log_success "数据库备份完成: $backup_file ($size)"
    
    echo "$backup_file"
}

# 备份 MinIO 文件
backup_minio() {
    log_info "开始备份文件存储..."
    
    local backup_dir=$(create_backup_dir)
    local timestamp=$(date +%H%M%S)
    local backup_file="$backup_dir/minio_${timestamp}.tar.gz"
    
    # 检查容器是否运行
    if ! docker ps | grep -q "$CONTAINER_MINIO"; then
        log_error "MinIO 容器 $CONTAINER_MINIO 未运行"
        return 1
    fi
    
    # 获取 MinIO 数据卷
    local volume_path=$(docker volume inspect law-firm_minio_data --format '{{.Mountpoint}}' 2>/dev/null || \
                       docker volume inspect docker_minio_data --format '{{.Mountpoint}}' 2>/dev/null || \
                       echo "")
    
    if [ -z "$volume_path" ]; then
        log_warn "未找到 MinIO 数据卷，尝试通过容器备份..."
        # 通过容器内部打包
        docker exec "$CONTAINER_MINIO" tar -czf /tmp/minio_backup.tar.gz -C /data .
        docker cp "$CONTAINER_MINIO:/tmp/minio_backup.tar.gz" "$backup_file"
        docker exec "$CONTAINER_MINIO" rm /tmp/minio_backup.tar.gz
    else
        # 直接备份卷目录（需要 root 权限）
        sudo tar -czf "$backup_file" -C "$volume_path" .
    fi
    
    local size=$(du -h "$backup_file" | cut -f1)
    log_success "文件存储备份完成: $backup_file ($size)"
    
    echo "$backup_file"
}

# 清理旧备份
cleanup_old_backups() {
    log_info "清理 ${RETENTION_DAYS} 天前的旧备份..."
    
    local count=$(find "$BACKUP_DIR" -type f -mtime +$RETENTION_DAYS 2>/dev/null | wc -l)
    
    if [ "$count" -gt 0 ]; then
        find "$BACKUP_DIR" -type f -mtime +$RETENTION_DAYS -delete
        find "$BACKUP_DIR" -type d -empty -delete 2>/dev/null || true
        log_success "已清理 $count 个旧备份文件"
    else
        log_info "没有需要清理的旧备份"
    fi
}

# 完整备份
full_backup() {
    log_info "=============================================="
    log_info "开始完整备份..."
    log_info "=============================================="
    
    local start_time=$(date +%s)
    
    backup_database
    backup_minio
    cleanup_old_backups
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    log_success "=============================================="
    log_success "完整备份完成！耗时: ${duration}秒"
    log_success "=============================================="
    log_info "备份目录: $BACKUP_DIR/$(date +%Y-%m-%d)"
}

# 设置定时备份
setup_schedule() {
    log_info "设置定时备份任务..."
    
    local script_path="$SCRIPT_DIR/backup.sh"
    local cron_job="0 2 * * * $script_path >> /var/log/law-firm-backup.log 2>&1"
    
    # 检查是否已存在
    if crontab -l 2>/dev/null | grep -q "$script_path"; then
        log_warn "定时备份任务已存在"
        crontab -l | grep "$script_path"
        return
    fi
    
    # 添加 cron 任务
    (crontab -l 2>/dev/null; echo "$cron_job") | crontab -
    
    log_success "定时备份已设置！"
    log_info "执行时间: 每天凌晨 2:00"
    log_info "日志文件: /var/log/law-firm-backup.log"
    echo ""
    log_info "查看定时任务: crontab -l"
    log_info "删除定时任务: crontab -e"
}

# 显示帮助
show_help() {
    echo "律师事务所管理系统 - 数据备份脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  (无参数)        完整备份（数据库 + 文件）"
    echo "  db              仅备份数据库"
    echo "  files           仅备份文件存储"
    echo "  --schedule      设置每日定时备份（凌晨2点）"
    echo "  --help          显示帮助"
    echo ""
    echo "环境变量:"
    echo "  BACKUP_DIR      备份存储目录（默认: ./backups）"
    echo ""
    echo "示例:"
    echo "  $0                        # 完整备份"
    echo "  $0 db                     # 仅备份数据库"
    echo "  BACKUP_DIR=/mnt/backup $0 # 备份到指定目录"
}

# 主入口
case "${1:-full}" in
    db|database)
        backup_database
        ;;
    files|minio)
        backup_minio
        ;;
    --schedule)
        setup_schedule
        ;;
    --help|-h|help)
        show_help
        ;;
    full|"")
        full_backup
        ;;
    *)
        log_error "未知命令: $1"
        show_help
        exit 1
        ;;
esac

