#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 系统升级脚本
# =====================================================
# 用法:
#   ./upgrade.sh              # 交互式升级（带备份确认）
#   ./upgrade.sh --quick      # 快速升级（跳过确认，自动备份）
#   ./upgrade.sh --no-backup  # 跳过备份直接升级
#   ./upgrade.sh --check      # 仅检查更新，不执行
# =====================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$SCRIPTS_DIR")"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }

# 参数
QUICK_MODE=false
SKIP_BACKUP=false
CHECK_ONLY=false

# 解析参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --quick|-q)
                QUICK_MODE=true
                shift
                ;;
            --no-backup)
                SKIP_BACKUP=true
                shift
                ;;
            --check|-c)
                CHECK_ONLY=true
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

show_help() {
    echo "律师事务所管理系统 - 系统升级脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  (无参数)        交互式升级（询问是否备份）"
    echo "  --quick, -q     快速升级（自动备份，跳过确认）"
    echo "  --no-backup     跳过备份直接升级（危险！）"
    echo "  --check, -c     仅检查更新，不执行升级"
    echo "  --help, -h      显示帮助"
    echo ""
    echo "示例:"
    echo "  $0                  # 交互式升级"
    echo "  $0 --quick          # 快速升级（推荐用于CI/CD）"
    echo "  $0 --check          # 检查是否有更新"
}

# 获取当前分支
get_current_branch() {
    git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo ""
}

# 获取当前版本（本地）
get_local_version() {
    git -C "$PROJECT_ROOT" describe --tags --abbrev=0 2>/dev/null || \
    git -C "$PROJECT_ROOT" rev-parse --short HEAD 2>/dev/null || echo "unknown"
}

# 获取远程最新版本
get_remote_version() {
    local branch=$1
    git -C "$PROJECT_ROOT" fetch origin "$branch" --tags --quiet 2>/dev/null || true
    git -C "$PROJECT_ROOT" describe --tags --abbrev=0 "origin/$branch" 2>/dev/null || \
    git -C "$PROJECT_ROOT" rev-parse --short "origin/$branch" 2>/dev/null || echo "unknown"
}

# 检查是否有更新
check_updates() {
    local branch=$(get_current_branch)
    
    if [ -z "$branch" ]; then
        log_error "无法检测当前分支，请确保在 Git 仓库中"
        exit 1
    fi
    
    log_info "当前分支: ${CYAN}$branch${NC}"
    
    # 获取版本信息
    local local_version=$(get_local_version)
    log_info "本地版本: ${CYAN}$local_version${NC}"
    
    log_info "检查远程更新..."
    git -C "$PROJECT_ROOT" fetch origin "$branch" --quiet 2>/dev/null || {
        log_error "无法连接到远程仓库"
        exit 1
    }
    
    local remote_version=$(get_remote_version "$branch")
    log_info "远程版本: ${CYAN}$remote_version${NC}"
    
    # 检查是否有新提交
    local local_commit=$(git -C "$PROJECT_ROOT" rev-parse HEAD)
    local remote_commit=$(git -C "$PROJECT_ROOT" rev-parse "origin/$branch")
    
    if [ "$local_commit" = "$remote_commit" ]; then
        log_success "✅ 已是最新版本，无需升级"
        return 1
    else
        local behind=$(git -C "$PROJECT_ROOT" rev-list --count HEAD.."origin/$branch")
        log_warn "📦 发现 ${behind} 个新提交"
        
        # 显示更新内容
        echo ""
        log_info "更新内容预览:"
        git -C "$PROJECT_ROOT" log --oneline HEAD.."origin/$branch" | head -10
        echo ""
        
        return 0
    fi
}

# 执行备份
do_backup() {
    log_info "执行数据备份..."
    
    if [ -x "$SCRIPT_DIR/backup.sh" ]; then
        "$SCRIPT_DIR/backup.sh" || {
            log_error "备份失败"
            exit 1
        }
    else
        log_warn "未找到备份脚本，跳过备份"
    fi
}

