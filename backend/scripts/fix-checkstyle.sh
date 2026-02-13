#!/bin/bash

# Checkstyle 修复脚本
# 用于批量修复代码格式问题

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_DIR"

echo "=========================================="
echo "Checkstyle 修复脚本"
echo "=========================================="
echo ""

# 1. 格式化代码
echo "步骤 1/4: 运行 Spotless 格式化代码..."
mvn spotless:apply
echo "✅ 格式化完成"
echo ""

# 2. 运行测试确保没有破坏功能
echo "步骤 2/4: 运行测试..."
if mvn test -DskipTests=false -B > /dev/null 2>&1; then
    echo "✅ 所有测试通过"
else
    echo "⚠️  测试失败，请检查"
    read -p "是否继续？(y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi
echo ""

# 3. 检查剩余错误
echo "步骤 3/4: 检查 Checkstyle 错误..."
mvn checkstyle:check > checkstyle-report.txt 2>&1 || true

ERROR_COUNT=$(grep -c "ERROR" checkstyle-report.txt 2>/dev/null || echo "0")
echo "剩余错误数: $ERROR_COUNT"
echo ""

# 4. 显示报告
if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "步骤 4/4: 错误报告已保存到 checkstyle-report.txt"
    echo ""
    echo "前 20 个错误："
    grep "ERROR" checkstyle-report.txt | head -20
    echo ""
    echo "查看完整报告: cat checkstyle-report.txt"
else
    echo "步骤 4/4: ✅ 所有 Checkstyle 错误已修复！"
fi

echo ""
echo "=========================================="
echo "修复完成！"
echo "=========================================="
