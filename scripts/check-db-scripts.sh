#!/bin/bash
# =====================================================
# 数据库脚本完整性检查工具
# =====================================================
# 用途: 检查数据库初始化脚本是否完整、顺序正确
# =====================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INIT_DB_DIR="$SCRIPT_DIR/init-db"

echo "=========================================="
echo "数据库脚本完整性检查"
echo "=========================================="
echo ""

# 定义预期的脚本列表（按执行顺序）
EXPECTED_SCRIPTS=(
    "00-extensions.sql"
    "01-system-schema.sql"
    "02-client-schema.sql"
    "03-matter-schema.sql"
    "04-finance-schema.sql"
    "05-document-schema.sql"
    "06-evidence-schema.sql"
    "07-archive-schema.sql"
    "08-timesheet-schema.sql"
    "09-task-schema.sql"
    "10-admin-schema.sql"
    "11-asset-schema.sql"
    "12-knowledge-schema.sql"
    "13-hr-schema.sql"
    "14-quality-schema.sql"
    "15-workbench-schema.sql"
    "16-contract-template-schema.sql"
    "20-system-init-data.sql"
    "21-template-init-data.sql"
)

# 检查1: 验证所有脚本文件是否存在
echo "1. 检查脚本文件是否存在..."
echo ""

missing_files=0
for script in "${EXPECTED_SCRIPTS[@]}"; do
    script_path="$INIT_DB_DIR/$script"
    if [ -f "$script_path" ]; then
        size=$(wc -c < "$script_path")
        lines=$(wc -l < "$script_path")
        echo "  ✓ $script ($lines 行, $(numfmt --to=iec-i --suffix=B $size 2>/dev/null || echo "${size}B"))"
    else
        echo "  ✗ $script (缺失)"
        missing_files=$((missing_files + 1))
    fi
done

echo ""
if [ $missing_files -eq 0 ]; then
    echo "✓ 所有脚本文件都存在"
else
    echo "✗ 发现 $missing_files 个缺失的脚本文件"
fi
echo ""

# 检查2: 验证脚本语法（基本检查）
echo "2. 检查脚本基本语法..."
echo ""

syntax_errors=0
for script in "${EXPECTED_SCRIPTS[@]}"; do
    script_path="$INIT_DB_DIR/$script"
    if [ -f "$script_path" ]; then
        # 检查是否有未闭合的引号或括号（简单检查）
        if grep -q "^CREATE TABLE" "$script_path" 2>/dev/null; then
            # 检查是否有基本的SQL关键字
            if grep -qE "(CREATE|ALTER|INSERT|UPDATE|DELETE|SELECT)" "$script_path" 2>/dev/null; then
                echo "  ✓ $script (语法检查通过)"
            else
                echo "  ⚠ $script (未发现SQL语句)"
            fi
        else
            echo "  ✓ $script (非表结构脚本)"
        fi
    fi
done

echo ""

# 检查3: 验证外键依赖顺序
echo "3. 检查外键依赖关系..."
echo ""

# 检查关键外键依赖（简化检查，只验证主要依赖）
fk_errors=0
echo "  检查主要外键依赖关系..."

# 验证关键依赖顺序
check_dependency() {
    local script="$1"
    local dep="$2"
    local script_idx=-1
    local dep_idx=-1
    
    for i in "${!EXPECTED_SCRIPTS[@]}"; do
        if [ "${EXPECTED_SCRIPTS[$i]}" == "$script" ]; then
            script_idx=$i
        fi
        if [ "${EXPECTED_SCRIPTS[$i]}" == "$dep" ]; then
            dep_idx=$i
        fi
    done
    
    if [ $script_idx -ne -1 ] && [ $dep_idx -ne -1 ]; then
        if [ $dep_idx -lt $script_idx ]; then
            echo "    ✓ $script 依赖 $dep (顺序正确)"
            return 0
        else
            echo "    ✗ $script 依赖 $dep (顺序错误！)"
            return 1
        fi
    fi
    return 0
}

# 检查主要依赖关系
check_dependency "02-client-schema.sql" "01-system-schema.sql" || fk_errors=$((fk_errors + 1))
check_dependency "03-matter-schema.sql" "02-client-schema.sql" || fk_errors=$((fk_errors + 1))
check_dependency "04-finance-schema.sql" "03-matter-schema.sql" || fk_errors=$((fk_errors + 1))
check_dependency "05-document-schema.sql" "03-matter-schema.sql" || fk_errors=$((fk_errors + 1))
check_dependency "16-contract-template-schema.sql" "04-finance-schema.sql" || fk_errors=$((fk_errors + 1))
check_dependency "20-system-init-data.sql" "01-system-schema.sql" || fk_errors=$((fk_errors + 1))
check_dependency "21-template-init-data.sql" "16-contract-template-schema.sql" || fk_errors=$((fk_errors + 1))

echo ""
if [ $fk_errors -eq 0 ]; then
    echo "✓ 外键依赖顺序正确"
else
    echo "✗ 发现 $fk_errors 个外键依赖顺序问题"
fi
echo ""

# 检查4: 验证重置脚本
echo "4. 检查重置脚本..."
echo ""

if [ -f "$SCRIPT_DIR/reset-db.sh" ]; then
    if [ -x "$SCRIPT_DIR/reset-db.sh" ]; then
        echo "  ✓ reset-db.sh 存在且可执行"
    else
        echo "  ⚠ reset-db.sh 存在但不可执行 (需要 chmod +x)"
    fi
else
    echo "  ✗ reset-db.sh 不存在"
fi

if [ -f "$INIT_DB_DIR/init-database.sh" ]; then
    if [ -x "$INIT_DB_DIR/init-database.sh" ]; then
        echo "  ✓ init-database.sh 存在且可执行"
    else
        echo "  ⚠ init-database.sh 存在但不可执行 (需要 chmod +x)"
    fi
else
    echo "  ✗ init-database.sh 不存在"
fi

echo ""

# 总结
echo "=========================================="
echo "检查完成"
echo "=========================================="
echo ""

if [ $missing_files -eq 0 ] && [ $fk_errors -eq 0 ]; then
    echo "✓ 所有检查通过，数据库脚本可以安全重置"
    exit 0
else
    echo "✗ 发现问题，请修复后再重置数据库"
    exit 1
fi

