#!/usr/bin/env sh
# 将仓库根目录 VERSION 同步到 backend/pom.xml 与 frontend/package.json
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
VERSION_FILE="$ROOT_DIR/VERSION"

if [ ! -f "$VERSION_FILE" ]; then
  echo "missing $VERSION_FILE"
  exit 1
fi

VER=$(tr -d ' \r\n' < "$VERSION_FILE")
if [ -z "$VER" ]; then
  echo "VERSION file is empty"
  exit 1
fi

POM="$ROOT_DIR/backend/pom.xml"
PKG="$ROOT_DIR/frontend/package.json"

if [ ! -f "$POM" ] || [ ! -f "$PKG" ]; then
  echo "expected $POM and $PKG"
  exit 1
fi

perl -0777 -pi -e 's/(<artifactId>archive-system<\/artifactId>\s*<version>)[^<]+(<\/version>)/${1}'"$VER"'${2}/' "$POM"
node -e '
const fs = require("fs");
const p = process.argv[1];
const v = process.argv[2];
const j = JSON.parse(fs.readFileSync(p, "utf8"));
j.version = v;
fs.writeFileSync(p, JSON.stringify(j, null, 2) + "\n");
' "$PKG" "$VER"

echo "synced product version to $VER (pom + frontend/package.json)"
