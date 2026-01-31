#!/bin/bash
# =====================================================
# 律师事务所管理系统 - Docker Swarm 分布式部署脚本
# =====================================================
# 用法:
#   ./deploy-swarm.sh init          # 初始化 Swarm 集群（Manager 节点）
#   ./deploy-swarm.sh join <token> <manager-ip>  # 加入集群（Worker 节点）
#   ./deploy-swarm.sh deploy        # 部署服务栈
#   ./deploy-swarm.sh update        # 更新服务
#   ./deploy-swarm.sh scale <service> <replicas>  # 扩缩容
#   ./deploy-swarm.sh status        # 查看状态
#   ./deploy-swarm.sh logs <service>  # 查看日志
#   ./deploy-swarm.sh remove        # 删除服务栈
# =====================================================

set -e

STACK_NAME="law-firm"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DOCKER_DIR="$PROJECT_ROOT/docker"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查 Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi
}

# 检查环境变量
check_env() {
    if [ ! -f "$DOCKER_DIR/.env" ]; then
        log_warn "未找到 $DOCKER_DIR/.env 文件，正在从模板创建..."
        cp "$DOCKER_DIR/env.example" "$DOCKER_DIR/.env"
        log_error "请编辑 $DOCKER_DIR/.env 文件配置密码后重新运行"
        exit 1
    fi
    
    source "$DOCKER_DIR/.env"
    
    if [ -z "$JWT_SECRET" ] || [ "$JWT_SECRET" = "your_very_long_and_secure_jwt_secret_key_here_at_least_64_characters" ]; then
        log_error "请修改 .env 中的 JWT_SECRET"
        exit 1
    fi
    
    if [ -z "$DB_PASSWORD" ] || [ "$DB_PASSWORD" = "your_secure_db_password_here" ]; then
        log_error "请修改 .env 中的 DB_PASSWORD"
        exit 1
    fi
}

# 初始化 Swarm 集群
cmd_init() {
    log_info "初始化 Docker Swarm 集群..."
    
    # 检查是否已经是 Swarm 节点
    if docker info 2>/dev/null | grep -q "Swarm: active"; then
        log_warn "当前节点已经是 Swarm 集群的一部分"
        docker node ls
        return
    fi
    
    # 获取本机 IP
    local ip=$(hostname -I | awk '{print $1}')
    
    docker swarm init --advertise-addr "$ip"
    
    echo ""
    log_success "Swarm 集群初始化完成！"
    echo ""
    log_info "当前节点 IP: $ip"
    echo ""
    log_info "要添加 Worker 节点，请在其他机器上运行："
    docker swarm join-token worker
    echo ""
    log_info "要添加 Manager 节点，请运行："
    docker swarm join-token manager
}

# 加入 Swarm 集群
cmd_join() {
    local token=$1
    local manager_ip=$2
    
    if [ -z "$token" ] || [ -z "$manager_ip" ]; then
        log_error "用法: $0 join <token> <manager-ip>"
        exit 1
    fi
    
    log_info "加入 Swarm 集群..."
    docker swarm join --token "$token" "$manager_ip:2377"
    log_success "已成功加入集群！"
}

# 构建镜像
build_images() {
    log_info "构建 Docker 镜像..."
    
    cd "$DOCKER_DIR"
    
    # 构建前端镜像
    log_info "构建前端镜像..."
    docker build -t law-firm-frontend:latest -f ../frontend/scripts/deploy/Dockerfile ../frontend
    
    # 构建后端镜像
    log_info "构建后端镜像..."
    docker build -t law-firm-backend:latest -f ../docker/Dockerfile.prod ../backend
    
    # 构建 OCR 镜像
    log_info "构建 OCR 镜像..."
    docker build -t law-firm-ocr:latest -f ocr/Dockerfile ocr/
    
    # 构建 OnlyOffice 镜像
    log_info "构建 OnlyOffice 镜像..."
    docker build -t law-firm-onlyoffice:latest -f onlyoffice/Dockerfile onlyoffice/
    
    log_success "所有镜像构建完成！"
}

