#!/bin/bash
#
# 升级服务器 - 监听 HTTP 请求并执行升级
# 用于支持后台"一键升级"功能
#
# 使用方法：
#   1. 在宿主机上运行此脚本（不是在 Docker 容器内）
#   2. 配置 .env 中的 UPGRADE_WEBHOOK_SECRET
#   3. 在后台点击"一键升级"按钮
#
# 启动：./scripts/upgrade-server.sh start
# 停止：./scripts/upgrade-server.sh stop
# 状态：./scripts/upgrade-server.sh status

set -e

# 配置
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
LOG_FILE="$PROJECT_DIR/logs/upgrade-server.log"
PID_FILE="$PROJECT_DIR/logs/upgrade-server.pid"
PORT="${UPGRADE_SERVER_PORT:-9999}"
SECRET="${UPGRADE_WEBHOOK_SECRET:-}"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 确保日志目录存在
mkdir -p "$(dirname "$LOG_FILE")"

log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 执行升级
do_upgrade() {
    log "${GREEN}开始升级...${NC}"
    
    cd "$PROJECT_DIR"
    
    # 1. 备份当前配置
    log "备份配置..."
    if [ -f ".env" ]; then
        cp .env ".env.backup.$(date +%Y%m%d%H%M%S)"
    fi
    
    # 2. 拉取最新代码
    log "拉取最新代码..."
    git fetch origin main
    git pull origin main
    
    # 3. 重新构建并启动
    log "重新构建容器..."
    cd docker
    docker compose down
    docker compose up -d --build
    
    # 4. 等待服务启动
    log "等待服务启动..."
    sleep 10
    
    # 5. 健康检查
    if curl -s -f http://localhost/api/health > /dev/null 2>&1; then
        log "${GREEN}升级成功！${NC}"
        echo "SUCCESS"
    else
        log "${YELLOW}服务可能还在启动中，请稍后检查${NC}"
        echo "PENDING"
    fi
}

# HTTP 服务器
start_server() {
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo -e "${YELLOW}升级服务器已在运行 (PID: $(cat "$PID_FILE"))${NC}"
        return 1
    fi
    
    echo -e "${GREEN}启动升级服务器，端口: $PORT${NC}"
    
    # 使用 ncat (netcat) 创建简易 HTTP 服务器
    (
        while true; do
            # 读取 HTTP 请求
            REQUEST=$(echo -e "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\n\r\n" | \
                nc -l -p "$PORT" -q 1 2>/dev/null | head -20)
            
            # 检查是否是升级请求
            if echo "$REQUEST" | grep -q "POST /upgrade"; then
                # 验证密钥
                AUTH_HEADER=$(echo "$REQUEST" | grep -i "X-Upgrade-Secret:" | tr -d '\r')
                PROVIDED_SECRET=$(echo "$AUTH_HEADER" | sed 's/.*: //')
                
                if [ -n "$SECRET" ] && [ "$PROVIDED_SECRET" != "$SECRET" ]; then
                    log "${RED}升级请求被拒绝：密钥错误${NC}"
                    continue
                fi
                
                log "收到升级请求"
                RESULT=$(do_upgrade 2>&1)
                log "升级结果: $RESULT"
            fi
        done
    ) >> "$LOG_FILE" 2>&1 &
    
    echo $! > "$PID_FILE"
    echo -e "${GREEN}升级服务器已启动 (PID: $!)${NC}"
    echo -e "日志文件: $LOG_FILE"
}

stop_server() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            kill "$PID"
            rm -f "$PID_FILE"
            echo -e "${GREEN}升级服务器已停止${NC}"
        else
            rm -f "$PID_FILE"
            echo -e "${YELLOW}进程已不存在${NC}"
        fi
    else
        echo -e "${YELLOW}升级服务器未运行${NC}"
    fi
}

status_server() {
    if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo -e "${GREEN}升级服务器运行中 (PID: $(cat "$PID_FILE"))${NC}"
        echo -e "端口: $PORT"
        echo -e "日志: $LOG_FILE"
    else
        echo -e "${YELLOW}升级服务器未运行${NC}"
    fi
}

# 显示帮助
show_help() {
    cat << EOF
升级服务器 - 支持后台一键升级功能

用法：
    $0 <command>

命令：
    start       启动升级服务器
    stop        停止升级服务器
    status      查看服务器状态
    upgrade     直接执行升级（不启动服务器）
    help        显示此帮助

环境变量：
    UPGRADE_SERVER_PORT     监听端口（默认: 9999）
    UPGRADE_WEBHOOK_SECRET  升级密钥（可选，用于验证请求）

示例：
    # 启动服务器
    UPGRADE_WEBHOOK_SECRET=your-secret ./scripts/upgrade-server.sh start
    
    # 直接升级
    ./scripts/upgrade-server.sh upgrade

EOF
}

# 主入口
case "${1:-help}" in
    start)
        start_server
        ;;
    stop)
        stop_server
        ;;
    status)
        status_server
        ;;
    upgrade)
        do_upgrade
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        echo -e "${RED}未知命令: $1${NC}"
        show_help
        exit 1
        ;;
esac
