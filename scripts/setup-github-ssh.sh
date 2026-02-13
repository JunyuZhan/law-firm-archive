#!/bin/bash
# =====================================================
# GitHub 私有仓库 SSH 配置脚本
# =====================================================
# 用途: 自动配置服务器访问私有 GitHub 仓库的 SSH 密钥
# 用法: ./scripts/setup-github-ssh.sh
# =====================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# 工具函数
log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; }

print_banner() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}    ${BOLD}GitHub 私有仓库 SSH 配置向导${NC}                        ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# 检查是否已有 SSH 密钥
check_existing_key() {
    local key_file="$1"
    if [ -f "$key_file" ]; then
        return 0
    else
        return 1
    fi
}

# 生成 SSH 密钥
generate_ssh_key() {
    local key_file="$1"
    local key_type="$2"
    
    log_info "正在生成 SSH 密钥..."
    
    if [ "$key_type" = "ed25519" ]; then
        ssh-keygen -t ed25519 -C "deploy@law-firm-$(hostname)" -f "$key_file" -N "" <<< "y" 2>/dev/null || {
            log_warn "ed25519 不支持，改用 RSA..."
            ssh-keygen -t rsa -b 4096 -C "deploy@law-firm-$(hostname)" -f "$key_file" -N "" <<< "y"
        }
    else
        ssh-keygen -t rsa -b 4096 -C "deploy@law-firm-$(hostname)" -f "$key_file" -N "" <<< "y"
    fi
    
    if [ -f "$key_file" ]; then
        log_success "SSH 密钥已生成: $key_file"
        return 0
    else
        log_error "SSH 密钥生成失败"
        return 1
    fi
}

# 配置 SSH config
setup_ssh_config() {
    local key_file="$1"
    local ssh_config="$HOME/.ssh/config"
    
    log_info "配置 SSH config..."
    
    # 创建 .ssh 目录（如果不存在）
    mkdir -p "$HOME/.ssh"
    chmod 700 "$HOME/.ssh"
    
    # 备份现有 config（如果存在）
    if [ -f "$ssh_config" ]; then
        cp "$ssh_config" "${ssh_config}.backup.$(date +%Y%m%d_%H%M%S)"
        log_info "已备份现有 SSH config"
    fi
    
    # 检查是否已有 github.com 配置
    if grep -q "Host github.com" "$ssh_config" 2>/dev/null; then
        log_warn "SSH config 中已存在 github.com 配置"
        read -p "是否覆盖现有配置？(y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "跳过 SSH config 配置"
            return 0
        fi
        # 删除旧的 github.com 配置
        sed -i.bak '/^Host github.com$/,/^$/d' "$ssh_config" 2>/dev/null || true
    fi
    
    # 添加新的配置
    {
        echo ""
        echo "# GitHub configuration for law-firm deployment"
        echo "Host github.com"
        echo "    HostName github.com"
        echo "    User git"
        echo "    IdentityFile $key_file"
        echo "    IdentitiesOnly yes"
        echo "    StrictHostKeyChecking accept-new"
    } >> "$ssh_config"
    
    chmod 600 "$ssh_config"
    log_success "SSH config 已配置"
}

# 测试 SSH 连接
test_ssh_connection() {
    log_info "测试 SSH 连接..."
    
    if ssh -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
        log_success "SSH 连接测试成功！"
        return 0
    else
        log_warn "SSH 连接测试失败，可能需要先将公钥添加到 GitHub"
        return 1
    fi
}

