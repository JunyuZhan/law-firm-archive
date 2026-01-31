#!/bin/bash
# =====================================================
# Keepalived 自动安装和配置脚本
# =====================================================
# 用法:
#   主服务器: ./install-keepalived.sh master <虚拟IP> <网卡名称>
#   从服务器: ./install-keepalived.sh slave <虚拟IP> <网卡名称> <认证密码>
#
# 示例:
#   主服务器: ./install-keepalived.sh master 192.168.50.100 eth0
#   从服务器: ./install-keepalived.sh slave 192.168.50.100 eth0 mypassword123
# =====================================================

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# 计算项目根目录：scripts/ops/keepalived -> scripts/ops -> scripts -> 项目根目录
PROJECT_ROOT="$(dirname "$(dirname "$(dirname "$SCRIPT_DIR")")")"

# 显示使用说明
show_usage() {
    echo -e "${CYAN}用法:${NC}"
    echo "  ./install-keepalived.sh master <虚拟IP> <网卡名称>"
    echo "  ./install-keepalived.sh slave <虚拟IP> <网卡名称> <认证密码>"
    echo ""
    echo -e "${CYAN}参数说明:${NC}"
    echo "  master/slave    服务器角色"
    echo "  虚拟IP          虚拟IP地址（例如：192.168.50.100）"
    echo "  网卡名称        网络接口名称（使用 ip addr 查看，通常是 eth0 或 ens33）"
    echo "  认证密码        主从服务器必须一致（仅从服务器需要）"
    echo ""
    echo -e "${CYAN}示例:${NC}"
    echo "  ./install-keepalived.sh master 192.168.50.100 eth0"
    echo "  ./install-keepalived.sh slave 192.168.50.100 eth0 mypassword123"
}

# 检查参数
if [ $# -lt 3 ]; then
    echo -e "${RED}错误: 参数不足${NC}"
    show_usage
    exit 1
fi

ROLE="$1"
VIP="$2"
INTERFACE="$3"

# 生成随机密码（如果未提供）
if [ -z "${4:-}" ]; then
    # 尝试使用 openssl，如果不存在则使用 /dev/urandom
    if command -v openssl > /dev/null 2>&1; then
        AUTH_PASS="keepalived_password_$(openssl rand -hex 8)"
    else
        # 使用 /dev/urandom 生成随机密码
        AUTH_PASS="keepalived_password_$(head -c 16 /dev/urandom | base64 | tr -d "=+/" | cut -c1-16)"
    fi
    GENERATED_PASSWORD=true
else
    AUTH_PASS="$4"
    GENERATED_PASSWORD=false
fi

# 验证角色
if [[ ! "$ROLE" =~ ^(master|slave)$ ]]; then
    echo -e "${RED}错误: 无效的角色 '$ROLE'，必须是 master 或 slave${NC}"
    exit 1
fi

# 验证IP格式
if ! echo "$VIP" | grep -qE '^([0-9]{1,3}\.){3}[0-9]{1,3}/[0-9]{1,2}$'; then
    # 如果没有子网掩码，自动添加 /24
    if echo "$VIP" | grep -qE '^([0-9]{1,3}\.){3}[0-9]{1,3}$'; then
        VIP="${VIP}/24"
    else
        echo -e "${RED}错误: 无效的IP地址格式: $VIP${NC}"
        exit 1
    fi
fi

# 验证网卡是否存在
if ! ip link show "$INTERFACE" > /dev/null 2>&1; then
    echo -e "${RED}错误: 网卡 '$INTERFACE' 不存在${NC}"
    echo -e "${YELLOW}提示: 使用 'ip addr' 查看可用的网卡${NC}"
    exit 1
fi

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}Keepalived 自动安装和配置${NC}"
echo -e "${BLUE}角色: $ROLE${NC}"
echo -e "${BLUE}虚拟IP: $VIP${NC}"
echo -e "${BLUE}网卡: $INTERFACE${NC}"
echo -e "${BLUE}认证密码: $AUTH_PASS${NC}"
if [ "$GENERATED_PASSWORD" = "true" ]; then
    echo -e "${YELLOW}⚠️  注意: 使用了随机生成的密码${NC}"
