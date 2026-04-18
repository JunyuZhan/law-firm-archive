#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DIST_ROOT="${DIST_ROOT:-$PROJECT_ROOT/../dist-center}"
PROJECT_SLUG="${PROJECT_SLUG:-law-firm-archive}"

COMPOSE_SRC="$PROJECT_ROOT/docker/docker-compose.registry.yml"
ENV_SRC="$PROJECT_ROOT/docker/.env.example"
SECRETS_SRC="$PROJECT_ROOT/docker/secrets/init-secrets.sh"
INIT_DB_SRC="$PROJECT_ROOT/scripts/init-db"

COMPOSE_DEST="$DIST_ROOT/assets/compose/$PROJECT_SLUG"
SCRIPTS_DEST="$DIST_ROOT/assets/scripts/$PROJECT_SLUG"

mkdir -p "$COMPOSE_DEST/secrets" "$SCRIPTS_DEST"

cp "$COMPOSE_SRC" "$COMPOSE_DEST/docker-compose.yml"
cp "$ENV_SRC" "$COMPOSE_DEST/.env.example"
if [[ -f "$SECRETS_SRC" ]]; then
  cp "$SECRETS_SRC" "$COMPOSE_DEST/secrets/init-secrets.sh"
fi

if [[ -d "$INIT_DB_SRC" ]]; then
  if command -v rsync >/dev/null 2>&1; then
    rsync -a --delete "$INIT_DB_SRC/" "$SCRIPTS_DEST/"
  else
    rm -rf "$SCRIPTS_DEST"
    mkdir -p "$SCRIPTS_DEST"
    cp -R "$INIT_DB_SRC"/. "$SCRIPTS_DEST/"
  fi
fi

cat <<EOF
Synced distribution-center assets.
Dist root: $DIST_ROOT
Project: $PROJECT_SLUG
Compose: $COMPOSE_DEST/docker-compose.yml
Env: $COMPOSE_DEST/.env.example
Init DB dir: $SCRIPTS_DEST
EOF
