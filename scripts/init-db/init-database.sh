#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 数据库初始化脚本
# =====================================================
# 版本: 1.0.0
# 日期: 2026-01-08
# 用法: ./init-database.sh [选项]
# 选项:
#   --docker    使用 Docker 容器中的 PostgreSQL
#   --drop      先删除已存在的数据库
#   --help      显示帮助信息
# =====================================================

set -e

# 默认配置
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-law_firm_dev}"
DB_USER="${DB_USER:-law_admin}"
DB_PASSWORD="${DB_PASSWORD:-dev_password_123}"
DOCKER_CONTAINER="${DOCKER_CONTAINER:-law-firm-postgres}"
USE_DOCKER=false
DROP_DB=false

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 显示帮助
show_help() {
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --docker    使用 Docker 容器中的 PostgreSQL"
    echo "  --drop      先删除已存在的数据库"
    echo "  --help      显示帮助信息"
    echo ""
    echo "环境变量:"
    echo "  DB_HOST     数据库主机 (默认: localhost)"
    echo "  DB_PORT     数据库端口 (默认: 5432)"
    echo "  DB_NAME     数据库名称 (默认: law_firm_dev)"
    echo "  DB_USER     数据库用户 (默认: law_admin)"
    echo "  DB_PASSWORD 数据库密码 (默认: dev_password_123)"
    echo "  DOCKER_CONTAINER Docker容器名 (默认: law-firm-postgres)"
}

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --docker)
            USE_DOCKER=true
            shift
            ;;
        --drop)
            DROP_DB=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 执行 SQL 的函数
run_sql() {
    local sql_file=$1
    if [ "$USE_DOCKER" = true ]; then
        docker exec -i "$DOCKER_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$sql_file"
    else
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$sql_file"
    fi
}

# 执行单条 SQL 命令
run_sql_cmd() {
    local cmd=$1
    if [ "$USE_DOCKER" = true ]; then
        docker exec -i "$DOCKER_CONTAINER" psql -U "$DB_USER" -d postgres -c "$cmd"
    else
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "$cmd"
    fi
}

echo "========================================"
echo "律师事务所管理系统 - 数据库初始化"
echo "========================================"
echo ""
echo "配置信息:"
echo "  数据库: $DB_NAME"
echo "  用户:   $DB_USER"
echo "  Docker: $USE_DOCKER"
echo ""

# 删除已存在的数据库
if [ "$DROP_DB" = true ]; then
    echo "正在删除已存在的数据库..."
    run_sql_cmd "DROP DATABASE IF EXISTS $DB_NAME;" || true
    echo "正在创建新数据库..."
    run_sql_cmd "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
fi

# 定义脚本执行顺序
SCRIPTS=(
    "00-extensions.sql"
    "01-system-schema.sql"
    "02-client-schema.sql"
    "03-matter-schema.sql"
    "04-finance-schema.sql"
    "05-document-schema.sql"
    "06-evidence-schema.sql"
    "07-archive-schema.sql"
    "08-timesheet-schema.sql"
    "09-task-schema.sql"
    "10-admin-schema.sql"
    "11-asset-schema.sql"
    "12-knowledge-schema.sql"
    "13-hr-schema.sql"
    "14-quality-schema.sql"
    "15-workbench-schema.sql"
    "16-contract-template-schema.sql"
    "20-system-init-data.sql"
    "21-template-init-data.sql"
    "26-add-dict-menu.sql"
    "27-dict-init-data.sql"
)

# 执行脚本
total=${#SCRIPTS[@]}
current=0

for script in "${SCRIPTS[@]}"; do
    current=$((current + 1))
    script_path="$SCRIPT_DIR/$script"
    
    if [ -f "$script_path" ]; then
        echo "[$current/$total] 执行: $script"
        run_sql "$script_path" > /dev/null 2>&1 || {
            echo "错误: $script 执行失败"
            exit 1
        }
    else
        echo "警告: $script 不存在，跳过"
    fi
done

echo ""
echo "========================================"
echo "数据库初始化完成！"
echo "========================================"
echo ""
echo "默认账号:"
echo "  用户名: admin    密码: admin123    角色: 管理员"
echo "  用户名: director 密码: lawyer123   角色: 律所主任"
echo "  用户名: lawyer1  密码: lawyer123   角色: 律师"
echo ""