fi
echo -e "${BLUE}==========================================${NC}"
echo ""

# 检查是否为 root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}错误: 需要 root 权限运行此脚本${NC}"
    echo -e "${YELLOW}提示: 使用 sudo 运行${NC}"
    exit 1
fi

# 步骤1：安装 Keepalived
echo -e "${CYAN}[步骤 1/5] 安装 Keepalived...${NC}"

# 检查是否已安装
if command -v keepalived > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  Keepalived 已安装，跳过安装步骤${NC}"
else
    if command -v apt-get > /dev/null 2>&1; then
        apt-get update
        apt-get install -y keepalived || {
            echo -e "${RED}错误: Keepalived 安装失败${NC}"
            exit 1
        }
    elif command -v yum > /dev/null 2>&1; then
        yum install -y keepalived || {
            echo -e "${RED}错误: Keepalived 安装失败${NC}"
            exit 1
        }
    else
        echo -e "${RED}错误: 无法识别包管理器（apt-get 或 yum）${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✓ Keepalived 安装完成${NC}"
echo ""

# 步骤2：复制脚本文件
echo -e "${CYAN}[步骤 2/5] 复制脚本文件...${NC}"

# 检查必需的脚本文件是否存在
for script in check-postgres.sh postgres-master.sh postgres-slave.sh postgres-fault.sh check-role.sh; do
    if [ ! -f "$SCRIPT_DIR/$script" ]; then
        echo -e "${RED}错误: 脚本文件不存在: $SCRIPT_DIR/$script${NC}"
        exit 1
    fi
done

# 复制 Keepalived 使用的脚本到系统目录
cp "$SCRIPT_DIR/check-postgres.sh" /usr/local/bin/ || {
    echo -e "${RED}错误: 无法复制脚本文件到 /usr/local/bin/${NC}"
    exit 1
}
cp "$SCRIPT_DIR/postgres-master.sh" /usr/local/bin/
cp "$SCRIPT_DIR/postgres-slave.sh" /usr/local/bin/
cp "$SCRIPT_DIR/postgres-fault.sh" /usr/local/bin/
cp "$SCRIPT_DIR/check-role.sh" /usr/local/bin/

chmod +x /usr/local/bin/check-postgres.sh
chmod +x /usr/local/bin/postgres-master.sh
chmod +x /usr/local/bin/postgres-slave.sh
chmod +x /usr/local/bin/postgres-fault.sh
chmod +x /usr/local/bin/check-role.sh
echo -e "${GREEN}✓ 脚本文件复制完成${NC}"
echo ""

# 步骤3：生成配置文件
echo -e "${CYAN}[步骤 3/5] 生成 Keepalived 配置文件...${NC}"
if [ "$ROLE" == "master" ]; then
    CONFIG_TEMPLATE="$SCRIPT_DIR/keepalived-master.conf.example"
    PRIORITY=100
else
    CONFIG_TEMPLATE="$SCRIPT_DIR/keepalived-slave.conf.example"
    PRIORITY=90
fi

# 检查模板文件是否存在
if [ ! -f "$CONFIG_TEMPLATE" ]; then
    echo -e "${RED}错误: 配置文件模板不存在: $CONFIG_TEMPLATE${NC}"
    exit 1
fi

# 备份现有配置文件（如果存在）
if [ -f /etc/keepalived/keepalived.conf ]; then
    BACKUP_FILE="/etc/keepalived/keepalived.conf.backup.$(date +%Y%m%d_%H%M%S)"
    cp /etc/keepalived/keepalived.conf "$BACKUP_FILE"
    echo -e "${YELLOW}⚠️  已备份现有配置文件到: $BACKUP_FILE${NC}"
