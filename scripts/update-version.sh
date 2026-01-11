#!/bin/bash
# 版本号同步脚本
# 用法: ./scripts/update-version.sh 3.2.3

set -e

if [ -z "$1" ]; then
    echo "用法: $0 <版本号>"
    echo "示例: $0 3.2.3"
    exit 1
fi

NEW_VERSION=$1
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "📦 更新版本号到 $NEW_VERSION"
echo ""

# 更新后端版本号 (pom.xml)
echo "1. 更新后端版本号..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s|<groupId>com.lawfirm</groupId>|<groupId>com.lawfirm</groupId>|" "$PROJECT_ROOT/backend/pom.xml"
    # 更新项目版本号（第16行左右）
    sed -i '' "/<artifactId>law-firm-management<\/artifactId>/,/<version>.*<\/version>/{s|<version>.*</version>|<version>$NEW_VERSION</version>|;}" "$PROJECT_ROOT/backend/pom.xml"
else
    # Linux
    sed -i "/<artifactId>law-firm-management<\/artifactId>/,/<version>.*<\/version>/{s|<version>.*</version>|<version>$NEW_VERSION</version>|;}" "$PROJECT_ROOT/backend/pom.xml"
fi
echo "   ✅ backend/pom.xml"

# 更新前端版本号 (package.json)
echo "2. 更新前端版本号..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s|\"version\": \".*\"|\"version\": \"$NEW_VERSION\"|" "$PROJECT_ROOT/frontend/apps/web-antd/package.json"
else
    sed -i "s|\"version\": \".*\"|\"version\": \"$NEW_VERSION\"|" "$PROJECT_ROOT/frontend/apps/web-antd/package.json"
fi
echo "   ✅ frontend/apps/web-antd/package.json"

echo ""
echo "✅ 版本号已更新为 $NEW_VERSION"
echo ""
echo "下一步："
echo "  git add ."
echo "  git commit -m \"release: v$NEW_VERSION\""
echo "  git tag v$NEW_VERSION"
echo "  git push origin main --tags"

