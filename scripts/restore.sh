#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 数据恢复脚本
# =====================================================
# 用法:
#   ./restore.sh list              # 列出可用备份
#   ./restore.sh db <备份文件>     # 恢复数据库
#   ./restore.sh files <备份文件>  # 恢复文件存储
#   ./restore.sh full <日期>       # 完整恢复（如: 2026-01-09）
# =====================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 配置
BACKUP_DIR="${BACKUP_DIR:-$PROJECT_ROOT/backups}"
CONTAINER_DB="law-firm-postgres"
CONTAINER_MINIO="law-firm-minio"
DB_NAME="law_firm"
DB_USER="law_admin"

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

# 列出可用备份
list_backups() {
    log_info "可用备份列表:"
    echo ""
    
    if [ ! -d "$BACKUP_DIR" ]; then
        log_warn "备份目录不存在: $BACKUP_DIR"
        return
    fi
    
    for date_dir in $(ls -1r "$BACKUP_DIR" 2>/dev/null); do
        if [ -d "$BACKUP_DIR/$date_dir" ]; then
            echo "📅 $date_dir"
            
            # 列出数据库备份
            for db_file in $(ls -1 "$BACKUP_DIR/$date_dir"/db_*.sql.gz 2>/dev/null); do
                local size=$(du -h "$db_file" | cut -f1)
                local name=$(basename "$db_file")
                echo "   └─ 🗄️  $name ($size)"
            done
            
            # 列出文件备份
            for minio_file in $(ls -1 "$BACKUP_DIR/$date_dir"/minio_*.tar.gz 2>/dev/null); do
                local size=$(du -h "$minio_file" | cut -f1)
                local name=$(basename "$minio_file")
                echo "   └─ 📁 $name ($size)"
            done
            echo ""
        fi
    done
}

# 恢复数据库
restore_database() {
    local backup_file=$1
    
    if [ -z "$backup_file" ]; then
        log_error "请指定备份文件"
        log_info "用法: $0 db <备份文件路径>"
        exit 1
    fi
    
    if [ ! -f "$backup_file" ]; then
        log_error "备份文件不存在: $backup_file"
        exit 1
    fi
    
    log_warn "⚠️  警告: 此操作将覆盖现有数据库！"
    read -p "确认恢复? (y/N) " confirm
    
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        log_info "操作已取消"
        exit 0
    fi
    
    log_info "开始恢复数据库..."
    
    # 检查容器
    if ! docker ps | grep -q "$CONTAINER_DB"; then
        log_error "数据库容器 $CONTAINER_DB 未运行"
        exit 1
    fi
    
    # 断开现有连接
    log_info "断开现有数据库连接..."
    docker exec "$CONTAINER_DB" psql -U "$DB_USER" -d postgres -c \
        "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" \
        > /dev/null 2>&1 || true
    
    # 删除并重建数据库
    log_info "重建数据库..."
    docker exec "$CONTAINER_DB" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
    docker exec "$CONTAINER_DB" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
    
    # 恢复数据
    log_info "恢复数据..."
    gunzip -c "$backup_file" | docker exec -i "$CONTAINER_DB" psql -U "$DB_USER" -d "$DB_NAME" > /dev/null
    
    log_success "数据库恢复完成！"
}

# 恢复文件存储
restore_minio() {
    local backup_file=$1
    
    if [ -z "$backup_file" ]; then
        log_error "请指定备份文件"
        log_info "用法: $0 files <备份文件路径>"
        exit 1
    fi
    
    if [ ! -f "$backup_file" ]; then
        log_error "备份文件不存在: $backup_file"
        exit 1
    fi
    
    log_warn "⚠️  警告: 此操作将覆盖现有文件！"
    read -p "确认恢复? (y/N) " confirm
    
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        log_info "操作已取消"
        exit 0
    fi
    
    log_info "开始恢复文件存储..."
    
    # 检查容器
    if ! docker ps | grep -q "$CONTAINER_MINIO"; then
        log_error "MinIO 容器 $CONTAINER_MINIO 未运行"
        exit 1
    fi
    
    # 通过容器恢复
    docker cp "$backup_file" "$CONTAINER_MINIO:/tmp/minio_restore.tar.gz"
    docker exec "$CONTAINER_MINIO" sh -c "rm -rf /data/* && tar -xzf /tmp/minio_restore.tar.gz -C /data && rm /tmp/minio_restore.tar.gz"
    
    log_success "文件存储恢复完成！"
}

