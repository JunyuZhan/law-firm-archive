#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 示例数据初始化脚本
# =====================================================
# 用途: 部署后自动生成示例数据，用于演示和测试
# 用法: ./init-demo-data.sh [选项]
# 
# 选项:
#   --docker          使用 Docker 容器执行 (默认)
#   --local           使用本地 psql 执行
#   --minimal         只生成最小示例数据
#   --full            生成完整示例数据 (默认)
#   --reset           重置已有示例数据后重新生成
#   --help            显示帮助信息
# =====================================================

set -e

# =====================================================
# 基础配置
# =====================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
INIT_DB_DIR="$SCRIPT_DIR/init-db"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# 默认配置
USE_DOCKER=true
DATA_LEVEL="full"  # minimal 或 full
RESET_DATA=false
DEMO_DATA_CREATED=false  # 跟踪是否成功创建了demo数据

# 数据库连接配置
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="law_firm"
DB_USER="law_admin"
DOCKER_CONTAINER="law-firm-postgres"

# =====================================================
# 工具函数
# =====================================================
log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✓]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }

print_banner() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}    ${BOLD}律师事务所管理系统 - 示例数据初始化${NC}                    ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

show_help() {
    echo ""
    echo -e "${BOLD}用法: $0 [选项]${NC}"
    echo ""
    echo "选项:"
    echo "  --docker          使用 Docker 容器执行 (默认)"
    echo "  --local           使用本地 psql 执行"
    echo "  --minimal         只生成最小示例数据（基础客户、项目）"
    echo "  --full            生成完整示例数据 (默认)"
    echo "  --reset           重置已有示例数据后重新生成"
    echo "  --help            显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0                # Docker 模式，完整数据"
    echo "  $0 --minimal      # Docker 模式，最小数据"
    echo "  $0 --local --full # 本地 psql，完整数据"
    echo "  $0 --reset        # 重置后重新生成"
    echo ""
}

# =====================================================
# 解析命令行参数
# =====================================================
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --docker)
                USE_DOCKER=true
                shift
                ;;
            --local)
                USE_DOCKER=false
                shift
                ;;
            --minimal)
                DATA_LEVEL="minimal"
                shift
                ;;
            --full)
                DATA_LEVEL="full"
                shift
                ;;
            --reset)
                RESET_DATA=true
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# =====================================================
# 执行 SQL 文件
# =====================================================
execute_sql_file() {
    local sql_file=$1
    local description=$2
    local exit_code=0
    
    if [ ! -f "$sql_file" ]; then
        log_warn "文件不存在: $sql_file"
        return 1
    fi
    
    log_info "执行: $description"
    
    if [ "$USE_DOCKER" = true ]; then
        # 使用管道，捕获psql的退出码
        docker exec -i "$DOCKER_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$sql_file" 2>&1 | \
            grep -E "(NOTICE|ERROR|INSERT|UPDATE|DELETE)" || true
        exit_code=${PIPESTATUS[0]}
    else
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$sql_file" 2>&1 | \
            grep -E "(NOTICE|ERROR|INSERT|UPDATE|DELETE)" || true
        exit_code=${PIPESTATUS[0]}
    fi
    
    if [ $exit_code -eq 0 ]; then
        log_success "$description 完成"
        return 0
    else
        log_error "$description 失败 (退出码: $exit_code)"
        return 1
    fi
}

# =====================================================
# 执行 SQL 语句
# =====================================================
execute_sql() {
    local sql=$1
    
    if [ "$USE_DOCKER" = true ]; then
        echo "$sql" | docker exec -i "$DOCKER_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" 2>&1
    else
        echo "$sql" | PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" 2>&1
    fi
}

# =====================================================
# 检查环境
# =====================================================
check_environment() {
    log_info "检查运行环境..."
    
    if [ "$USE_DOCKER" = true ]; then
        # 检查 Docker 容器
        if ! docker ps --format '{{.Names}}' | grep -q "^${DOCKER_CONTAINER}$"; then
            log_error "PostgreSQL 容器未运行: $DOCKER_CONTAINER"
            log_info "请先启动服务: cd docker && docker compose up -d"
            exit 1
        fi
        log_success "Docker 容器运行正常"
    else
        # 检查本地 psql
        if ! command -v psql &> /dev/null; then
            log_error "psql 未安装"
            exit 1
        fi
        
        # 检查数据库连接
        if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" &> /dev/null; then
            log_error "无法连接到数据库"
            exit 1
        fi
        log_success "数据库连接正常"
    fi
}

