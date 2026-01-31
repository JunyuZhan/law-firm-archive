#!/bin/bash
# =====================================================
# OnlyOffice 和 MinIO 集成测试脚本
# =====================================================
# 测试文档编辑功能中 OnlyOffice 和 MinIO 的配合
# =====================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

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

echo "=============================================="
echo "OnlyOffice 和 MinIO 集成测试"
echo "=============================================="
echo ""

# 1. 检查容器状态
log_info "1. 检查容器状态..."
containers=("law-firm-backend" "law-firm-minio" "onlyoffice")
all_running=true

for container in "${containers[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        log_success "  ✅ $container 正在运行"
    else
        log_error "  ❌ $container 未运行"
        all_running=false
    fi
done

if [ "$all_running" = false ]; then
    log_error "部分容器未运行，请先启动服务"
    exit 1
fi

echo ""

# 2. 测试容器间网络连通性
log_info "2. 测试容器间网络连通性..."

# OnlyOffice → Backend
log_info "   测试 OnlyOffice → Backend..."
if docker exec onlyoffice ping -c 1 backend > /dev/null 2>&1; then
    log_success "   ✅ OnlyOffice 可以访问 Backend"
else
    log_error "   ❌ OnlyOffice 无法访问 Backend"
fi

# Backend → MinIO
log_info "   测试 Backend → MinIO..."
if docker exec law-firm-backend ping -c 1 minio > /dev/null 2>&1 || \
   docker exec law-firm-backend ping -c 1 law-firm-minio > /dev/null 2>&1; then
    log_success "   ✅ Backend 可以访问 MinIO"
else
    log_error "   ❌ Backend 无法访问 MinIO"
fi

# OnlyOffice → MinIO（如果需要）
log_info "   测试 OnlyOffice → MinIO..."
if docker exec onlyoffice ping -c 1 minio > /dev/null 2>&1 || \
   docker exec onlyoffice ping -c 1 law-firm-minio > /dev/null 2>&1; then
    log_success "   ✅ OnlyOffice 可以访问 MinIO"
else
    log_warn "   ⚠️  OnlyOffice 无法访问 MinIO（如果使用 /file-proxy 接口则不需要）"
fi

echo ""

# 3. 测试 HTTP 连接
log_info "3. 测试 HTTP 连接..."