# 更新 Git 远程 URL
update_git_remote() {
    local repo_path="${1:-.}"
    
    if [ ! -d "$repo_path/.git" ]; then
        log_warn "未找到 Git 仓库，跳过远程 URL 更新"
        return 0
    fi
    
    cd "$repo_path"
    
    local current_url=$(git remote get-url origin 2>/dev/null || echo "")
    
    if [ -z "$current_url" ]; then
        log_warn "未找到 origin 远程仓库"
        return 0
    fi
    
    # 如果已经是 SSH URL，跳过
    if [[ "$current_url" == git@github.com:* ]]; then
        log_info "远程 URL 已经是 SSH 格式，无需更新"
        return 0
    fi
    
    # 从 HTTPS URL 提取仓库路径
    local repo_path_in_url
    if [[ "$current_url" == https://github.com/* ]]; then
        repo_path_in_url=$(echo "$current_url" | sed 's|https://github.com/||' | sed 's|\.git$||')
    elif [[ "$current_url" == https://*@github.com/* ]]; then
        repo_path_in_url=$(echo "$current_url" | sed 's|https://.*@github.com/||' | sed 's|\.git$||')
    else
        log_warn "无法识别远程 URL 格式: $current_url"
        read -p "请输入 GitHub 仓库路径 (格式: username/repo): " repo_path_in_url
    fi
    
    if [ -n "$repo_path_in_url" ]; then
        local new_url="git@github.com:${repo_path_in_url}.git"
        log_info "更新远程 URL: $current_url -> $new_url"
        git remote set-url origin "$new_url"
        log_success "远程 URL 已更新"
    fi
}

# 显示公钥
show_public_key() {
    local key_file="$1"
    local pub_key_file="${key_file}.pub"
    
    if [ ! -f "$pub_key_file" ]; then
        log_error "公钥文件不存在: $pub_key_file"
        return 1
    fi
    
    echo ""
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}请将以下公钥添加到 GitHub：${NC}"
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    cat "$pub_key_file"
    echo ""
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${BOLD}添加方式：${NC}"
    echo -e "  ${GREEN}方式 1 (推荐):${NC} 添加为 Deploy Key（只读权限）"
    echo -e "    1. 访问: ${CYAN}https://github.com/junyuzhan/law-firm/settings/keys${NC}"
    echo -e "    2. 点击 \"Add deploy key\""
    echo -e "    3. 粘贴上面的公钥"
    echo -e "    4. 添加标题（如: Production Server）"
    echo -e "    5. ${BOLD}取消勾选 \"Allow write access\"${NC}（只读即可）"
    echo -e "    6. 点击 \"Add key\""
    echo ""
    echo -e "  ${GREEN}方式 2:${NC} 添加到个人账户 SSH Keys"
    echo -e "    1. 访问: ${CYAN}https://github.com/settings/keys${NC}"
    echo -e "    2. 点击 \"New SSH key\""
    echo -e "    3. 粘贴上面的公钥"
    echo -e "    4. 添加标题（如: Law Firm Server）"
    echo -e "    5. 点击 \"Add SSH key\""
    echo ""
    read -p "添加完成后，按 Enter 继续..."
}

# 主函数
main() {
    print_banner
    
    # 确定密钥文件路径
    local key_file_ed25519="$HOME/.ssh/id_ed25519_deploy"
    local key_file_rsa="$HOME/.ssh/id_rsa_deploy"
    local key_file=""
    
    # 检查是否已有密钥
    if check_existing_key "$key_file_ed25519"; then
        log_info "发现现有 SSH 密钥: $key_file_ed25519"
        read -p "是否使用现有密钥？(Y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Nn]$ ]]; then
            key_file="$key_file_ed25519"
        fi
    elif check_existing_key "$key_file_rsa"; then
        log_info "发现现有 SSH 密钥: $key_file_rsa"
        read -p "是否使用现有密钥？(Y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Nn]$ ]]; then
            key_file="$key_file_rsa"
        fi
    fi
    
    # 如果没有现有密钥或用户选择生成新的
    if [ -z "$key_file" ]; then
        log_info "将生成新的 SSH 密钥"
        key_file="$key_file_ed25519"
        
        if ! generate_ssh_key "$key_file" "ed25519"; then
            # 如果 ed25519 失败，尝试 RSA
            key_file="$key_file_rsa"
            generate_ssh_key "$key_file" "rsa" || {
                log_error "无法生成 SSH 密钥"
                exit 1
            }
        fi
    fi
    
    # 显示公钥并提示添加到 GitHub
    show_public_key "$key_file"
    
    # 配置 SSH config
    setup_ssh_config "$key_file"
    
    # 测试 SSH 连接
    echo ""
    log_info "测试 SSH 连接..."
    if test_ssh_connection; then
        log_success "SSH 配置成功！"
    else
        log_warn "SSH 连接测试失败"
        echo ""
        echo -e "${YELLOW}可能的原因：${NC}"
        echo "  1. 公钥尚未添加到 GitHub"
        echo "  2. GitHub 服务器暂时不可用"
        echo ""
        echo -e "${BOLD}请确认：${NC}"
        echo "  - 公钥已正确添加到 GitHub"
        echo "  - 网络连接正常"
        echo ""
        read -p "是否继续配置 Git 远程 URL？(y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "配置已保存，稍后可以手动测试"
            exit 0
        fi
    fi
    
    # 更新 Git 远程 URL
    echo ""
    log_info "检查并更新 Git 远程 URL..."
    
    # 尝试在当前目录和常见部署目录查找仓库
    local repo_paths=("." "$HOME/law-firm" "/opt/law-firm")
    local found_repo=false
    
    for repo_path in "${repo_paths[@]}"; do
        if [ -d "$repo_path/.git" ]; then
            log_info "找到 Git 仓库: $repo_path"
            update_git_remote "$repo_path"
            found_repo=true
            break
        fi
    done
    
    if [ "$found_repo" = false ]; then
        read -p "请输入项目路径（留空跳过）: " custom_path
        if [ -n "$custom_path" ] && [ -d "$custom_path/.git" ]; then
            update_git_remote "$custom_path"
        else
            log_warn "未找到 Git 仓库，请稍后手动更新远程 URL"
        fi
    fi
    
    # 最终测试
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}配置完成！${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${BOLD}验证配置：${NC}"
    echo "  1. 测试 SSH 连接: ${CYAN}ssh -T git@github.com${NC}"
    echo "  2. 测试 git pull: ${CYAN}cd /opt/law-firm && git pull origin main${NC}"
    echo ""
    echo -e "${BOLD}如果遇到问题，请参考：${NC}"
    echo "  ${CYAN}docs/GITHUB_PRIVATE_REPO_SETUP.md${NC}"
    echo ""
}

# 执行主函数
main "$@"