# 部署服务栈
cmd_deploy() {
    check_env
    
    log_info "部署服务栈到 Swarm 集群..."
    
    # 检查是否是 Swarm Manager
    if ! docker info 2>/dev/null | grep -q "Is Manager: true"; then
        log_error "当前节点不是 Swarm Manager，请在 Manager 节点上运行此命令"
        exit 1
    fi
    
    # 构建镜像
    build_images
    
    cd "$DOCKER_DIR"
    
    # 加载环境变量并部署
    set -a
    source .env
    set +a
    
    docker stack deploy -c docker-compose.swarm.yml "$STACK_NAME"
    
    echo ""
    log_success "服务栈部署完成！"
    echo ""
    log_info "等待服务启动..."
    sleep 10
    
    cmd_status
    
    echo ""
    echo "=============================================="
    echo "    分布式部署完成！"
    echo "=============================================="
    echo ""
    echo "🌐 访问地址：http://<任意节点IP>"
    echo "📦 MinIO 控制台：http://<Manager节点IP>:9001"
    echo ""
    echo "📋 查看服务: $0 status"
    echo "📋 查看日志: $0 logs <service>"
    echo "🔄 扩缩容:   $0 scale backend 3"
    echo "🛑 删除服务: $0 remove"
    echo ""
}

# 更新服务
cmd_update() {
    log_info "更新服务..."
    
    build_images
    
    # 强制更新服务
    docker service update --force "${STACK_NAME}_frontend"
    docker service update --force "${STACK_NAME}_backend"
    
    log_success "服务更新完成！"
}

# 扩缩容
cmd_scale() {
    local service=$1
    local replicas=$2
    
    if [ -z "$service" ] || [ -z "$replicas" ]; then
        log_error "用法: $0 scale <service> <replicas>"
        log_info "可用服务: frontend, backend, paddle-ocr, onlyoffice"
        exit 1
    fi
    
    log_info "将 ${service} 扩缩容到 ${replicas} 个副本..."
    docker service scale "${STACK_NAME}_${service}=${replicas}"
    log_success "扩缩容完成！"
}

# 查看状态
cmd_status() {
    log_info "Swarm 节点状态:"
    docker node ls
    echo ""
    
    log_info "服务状态:"
    docker service ls --filter "name=${STACK_NAME}"
    echo ""
    
    log_info "服务副本详情:"
    docker stack ps "$STACK_NAME" --no-trunc 2>/dev/null || true
}

# 查看日志
cmd_logs() {
    local service=$1
    
    if [ -z "$service" ]; then
        log_error "用法: $0 logs <service>"
        log_info "可用服务: frontend, backend, postgres, redis, minio, paddle-ocr, onlyoffice"
        exit 1
    fi
    
    docker service logs -f "${STACK_NAME}_${service}"
}

# 删除服务栈
cmd_remove() {
    log_warn "即将删除服务栈 ${STACK_NAME}..."
    read -p "确认删除? (y/N) " confirm
    
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        docker stack rm "$STACK_NAME"
        log_success "服务栈已删除！"
        log_warn "数据卷未删除，如需清理请手动执行: docker volume prune"
    else
        log_info "操作已取消"
    fi
}

# 显示帮助
show_help() {
    echo "律师事务所管理系统 - Docker Swarm 分布式部署"
    echo ""
    echo "用法: $0 <命令> [参数]"
    echo ""
    echo "命令:"
    echo "  init                        初始化 Swarm 集群（Manager 节点）"
    echo "  join <token> <manager-ip>   加入集群（Worker 节点）"
    echo "  deploy                      部署服务栈"
    echo "  update                      更新服务（重新构建并部署）"
    echo "  scale <service> <replicas>  扩缩容服务"
    echo "  status                      查看集群和服务状态"
    echo "  logs <service>              查看服务日志"
    echo "  remove                      删除服务栈"
    echo "  help                        显示帮助"
    echo ""
    echo "示例:"
    echo "  $0 init                     # 在第一台服务器初始化集群"
    echo "  $0 deploy                   # 部署所有服务"
    echo "  $0 scale backend 3          # 将后端扩展到3个副本"
    echo "  $0 logs backend             # 查看后端日志"
}

# 主入口
check_docker

case "${1:-help}" in
    init)
        cmd_init
        ;;
    join)
        cmd_join "$2" "$3"
        ;;
    deploy)
        cmd_deploy
        ;;
    update)
        cmd_update
        ;;
    scale)
        cmd_scale "$2" "$3"
        ;;
    status)
        cmd_status
        ;;
    logs)
        cmd_logs "$2"
        ;;
    remove)
        cmd_remove
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "未知命令: $1"
        show_help
        exit 1
        ;;
esac