fi

# 读取模板并替换变量
sed -e "s|eth0|$INTERFACE|g" \
    -e "s|your_secure_password_here|$AUTH_PASS|g" \
    -e "s|192.168.50.100/24|$VIP|g" \
    -e "s|priority 100|priority $PRIORITY|g" \
    -e "s|priority 90|priority $PRIORITY|g" \
    "$CONFIG_TEMPLATE" > /etc/keepalived/keepalived.conf || {
    echo -e "${RED}错误: 无法生成配置文件${NC}"
    exit 1
}

echo -e "${GREEN}✓ 配置文件生成完成${NC}"
echo ""

# 步骤4：检查配置
echo -e "${CYAN}[步骤 4/5] 检查配置...${NC}"
if keepalived -t; then
    echo -e "${GREEN}✓ 配置检查通过${NC}"
else
    echo -e "${RED}✗ 配置检查失败，请检查配置文件${NC}"
    exit 1
fi
echo ""

# 步骤5：启动服务
echo -e "${CYAN}[步骤 5/5] 启动 Keepalived 服务...${NC}"

# 如果服务已经在运行，先停止
if systemctl is-active --quiet keepalived 2>/dev/null; then
    echo -e "${YELLOW}⚠️  Keepalived 服务正在运行，先停止...${NC}"
    systemctl stop keepalived
    sleep 1
fi

systemctl enable keepalived || {
    echo -e "${RED}错误: 无法设置 Keepalived 开机自启${NC}"
    exit 1
}

systemctl restart keepalived || {
    echo -e "${RED}错误: Keepalived 服务启动失败${NC}"
    echo -e "${YELLOW}查看日志: sudo journalctl -u keepalived -n 50${NC}"
    exit 1
}

sleep 2

if systemctl is-active --quiet keepalived; then
    echo -e "${GREEN}✓ Keepalived 服务启动成功${NC}"
else
    echo -e "${RED}✗ Keepalived 服务启动失败${NC}"
    echo -e "${YELLOW}查看日志: sudo journalctl -u keepalived -n 50${NC}"
    exit 1
fi
echo ""

# 完成
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}✓ Keepalived 安装和配置完成！${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""

# 显示配置信息
echo -e "${CYAN}配置信息:${NC}"
echo "  角色: $ROLE"
echo "  虚拟IP: $VIP"
echo "  网卡: $INTERFACE"
echo "  优先级: $PRIORITY"
echo "  认证密码: $AUTH_PASS"
if [ "$GENERATED_PASSWORD" = "true" ]; then
    echo ""
    echo -e "${YELLOW}⚠️  重要: 这是随机生成的密码，请记录下来！${NC}"
    echo -e "${YELLOW}⚠️  从服务器安装时必须使用相同的密码！${NC}"
fi
if [ "$ROLE" == "slave" ]; then
    echo ""
    echo -e "${YELLOW}⚠️  重要: 请确保主服务器的认证密码与此密码一致！${NC}"
fi
echo ""

# 显示验证命令
echo -e "${CYAN}验证命令:${NC}"
echo "  查看服务状态: sudo systemctl status keepalived"
echo "  查看虚拟IP: ip addr show $INTERFACE"
echo "  查看日志: sudo journalctl -u keepalived -f"
echo "  查看故障切换日志: tail -f /var/log/postgres-failover.log"
echo "  检查主从角色: sudo /usr/local/bin/check-role.sh"
echo ""

# 显示相关文档
echo -e "${CYAN}相关文档:${NC}"
echo "  快速开始: $PROJECT_ROOT/scripts/ops/keepalived/QUICK-START.md"
echo "  故障恢复: $PROJECT_ROOT/scripts/ops/keepalived/FAILOVER-RECOVERY.md"
echo "  脚本说明: $PROJECT_ROOT/scripts/ops/keepalived/README.md"
echo ""