# OnlyOffice → Backend (健康检查)
log_info "   测试 OnlyOffice → Backend HTTP..."
backend_health=$(docker exec onlyoffice curl -s -o /dev/null -w "%{http_code}" http://backend:8080/api/actuator/health 2>/dev/null || echo "000")
if [ "$backend_health" = "200" ]; then
    log_success "   ✅ OnlyOffice 可以访问 Backend HTTP (200)"
else
    log_error "   ❌ OnlyOffice 无法访问 Backend HTTP (状态码: $backend_health)"
fi

# Backend → MinIO (健康检查)
log_info "   测试 Backend → MinIO HTTP..."
minio_health=$(docker exec law-firm-backend curl -s -o /dev/null -w "%{http_code}" http://minio:9000/minio/health/live 2>/dev/null || \
               docker exec law-firm-backend curl -s -o /dev/null -w "%{http_code}" http://law-firm-minio:9000/minio/health/live 2>/dev/null || echo "000")
if [ "$minio_health" = "200" ]; then
    log_success "   ✅ Backend 可以访问 MinIO HTTP (200)"
else
    log_error "   ❌ Backend 无法访问 MinIO HTTP (状态码: $minio_health)"
fi

# OnlyOffice 健康检查
log_info "   测试 OnlyOffice 健康检查..."
onlyoffice_health=$(docker exec onlyoffice curl -s -o /dev/null -w "%{http_code}" http://localhost/healthcheck 2>/dev/null || echo "000")
if [ "$onlyoffice_health" = "200" ]; then
    log_success "   ✅ OnlyOffice 健康检查正常 (200)"
else
    log_warn "   ⚠️  OnlyOffice 健康检查异常 (状态码: $onlyoffice_health)"
fi

echo ""

# 4. 检查关键配置
log_info "4. 检查关键配置..."

# ONLYOFFICE_CALLBACK_URL
callback_url=$(docker exec law-firm-backend env | grep "^ONLYOFFICE_CALLBACK_URL=" | cut -d'=' -f2- || echo "")
if [[ "$callback_url" == *"backend:8080"* ]]; then
    log_success "   ✅ ONLYOFFICE_CALLBACK_URL 使用 Docker 内部地址: $callback_url"
else
    log_error "   ❌ ONLYOFFICE_CALLBACK_URL 配置错误: $callback_url"
    log_error "      应该使用: http://backend:8080/api"
fi

# FILE_SERVER_URL
file_server_url=$(docker exec law-firm-backend env | grep "^FILE_SERVER_URL=" | cut -d'=' -f2- || echo "")
if [[ "$file_server_url" == *"minio:9000"* ]] || [[ "$file_server_url" == *"law-firm-minio:9000"* ]]; then
    log_success "   ✅ FILE_SERVER_URL 使用 Docker 内部地址: $file_server_url"
else
    log_error "   ❌ FILE_SERVER_URL 配置错误: $file_server_url"
    log_error "      应该使用: http://minio:9000 或 http://law-firm-minio:9000"
fi

# ONLYOFFICE_URL
onlyoffice_url=$(docker exec law-firm-backend env | grep "^ONLYOFFICE_URL=" | cut -d'=' -f2- || echo "")
if [[ "$onlyoffice_url" == *"/onlyoffice"* ]] || [[ "$onlyoffice_url" == *"onlyoffice"* ]]; then
    log_success "   ✅ ONLYOFFICE_URL 配置: $onlyoffice_url"
else
    log_warn "   ⚠️  ONLYOFFICE_URL 配置: $onlyoffice_url"
fi

# MINIO_ENDPOINT
minio_endpoint=$(docker exec law-firm-backend env | grep "^MINIO_ENDPOINT=" | cut -d'=' -f2- || echo "")
if [[ "$minio_endpoint" == *"minio:9000"* ]]; then
    log_success "   ✅ MINIO_ENDPOINT 使用 Docker 内部地址: $minio_endpoint"
else
    log_error "   ❌ MINIO_ENDPOINT 配置错误: $minio_endpoint"
fi

echo ""

# 5. 检查 Nginx 配置
log_info "5. 检查 Nginx 配置..."

if docker ps --format '{{.Names}}' | grep -q "^law-firm-frontend$"; then
    # 检查 /onlyoffice/ location
    if docker exec law-firm-frontend cat /etc/nginx/nginx.conf 2>/dev/null | grep -q "location /onlyoffice/"; then
        log_success "   ✅ Nginx /onlyoffice/ location 已配置"
    else
        log_error "   ❌ Nginx /onlyoffice/ location 未配置"
    fi
    
    # 检查 WebSocket 支持
    if docker exec law-firm-frontend cat /etc/nginx/nginx.conf 2>/dev/null | grep -A 5 "location /onlyoffice/" | grep -q "Upgrade"; then
        log_success "   ✅ Nginx WebSocket 支持已配置"
    else
        log_warn "   ⚠️  Nginx WebSocket 支持可能未配置"
    fi
else
    log_warn "   ⚠️  Frontend 容器未运行，跳过 Nginx 检查"
fi

echo ""

# 6. 检查端口映射
log_info "6. 检查端口映射..."

# OnlyOffice 端口
if docker ps --format '{{.Names}}\t{{.Ports}}' | grep "^onlyoffice" | grep -q "8088:80"; then
    log_warn "   ⚠️  OnlyOffice 端口 8088 仍暴露（应该通过 Nginx 访问）"
else
    log_success "   ✅ OnlyOffice 端口未暴露（通过 Nginx 访问）"
fi

# MinIO 端口
if docker ps --format '{{.Names}}\t{{.Ports}}' | grep "^law-firm-minio" | grep -q "9000:9000\|9001:9001"; then
    log_warn "   ⚠️  MinIO 端口仍暴露（应该通过 Nginx 访问）"
else
    log_success "   ✅ MinIO 端口未暴露（通过 Nginx 访问）"
fi

echo ""

# 7. 检查最近的日志
log_info "7. 检查最近的 OnlyOffice 相关日志..."
echo ""
docker logs law-firm-backend --tail 30 2>/dev/null | grep -i "onlyoffice\|file-proxy\|minio" | tail -10 || log_warn "   无相关日志"

echo ""
echo "=============================================="
log_info "测试完成！"
echo "=============================================="
echo ""
log_info "下一步手动测试："
echo "  1. 登录系统"
echo "  2. 进入卷宗管理"
echo "  3. 上传一个 DOC 文件（或使用已有文件）"
echo "  4. 点击'编辑'按钮"
echo "  5. 验证 OnlyOffice 编辑器正常加载"
echo "  6. 修改文档内容"
echo "  7. 点击'保存'"
echo "  8. 验证保存成功，文档已更新"
echo ""
log_info "查看详细日志："
echo "  docker logs law-firm-backend | grep -i onlyoffice"
echo "  docker logs onlyoffice | tail -50"
echo ""