# 执行升级
do_upgrade() {
    local branch=$(get_current_branch)
    
    log_info "=============================================="
    log_info "开始升级..."
    log_info "分支: ${CYAN}$branch${NC}"
    log_info "=============================================="
    
    # 检查是否有未提交的更改
    if ! git -C "$PROJECT_ROOT" diff --quiet 2>/dev/null; then
        log_warn "检测到未提交的本地更改"
        if [ "$QUICK_MODE" = true ]; then
            log_info "暂存本地更改..."
            git -C "$PROJECT_ROOT" stash push -m "upgrade-auto-stash-$(date +%Y%m%d%H%M%S)"
        else
            read -p "是否暂存本地更改并继续? (y/N) " confirm
            if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
                git -C "$PROJECT_ROOT" stash push -m "upgrade-auto-stash-$(date +%Y%m%d%H%M%S)"
            else
                log_error "请先处理本地更改后再升级"
                exit 1
            fi
        fi
    fi
    
    # 拉取最新代码
    log_info "拉取最新代码..."
    git -C "$PROJECT_ROOT" pull origin "$branch" || {
        log_error "拉取代码失败"
        exit 1
    }
    
    # 执行部署
    log_info "重新部署服务..."
    local deploy_script=""
    
    # 根据分支选择部署脚本
    if [ "$branch" = "feature/client-service-system" ] && [ -f "$PROJECT_ROOT/client-service/deploy.sh" ]; then
        deploy_script="$PROJECT_ROOT/client-service/deploy.sh"
    elif [ -f "$SCRIPTS_DIR/deploy/deploy.sh" ]; then
        deploy_script="$SCRIPTS_DIR/deploy/deploy.sh"
    elif [ -f "$SCRIPTS_DIR/deploy.sh" ]; then
        deploy_script="$SCRIPTS_DIR/deploy.sh"
    fi
    
    if [ -n "$deploy_script" ] && [ -x "$deploy_script" ]; then
        "$deploy_script" --quick --no-cache || {
            log_error "部署失败"
            exit 1
        }
    else
        log_warn "未找到部署脚本，需要手动部署"
        # 检查是否存在 .env 文件（在项目根目录或 docker 目录）
        local env_file=""
        if [ -f "$PROJECT_ROOT/.env" ]; then
            env_file="--env-file $PROJECT_ROOT/.env"
        elif [ -f "$PROJECT_ROOT/docker/.env" ]; then
            env_file="--env-file $PROJECT_ROOT/docker/.env"
        fi
        log_info "请执行: cd $PROJECT_ROOT/docker && docker compose $env_file -f docker-compose.prod.yml up -d --build"
    fi
    
    echo ""
    log_success "=============================================="
    log_success "✅ 升级完成！"
    log_success "=============================================="
    
    # 显示新版本
    local new_version=$(get_local_version)
    log_info "当前版本: ${CYAN}$new_version${NC}"
}

# 主流程
main() {
    parse_args "$@"
    
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}         ${BOLD}律师事务所管理系统 - 系统升级${NC}                  ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    # 检查更新
    if ! check_updates; then
        exit 0
    fi
    
    # 仅检查模式
    if [ "$CHECK_ONLY" = true ]; then
        log_info "仅检查模式，不执行升级"
        exit 0
    fi
    
    # 确认升级
    if [ "$QUICK_MODE" != true ]; then
        echo ""
        read -p "是否继续升级? (y/N) " confirm
        if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
            log_info "已取消升级"
            exit 0
        fi
    fi
    
    # 备份
    if [ "$SKIP_BACKUP" != true ]; then
        if [ "$QUICK_MODE" = true ]; then
            do_backup
        else
            read -p "升级前是否备份数据? (Y/n) " backup_confirm
            if [ "$backup_confirm" != "n" ] && [ "$backup_confirm" != "N" ]; then
                do_backup
            fi
        fi
    fi
    
    # 执行升级
    do_upgrade
}

main "$@"
