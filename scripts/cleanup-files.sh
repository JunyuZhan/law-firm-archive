#!/bin/bash
# =====================================================
# 律师事务所管理系统 - 文件清理脚本
# =====================================================
# 清理不再需要的文件：备份文件、临时文件、系统文件等
# 用法: ./cleanup-files.sh [--dry-run]
# =====================================================

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 解析参数
DRY_RUN=false
for arg in "$@"; do
    case $arg in
        --dry-run)
            DRY_RUN=true
            ;;
        *)
            echo "未知参数: $arg"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}文件清理工具${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}⚠️  预览模式（不会实际删除文件）${NC}"
    echo ""
fi

# 统计变量
DELETED_COUNT=0
DELETED_SIZE=0

# 删除文件的函数
delete_file() {
    local file="$1"
    local desc="$2"
    
    if [ -e "$file" ]; then
        local size=$(du -sh "$file" 2>/dev/null | cut -f1)
        if [ "$DRY_RUN" = true ]; then
            echo -e "${YELLOW}[预览] 将删除: $file ($size) - $desc${NC}"
        else
            rm -rf "$file"
            echo -e "${GREEN}✓ 已删除: $file ($size) - $desc${NC}"
            DELETED_COUNT=$((DELETED_COUNT + 1))
        fi
    fi
}

echo -e "${YELLOW}[1/6] 清理备份文件...${NC}"
delete_file "$PROJECT_ROOT/docker/docker-compose.dev.yml.bak" "Docker Compose 备份文件"

echo ""
echo -e "${YELLOW}[2/6] 清理 macOS 系统文件 (.DS_Store)...${NC}"
DS_STORE_COUNT=0
while IFS= read -r file; do
    delete_file "$file" "macOS 系统文件"
    DS_STORE_COUNT=$((DS_STORE_COUNT + 1))
done < <(find "$PROJECT_ROOT" -name ".DS_Store" -type f 2>/dev/null)
if [ $DS_STORE_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ 未找到 .DS_Store 文件${NC}"
fi

echo ""
echo -e "${YELLOW}[3/6] 清理测试文件...${NC}"
delete_file "$PROJECT_ROOT/test-document.txt" "测试文档"
delete_file "$PROJECT_ROOT/test-onlyoffice.txt" "OnlyOffice 测试文件"
delete_file "$PROJECT_ROOT/test.txt" "测试文件"

echo ""
echo -e "${YELLOW}[4/6] 清理后端日志和临时目录...${NC}"
delete_file "$PROJECT_ROOT/backend/LOG_PATH_IS_UNDEFINED" "日志路径未定义目录"
delete_file "$PROJECT_ROOT/backend/logs" "后端日志目录（如果为空）"

# 检查 logs 目录是否为空
if [ -d "$PROJECT_ROOT/backend/logs" ] && [ "$DRY_RUN" = false ]; then
    if [ -z "$(ls -A "$PROJECT_ROOT/backend/logs" 2>/dev/null)" ]; then
        rmdir "$PROJECT_ROOT/backend/logs" 2>/dev/null && echo -e "${GREEN}✓ 已删除空日志目录${NC}" || true
    else
        echo -e "${YELLOW}⚠  logs 目录不为空，保留${NC}"
    fi
fi

echo ""
echo -e "${YELLOW}[5/6] 清理编译产物（谨慎）...${NC}"
# 不自动清理 target 和 node_modules，因为它们是构建需要的
echo -e "${BLUE}提示: target/ 和 node_modules/ 目录保留（构建需要）${NC}"

echo ""
echo -e "${YELLOW}[6/6] 清理其他临时文件...${NC}"
# 清理常见的临时文件
find "$PROJECT_ROOT" -type f \( -name "*.tmp" -o -name "*.swp" -o -name "*~" -o -name "*.bak" \) ! -path "*/node_modules/*" ! -path "*/target/*" ! -path "*/.git/*" 2>/dev/null | while read -r file; do
    delete_file "$file" "临时文件"
done

echo ""
echo -e "${GREEN}==========================================${NC}"
if [ "$DRY_RUN" = true ]; then
    echo -e "${GREEN}预览完成${NC}"
    echo ""
    echo "要实际执行清理，请运行:"
    echo "  ./scripts/cleanup-files.sh"
else
    echo -e "${GREEN}文件清理完成!${NC}"
    echo ""
    echo "清理统计:"
    echo "  删除文件/目录数: $DELETED_COUNT"
fi
echo -e "${GREEN}==========================================${NC}"
echo ""
