#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

DIST_ROOT="${DIST_ROOT:-$PROJECT_ROOT/../dist-center}"
PROJECT_SLUG="${PROJECT_SLUG:-law-firm-archive}"
VERSION=""
APP_VERSION=""
DEFAULT_REGISTRY="${DEFAULT_REGISTRY:-hub.albertzhan.top/law-firm-archive/}"
INTERNAL_REGISTRY="${INTERNAL_REGISTRY:-192.168.50.5:5050/law-firm-archive/}"
REGISTRY_USERNAME="${REGISTRY_USERNAME:-installer}"
PUBLISHED_AT="${PUBLISHED_AT:-}"
SNAPSHOT=false

usage() {
  cat <<'EOF'
Usage:
  bash scripts/release/update-dist-version.sh \
    --version <tag-or-version> \
    [--app-version <image-tag>] \
    [--dist-root <path>] \
    [--project <slug>] \
    [--default-registry <prefix>] \
    [--internal-registry <prefix>] \
    [--registry-username <value>] \
    [--published-at <iso-8601>] \
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
    --default-registry)
      DEFAULT_REGISTRY="${2:-}"
      shift 2
      ;;
    --internal-registry)
      INTERNAL_REGISTRY="${2:-}"
      shift 2
      ;;
    --registry-username)
      REGISTRY_USERNAME="${2:-}"
      shift 2
      ;;
    --published-at)
      PUBLISHED_AT="${2:-}"
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
PUBLISHED_AT="${PUBLISHED_AT:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"

LATEST_FILE="$DIST_ROOT/projects/$PROJECT_SLUG/versions/latest.json"
SNAPSHOT_FILE="$DIST_ROOT/projects/$PROJECT_SLUG/versions/${VERSION}.json"

[[ -f "$LATEST_FILE" ]] || { echo "Missing latest descriptor: $LATEST_FILE" >&2; exit 1; }

python3 - "$LATEST_FILE" "$SNAPSHOT_FILE" "$VERSION" "$APP_VERSION" "$SNAPSHOT" \
  "$DEFAULT_REGISTRY" "$INTERNAL_REGISTRY" "$REGISTRY_USERNAME" "$PUBLISHED_AT" <<'PY'
from pathlib import Path
import json
import sys

latest_file = Path(sys.argv[1])
snapshot_file = Path(sys.argv[2])
version = sys.argv[3]
app_version = sys.argv[4]
snapshot = sys.argv[5].lower() == "true"
default_registry = sys.argv[6]
internal_registry = sys.argv[7]
registry_username = sys.argv[8]
published_at = sys.argv[9]

data = json.loads(latest_file.read_text())
data["version"] = version
data["descriptor_version"] = version
data["app_version"] = app_version
data["default_registry"] = default_registry
data["internal_registry"] = internal_registry
if "release" in data:
    data["release"]["version"] = app_version
    data["release"]["published_at"] = published_at
if "env" in data:
    data["env"]["APP_VERSION"] = app_version
    data["env"]["APP_REGISTRY"] = default_registry
    data["env"]["APP_REGISTRY_USERNAME"] = registry_username

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
Default registry: $DEFAULT_REGISTRY
Internal registry: $INTERNAL_REGISTRY
Latest: $LATEST_FILE
Snapshot: $SNAPSHOT_FILE
EOF