# =====================================================
# 重置示例数据
# =====================================================
reset_demo_data() {
    log_warn "正在清理已有的示例数据..."
    
    local reset_sql="
    -- 禁用外键约束检查
    SET session_replication_role = 'replica';
    
    -- 清理示例数据 (ID >= 100 的记录)
    DELETE FROM archive_borrow WHERE id >= 100;
    DELETE FROM archive WHERE id >= 100;
    DELETE FROM archive_location WHERE id >= 1;
    DELETE FROM task WHERE id >= 100;
    DELETE FROM matter_participant WHERE id >= 100;
    DELETE FROM matter_client WHERE id >= 100;
    DELETE FROM matter WHERE id >= 100;
    DELETE FROM finance_fee WHERE id >= 100;
    DELETE FROM finance_contract WHERE id >= 100;
    DELETE FROM crm_client WHERE id >= 100;
    DELETE FROM timesheet WHERE id >= 100;
    DELETE FROM letter_application WHERE id >= 100;
    DELETE FROM schedule_event WHERE id >= 100;
    DELETE FROM kb_article WHERE id >= 100;
    DELETE FROM kb_category WHERE id >= 100;
    DELETE FROM hr_attendance WHERE id >= 100;
    DELETE FROM admin_meeting_room WHERE id >= 100;
    DELETE FROM admin_seal WHERE id >= 100;
    DELETE FROM admin_seal_application WHERE id >= 100;
    
    -- 恢复外键约束
    SET session_replication_role = 'origin';
    
    -- 重置序列
    SELECT setval('crm_client_id_seq', COALESCE((SELECT MAX(id) FROM crm_client), 1));
    SELECT setval('finance_contract_id_seq', COALESCE((SELECT MAX(id) FROM finance_contract), 1));
    SELECT setval('matter_id_seq', COALESCE((SELECT MAX(id) FROM matter), 1));
    SELECT setval('task_id_seq', COALESCE((SELECT MAX(id) FROM task), 1));
    
    -- 完成提示
    DO \$\$
    BEGIN
      RAISE NOTICE '示例数据已清理完成';
    END \$\$;
    "
    
    execute_sql "$reset_sql"
    log_success "示例数据清理完成"
}

# =====================================================
# 生成最小示例数据
# =====================================================
generate_minimal_data() {
    log_info "生成最小示例数据..."
    
    # 使用整合版演示数据文件（v2.0整合后的文件名）
    if [ -f "$INIT_DB_DIR/30-demo-data-full.sql" ]; then
        if execute_sql_file "$INIT_DB_DIR/30-demo-data-full.sql" "完整示例数据（已整合）"; then
            DEMO_DATA_CREATED=true
        fi
    elif [ -f "$INIT_DB_DIR/30-demo-data.sql" ]; then
        if execute_sql_file "$INIT_DB_DIR/30-demo-data.sql" "基础示例数据（客户、合同、项目、任务）"; then
            DEMO_DATA_CREATED=true
        fi
    else
        log_error "未找到示例数据文件！"
        log_info "请确保以下文件之一存在："
        log_info "  - $INIT_DB_DIR/30-demo-data-full.sql"
        log_info "  - $INIT_DB_DIR/30-demo-data.sql"
        return 1
    fi
}

# =====================================================
# 生成完整示例数据
# =====================================================
generate_full_data() {
    log_info "生成完整示例数据..."
    
    # 优先使用整合版演示数据文件（v2.0整合后的文件名）
    if [ -f "$INIT_DB_DIR/30-demo-data-full.sql" ]; then
        if execute_sql_file "$INIT_DB_DIR/30-demo-data-full.sql" "完整示例数据（已整合）"; then
            DEMO_DATA_CREATED=true
        fi
        return
    fi
    
    # 回退到旧版分散的演示数据文件
    if [ -f "$INIT_DB_DIR/30-demo-data.sql" ]; then
        if execute_sql_file "$INIT_DB_DIR/30-demo-data.sql" "基础示例数据（客户、合同、项目、任务）"; then
            DEMO_DATA_CREATED=true
        fi
    else
        log_error "未找到示例数据文件！"
        log_info "请确保以下文件之一存在："
        log_info "  - $INIT_DB_DIR/30-demo-data-full.sql"
        log_info "  - $INIT_DB_DIR/30-demo-data.sql"
        return 1
    fi
    
    # 知识库示例数据
    if [ -f "$INIT_DB_DIR/40-knowledge-demo.sql" ]; then
        execute_sql_file "$INIT_DB_DIR/40-knowledge-demo.sql" "知识库示例数据"
    fi
    
    # 日程示例数据
    if [ -f "$INIT_DB_DIR/41-schedule-demo.sql" ]; then
        execute_sql_file "$INIT_DB_DIR/41-schedule-demo.sql" "日程示例数据"
    fi
    
    # 人力资源示例数据
    if [ -f "$INIT_DB_DIR/42-hr-demo.sql" ]; then
        execute_sql_file "$INIT_DB_DIR/42-hr-demo.sql" "人力资源示例数据"
    fi
    
    # 行政管理示例数据
    if [ -f "$INIT_DB_DIR/43-admin-demo.sql" ]; then
        execute_sql_file "$INIT_DB_DIR/43-admin-demo.sql" "行政管理示例数据"
    fi
    
    # 财务记录示例数据
    if [ -f "$INIT_DB_DIR/44-finance-demo.sql" ]; then
        execute_sql_file "$INIT_DB_DIR/44-finance-demo.sql" "财务记录示例数据"
    fi
    
    # 额外示例数据
    if [ -f "$INIT_DB_DIR/99-extra-demo-data.sql" ]; then
        execute_sql_file "$INIT_DB_DIR/99-extra-demo-data.sql" "增量示例数据"
    fi
}

