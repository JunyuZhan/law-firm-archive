#!/bin/bash
# =====================================================
# 强制更新服务器代码脚本
# =====================================================
# 用法:
#   方式1: 在服务器上直接运行
#     ./scripts/force-update-server.sh
#
#   方式2: 从本地执行（SSH到服务器）
#     ./scripts/force-update-server.sh <服务器IP> [用户名]
#     例如: ./scripts/force-update-server.sh 192.168.1.100 root
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查是否通过SSH执行
if [ -n "$1" ]; then
    # 从本地SSH到服务器执行
    SERVER_IP="$1"
    SERVER_USER="${2:-root}"
    PROJECT_PATH="${3:-/opt/law-firm}"
    
    log_info "准备在服务器上强制更新代码: ${SERVER_USER}@${SERVER_IP}"
    
    # 检查 SSH 连接
    log_info "检查 SSH 连接..."
    if ! ssh -o ConnectTimeout=5 "${SERVER_USER}@${SERVER_IP}" "echo 'SSH连接成功'" &>/dev/null; then
        log_error "无法连接到服务器，请检查："
        echo "  1. 服务器IP是否正确: ${SERVER_IP}"
        echo "  2. SSH服务是否运行"
        echo "  3. 防火墙是否开放22端口"
        echo "  4. SSH密钥是否配置"
        exit 1
    fi
    log_success "SSH 连接正常"
    
    # 在服务器上执行更新
    ssh "${SERVER_USER}@${SERVER_IP}" << ENDSSH
        set -e
        cd ${PROJECT_PATH}
        
        echo "=============================================="
        echo "  强制更新服务器代码"
        echo "=============================================="
        echo ""
        
        # 检查是否是git仓库
        if [ ! -d .git ]; then
            echo "❌ 错误: 当前目录不是git仓库"
            exit 1
        fi
        
        # 显示当前状态
        echo "📋 当前分支:"
        git branch --show-current
        echo ""
        
        echo "📋 当前提交:"
        git log -1 --oneline
        echo ""
        
        # 检查是否有未提交的更改
        if ! git diff --quiet || ! git diff --cached --quiet; then
            echo "⚠️  警告: 检测到未提交的更改"
            echo ""
            echo "未提交的文件:"
            git status --short
            echo ""
            echo "这些更改将被丢弃！"
            echo ""
            read -p "是否继续？(y/N): " -n 1 -r
            echo
            if [[ ! \$REPLY =~ ^[Yy]$ ]]; then
                echo "❌ 已取消"
                exit 0
            fi
        fi
        
        # 获取远程更新
        echo "📥 获取远程更新..."
        git fetch origin
        
        # 显示远程更新
        echo ""
        echo "📋 远程更新:"
        LOCAL_COMMIT=\$(git rev-parse HEAD)
        REMOTE_COMMIT=\$(git rev-parse origin/main 2>/dev/null || git rev-parse origin/master 2>/dev/null)
        
        if [ "\$LOCAL_COMMIT" = "\$REMOTE_COMMIT" ]; then
            echo "✅ 代码已是最新，无需更新"
            exit 0
        fi
        
        echo "本地: \$(git log -1 --oneline \$LOCAL_COMMIT)"
        echo "远程: \$(git log -1 --oneline \$REMOTE_COMMIT)"
        echo ""
        
        # 强制重置到远程分支
        echo "🔄 强制重置到远程分支..."
        BRANCH=\$(git branch --show-current)
        REMOTE_BRANCH="origin/\${BRANCH}"
        
        # 如果当前分支是main或master，使用对应的远程分支
        if [ "\$BRANCH" = "main" ] || [ "\$BRANCH" = "master" ]; then
            if git rev-parse origin/main &>/dev/null; then
                REMOTE_BRANCH="origin/main"
            elif git rev-parse origin/master &>/dev/null; then
                REMOTE_BRANCH="origin/master"
            fi
        fi
        
        git reset --hard \$REMOTE_BRANCH
        
        # 清理未跟踪的文件（可选）
        echo ""
        read -p "是否清理未跟踪的文件？(y/N): " -n 1 -r
        echo
        if [[ \$REPLY =~ ^[Yy]$ ]]; then
            echo "🧹 清理未跟踪的文件..."
            git clean -fd
        fi
        
        # 显示更新后的状态
        echo ""
        echo "✅ 代码更新完成！"
        echo ""
        echo "📋 更新后的提交:"
        git log -1 --oneline
        echo ""
        echo "📋 更新内容:"
        git log \$LOCAL_COMMIT..HEAD --oneline | head -10
        echo ""
ENDSSH
    
    log_success "服务器代码更新完成！"
    
else
    # 在服务器上直接执行
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
    
    cd "$PROJECT_ROOT"
    
    echo "=============================================="
    echo "  强制更新服务器代码"
    echo "=============================================="
    echo ""
    
    # 检查是否是git仓库
    if [ ! -d .git ]; then
        log_error "当前目录不是git仓库"
        exit 1
    fi
    
    # 显示当前状态
    log_info "当前分支:"
    git branch --show-current
    echo ""
    
    log_info "当前提交:"
    git log -1 --oneline
    echo ""
    
    # 检查是否有未提交的更改
    if ! git diff --quiet || ! git diff --cached --quiet; then
        log_warn "检测到未提交的更改"
        echo ""
        echo "未提交的文件:"
        git status --short
        echo ""
        log_warn "这些更改将被丢弃！"
        echo ""
        read -p "是否继续？(y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "已取消"
            exit 0
        fi
    fi
    
    # 获取远程更新
    log_info "获取远程更新..."
    git fetch origin
    
    # 显示远程更新
    echo ""
    log_info "远程更新:"
    LOCAL_COMMIT=$(git rev-parse HEAD)
    REMOTE_COMMIT=$(git rev-parse origin/main 2>/dev/null || git rev-parse origin/master 2>/dev/null)
    
    if [ "$LOCAL_COMMIT" = "$REMOTE_COMMIT" ]; then
        log_success "代码已是最新，无需更新"
        exit 0
    fi
    
    echo "本地: $(git log -1 --oneline $LOCAL_COMMIT)"
    echo "远程: $(git log -1 --oneline $REMOTE_COMMIT)"
    echo ""
    
    # 强制重置到远程分支
    log_info "强制重置到远程分支..."
    BRANCH=$(git branch --show-current)
    REMOTE_BRANCH="origin/${BRANCH}"
    
    # 如果当前分支是main或master，使用对应的远程分支
    if [ "$BRANCH" = "main" ] || [ "$BRANCH" = "master" ]; then
        if git rev-parse origin/main &>/dev/null; then
            REMOTE_BRANCH="origin/main"
        elif git rev-parse origin/master &>/dev/null; then
            REMOTE_BRANCH="origin/master"
        fi
    fi
    
    git reset --hard $REMOTE_BRANCH
    
    # 清理未跟踪的文件（可选）
    echo ""
    read -p "是否清理未跟踪的文件？(y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "清理未跟踪的文件..."
        git clean -fd
    fi
    
    # 显示更新后的状态
    echo ""
    log_success "代码更新完成！"
    echo ""
    log_info "更新后的提交:"
    git log -1 --oneline
    echo ""
    log_info "更新内容:"
    git log $LOCAL_COMMIT..HEAD --oneline | head -10
    echo ""
fi
