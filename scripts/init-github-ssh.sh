#!/bin/bash
# =====================================================
# GitHub 私有仓库 SSH 初始化脚本（服务器首次配置）
# =====================================================
# 用途: 在服务器上首次配置 SSH 密钥，用于克隆私有仓库
# 用法: 
#   1. 在本地电脑上传脚本到服务器（替换 root 为你的实际用户名）:
#      scp scripts/init-github-ssh.sh root@192.168.50.10:/tmp/
#   2. SSH 登录服务器并运行:
#      ssh root@192.168.50.10
#      bash /tmp/init-github-ssh.sh
# 
#   注意: 私有仓库无法直接从 GitHub 下载脚本，需要先上传
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
    echo -e "${CYAN}║${NC}    ${BOLD}GitHub 私有仓库 SSH 初始化配置${NC}                        ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}                                                              ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# 生成 SSH 密钥
generate_ssh_key() {
    local key_file="$1"
    
    log_info "正在生成 SSH 密钥..."
    
    # 创建 .ssh 目录
    mkdir -p "$HOME/.ssh"
    chmod 700 "$HOME/.ssh"
    
    # 尝试生成 ed25519 密钥
    if ssh-keygen -t ed25519 -C "deploy@law-firm-$(hostname)" -f "$key_file" -N "" <<< "y" 2>/dev/null; then
        log_success "SSH 密钥已生成 (ed25519): $key_file"
        return 0
    else
        log_warn "ed25519 不支持，改用 RSA..."
        if ssh-keygen -t rsa -b 4096 -C "deploy@law-firm-$(hostname)" -f "$key_file" -N "" <<< "y"; then
            log_success "SSH 密钥已生成 (RSA): $key_file"
            return 0
        else
            log_error "SSH 密钥生成失败"
            return 1
        fi
    fi
}

# 配置 SSH config
setup_ssh_config() {
    local key_file="$1"
    local ssh_config="$HOME/.ssh/config"
    
    log_info "配置 SSH config..."
    
    # 备份现有 config（如果存在）
    if [ -f "$ssh_config" ]; then
        cp "$ssh_config" "${ssh_config}.backup.$(date +%Y%m%d_%H%M%S)"
        log_info "已备份现有 SSH config"
        
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
    echo -e "${BOLD}添加方式（推荐使用 Deploy Key）：${NC}"
    echo ""
    echo -e "  ${GREEN}方式 1 (推荐):${NC} 添加为 Deploy Key（只读权限）"
    echo -e "    1. 访问你的仓库设置页面："
    echo -e "       ${CYAN}https://github.com/junyuzhan/law-firm/settings/keys${NC}"
    echo -e "    2. 点击 \"Add deploy key\""
    echo -e "    3. 粘贴上面的公钥"
    echo -e "    4. 添加标题（如: Production Server - $(hostname)）"
    echo -e "    5. ${BOLD}取消勾选 \"Allow write access\"${NC}（只读即可）"
    echo -e "    6. 点击 \"Add key\""
    echo ""
    echo -e "  ${GREEN}方式 2:${NC} 添加到个人账户 SSH Keys"
    echo -e "    1. 访问: ${CYAN}https://github.com/settings/keys${NC}"
    echo -e "    2. 点击 \"New SSH key\""
    echo -e "    3. 粘贴上面的公钥"
    echo -e "    4. 添加标题（如: Law Firm Server - $(hostname)）"
    echo -e "    5. 点击 \"Add SSH key\""
    echo ""
    echo -e "${BOLD}提示：${NC}复制上面的公钥内容（从 ssh-ed25519 或 ssh-rsa 开始到邮箱结束）"
    echo ""
}

# 测试 SSH 连接
test_ssh_connection() {
    log_info "测试 SSH 连接..."
    
    if ssh -T git@github.com 2>&1 | grep -q "successfully authenticated"; then
        log_success "SSH 连接测试成功！"
        return 0
    else
        log_warn "SSH 连接测试失败"
        return 1
    fi
}

# 主函数
main() {
    print_banner
    
    # 确定密钥文件路径
    local key_file_ed25519="$HOME/.ssh/id_ed25519_deploy"
    local key_file_rsa="$HOME/.ssh/id_rsa_deploy"
    local key_file=""
    
    # 检查是否已有密钥
    if [ -f "$key_file_ed25519" ]; then
        log_info "发现现有 SSH 密钥: $key_file_ed25519"
        read -p "是否使用现有密钥？(Y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Nn]$ ]]; then
            key_file="$key_file_ed25519"
        fi
    elif [ -f "$key_file_rsa" ]; then
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
        
        if ! generate_ssh_key "$key_file"; then
            # 如果 ed25519 失败，尝试 RSA
            key_file="$key_file_rsa"
            generate_ssh_key "$key_file" || {
                log_error "无法生成 SSH 密钥"
                exit 1
            }
        fi
    fi
    
    # 显示公钥并提示添加到 GitHub
    show_public_key "$key_file"
    
    read -p "添加完成后，按 Enter 继续..."
    
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
        echo "  3. 网络连接问题"
        echo ""
        echo -e "${BOLD}请确认：${NC}"
        echo "  - 公钥已正确添加到 GitHub"
        echo "  - 网络连接正常"
        echo ""
        read -p "是否重试测试？(y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            if test_ssh_connection; then
                log_success "SSH 连接测试成功！"
            else
                log_error "连接仍然失败，请检查配置"
                exit 1
            fi
        fi
    fi
    
    # 最终说明
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}SSH 配置完成！${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "${BOLD}现在可以克隆代码了：${NC}"
    echo ""
    echo -e "  ${CYAN}git clone git@github.com:junyuzhan/law-firm.git${NC}"
    echo ""
    echo -e "或指定目录："
    echo -e "  ${CYAN}git clone git@github.com:junyuzhan/law-firm.git /opt/law-firm${NC}"
    echo ""
    echo -e "${BOLD}克隆完成后，可以运行完整配置脚本：${NC}"
    echo -e "  ${CYAN}cd /opt/law-firm && ./scripts/setup-github-ssh.sh${NC}"
    echo ""
    echo -e "${BOLD}验证配置：${NC}"
    echo "  ${CYAN}ssh -T git@github.com${NC}"
    echo ""
}

# 执行主函数
main "$@"