# =====================================================
# 显示完成信息
# =====================================================
show_completion() {
    echo ""
    
    if [ "$DEMO_DATA_CREATED" = true ]; then
        echo -e "${GREEN}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║${NC}                                                              ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}    ${BOLD}✅ 示例数据初始化完成！${NC}                                  ${GREEN}║${NC}"
        echo -e "${GREEN}║${NC}                                                              ${GREEN}║${NC}"
        echo -e "${GREEN}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        
        if [ "$DATA_LEVEL" = "minimal" ]; then
            echo -e "${BOLD}已创建的最小示例数据：${NC}"
            echo "  📋 客户: 7个（5企业 + 2个人）"
            echo "  📄 合同: 6份"
            echo "  📁 项目: 6个"
            echo "  ✅ 任务: 13个"
            echo "  📦 归档: 3个"
        else
            echo -e "${BOLD}已创建的完整示例数据：${NC}"
            echo "  📋 客户: 10+个"
            echo "  📄 合同: 9+份"
            echo "  📁 项目: 6+个"
            echo "  ✅ 任务: 13+个"
            echo "  📦 归档: 3个"
            echo "  📚 知识库文章: 10+篇"
            echo "  📅 日程事件: 15+条"
            echo "  ⏰ 考勤记录: 若干"
            echo "  🏢 会议室: 3个"
            echo "  🔏 印章: 3个"
            echo "  💰 收费/付款记录: 若干"
        fi
        
        echo ""
        echo -e "${BOLD}默认账号：${NC}"
        echo "  👤 admin / director / lawyer1 / leader / finance / staff / trainee"
        echo "  🔐 密码统一为: admin123"
        echo ""
        echo -e "${DIM}提示: 使用 --reset 选项可以清理并重新生成示例数据${NC}"
    else
        echo -e "${YELLOW}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${YELLOW}║${NC}                                                              ${YELLOW}║${NC}"
        echo -e "${YELLOW}║${NC}    ${BOLD}⚠️  未创建示例数据${NC}                                        ${YELLOW}║${NC}"
        echo -e "${YELLOW}║${NC}                                                              ${YELLOW}║${NC}"
        echo -e "${YELLOW}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        echo -e "${DIM}系统基础数据已在初始化脚本中配置，可以正常使用。${NC}"
        echo -e "${DIM}如需示例数据，请确保 30-demo-data-full.sql 文件存在。${NC}"
    fi
    echo ""
}

# =====================================================
# 主函数
# =====================================================
main() {
    parse_args "$@"
    print_banner
    
    log_info "配置信息:"
    echo "  - 执行模式: $([ "$USE_DOCKER" = true ] && echo "Docker" || echo "本地")"
    echo "  - 数据级别: $([ "$DATA_LEVEL" = "minimal" ] && echo "最小" || echo "完整")"
    echo "  - 重置数据: $([ "$RESET_DATA" = true ] && echo "是" || echo "否")"
    echo ""
    
    # 检查环境
    check_environment
    
    # 重置数据（如果需要）
    if [ "$RESET_DATA" = true ]; then
        reset_demo_data
    fi
    
    # 生成数据（允许失败，不影响脚本继续）
    set +e
    if [ "$DATA_LEVEL" = "minimal" ]; then
        generate_minimal_data
    else
        generate_full_data
    fi
    set -e
    
    # 显示完成信息
    show_completion
}

# 执行主函数
main "$@"