# 完整恢复
restore_full() {
    local date=$1
    
    if [ -z "$date" ]; then
        log_error "请指定备份日期"
        log_info "用法: $0 full <日期>"
        log_info "示例: $0 full 2026-01-09"
        exit 1
    fi
    
    local date_dir="$BACKUP_DIR/$date"
    
    if [ ! -d "$date_dir" ]; then
        log_error "备份目录不存在: $date_dir"
        list_backups
        exit 1
    fi
    
    # 查找最新的备份文件
    local db_file=$(ls -1t "$date_dir"/db_*.sql.gz 2>/dev/null | head -1)
    local minio_file=$(ls -1t "$date_dir"/minio_*.tar.gz 2>/dev/null | head -1)
    
    if [ -z "$db_file" ]; then
        log_error "未找到数据库备份文件"
        exit 1
    fi
    
    log_warn "=============================================="
    log_warn "即将恢复以下备份:"
    log_warn "  数据库: $(basename "$db_file")"
    [ -n "$minio_file" ] && log_warn "  文件:   $(basename "$minio_file")"
    log_warn "=============================================="
    log_warn "⚠️  此操作将覆盖所有现有数据！"
    read -p "确认恢复? (y/N) " confirm
    
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        log_info "操作已取消"
        exit 0
    fi
    
    # 恢复数据库（跳过确认，因为上面已确认）
    log_info "恢复数据库..."
    docker exec "$CONTAINER_DB" psql -U "$DB_USER" -d postgres -c \
        "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$DB_NAME' AND pid <> pg_backend_pid();" \
        > /dev/null 2>&1 || true
    docker exec "$CONTAINER_DB" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
    docker exec "$CONTAINER_DB" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
    gunzip -c "$db_file" | docker exec -i "$CONTAINER_DB" psql -U "$DB_USER" -d "$DB_NAME" > /dev/null
    log_success "数据库恢复完成"
    
    # 恢复文件
    if [ -n "$minio_file" ]; then
        log_info "恢复文件存储..."
        docker cp "$minio_file" "$CONTAINER_MINIO:/tmp/minio_restore.tar.gz"
        docker exec "$CONTAINER_MINIO" sh -c "rm -rf /data/* && tar -xzf /tmp/minio_restore.tar.gz -C /data && rm /tmp/minio_restore.tar.gz"
        log_success "文件存储恢复完成"
    fi
    
    echo ""
    log_success "=============================================="
    log_success "完整恢复完成！"
    log_success "=============================================="
}

# 显示帮助
show_help() {
    echo "律师事务所管理系统 - 数据恢复脚本"
    echo ""
    echo "用法: $0 <命令> [参数]"
    echo ""
    echo "命令:"
    echo "  list                  列出所有可用备份"
    echo "  db <备份文件>         恢复数据库"
    echo "  files <备份文件>      恢复文件存储"
    echo "  full <日期>           完整恢复指定日期的备份"
    echo "  --help                显示帮助"
    echo ""
    echo "示例:"
    echo "  $0 list"
    echo "  $0 db ./backups/2026-01-09/db_020000.sql.gz"
    echo "  $0 full 2026-01-09"
}

# 主入口
case "${1:-help}" in
    list)
        list_backups
        ;;
    db|database)
        restore_database "$2"
        ;;
    files|minio)
        restore_minio "$2"
        ;;
    full)
        restore_full "$2"
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

