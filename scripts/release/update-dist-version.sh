#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

DIST_ROOT="${DIST_ROOT:-$PROJECT_ROOT/../dist-center}"
PROJECT_SLUG="${PROJECT_SLUG:-law-firm-archive}"
VERSION=""
APP_VERSION=""
SNAPSHOT=false

usage() {
  cat <<'EOF'
Usage:
  bash scripts/release/update-dist-version.sh \
    --version <tag-or-version> \
    [--app-version <image-tag>] \
    [--dist-root <path>] \
    [--project <slug>] \
    [--snapshot]
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version)
      VERSION="${2:-}"
      shift 2
      ;;
    --app-version)
      APP_VERSION="${2:-}"
      shift 2
      ;;
    --dist-root)
      DIST_ROOT="${2:-}"
      shift 2
      ;;
    --project)
      PROJECT_SLUG="${2:-}"
      shift 2
      ;;
    --snapshot)
      SNAPSHOT=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

[[ -n "$VERSION" ]] || { echo "Missing --version" >&2; exit 1; }
APP_VERSION="${APP_VERSION:-$VERSION}"

LATEST_FILE="$DIST_ROOT/projects/$PROJECT_SLUG/versions/latest.json"
SNAPSHOT_FILE="$DIST_ROOT/projects/$PROJECT_SLUG/versions/${VERSION}.json"

[[ -f "$LATEST_FILE" ]] || { echo "Missing latest descriptor: $LATEST_FILE" >&2; exit 1; }

python3 - "$LATEST_FILE" "$SNAPSHOT_FILE" "$VERSION" "$APP_VERSION" "$SNAPSHOT" <<'PY'
from pathlib import Path
import json
import sys

latest_file = Path(sys.argv[1])
snapshot_file = Path(sys.argv[2])
version = sys.argv[3]
app_version = sys.argv[4]
snapshot = sys.argv[5].lower() == "true"

data = json.loads(latest_file.read_text())
data["version"] = version
data["app_version"] = app_version
if "release" in data:
    data["release"]["version"] = app_version
if "env" in data:
    data["env"]["APP_VERSION"] = app_version

latest_file.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
if snapshot:
    snapshot_file.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
PY

cat <<EOF
Updated dist-center version descriptor.
Dist root: $DIST_ROOT
Project: $PROJECT_SLUG
Version: $VERSION
App version: $APP_VERSION
Latest: $LATEST_FILE
Snapshot: $SNAPSHOT_FILE
EOF
