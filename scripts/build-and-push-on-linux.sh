#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
ENV_FILE="${ENV_FILE:-$ROOT_DIR/docker/.env.registry}"

if [ ! -f "$ENV_FILE" ]; then
  echo "missing env file: $ENV_FILE"
  echo "copy docker/.env.registry.example to docker/.env.registry first"
  exit 1
fi

read_env_value() {
  awk -v key="$1" '
    /^[[:space:]]*#/ { next }
    {
      pos = index($0, "=")
      if (pos == 0) {
        next
      }
      current_key = substr($0, 1, pos - 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", current_key)
      if (current_key == key) {
        value = substr($0, pos + 1)
        sub(/\r$/, "", value)
        print value
        exit
      }
    }
  ' "$ENV_FILE"
}

REGISTRY_PUSH="$(read_env_value REGISTRY_PUSH)"
APP_VERSION="$(read_env_value APP_VERSION)"
APP_COMMIT_SHA="$(read_env_value APP_COMMIT_SHA)"
REGISTRY_PUSH="${REGISTRY_PUSH:-192.168.50.5:5050}"
APP_VERSION="${APP_VERSION:-v0.1.2}"
APP_COMMIT_SHA="${APP_COMMIT_SHA:-$(git -C "$ROOT_DIR" rev-parse --short HEAD)}"
PROJECT_PATH="law-firm-archive"

ARCH="$(uname -m)"
echo "building native images on architecture: $ARCH"
echo "target registry: $REGISTRY_PUSH"
echo "app version: $APP_VERSION"
echo "commit tag: $APP_COMMIT_SHA"

docker build -f "$ROOT_DIR/docker/Dockerfile" -t "$REGISTRY_PUSH/$PROJECT_PATH/backend:$APP_VERSION" "$ROOT_DIR"
docker tag "$REGISTRY_PUSH/$PROJECT_PATH/backend:$APP_VERSION" "$REGISTRY_PUSH/$PROJECT_PATH/backend:$APP_COMMIT_SHA"

docker build -f "$ROOT_DIR/docker/Dockerfile.frontend" -t "$REGISTRY_PUSH/$PROJECT_PATH/frontend:$APP_VERSION" "$ROOT_DIR"
docker tag "$REGISTRY_PUSH/$PROJECT_PATH/frontend:$APP_VERSION" "$REGISTRY_PUSH/$PROJECT_PATH/frontend:$APP_COMMIT_SHA"

docker build -f "$ROOT_DIR/docker/Dockerfile.elasticsearch" -t "$REGISTRY_PUSH/$PROJECT_PATH/elasticsearch:$APP_VERSION" "$ROOT_DIR/docker"
docker tag "$REGISTRY_PUSH/$PROJECT_PATH/elasticsearch:$APP_VERSION" "$REGISTRY_PUSH/$PROJECT_PATH/elasticsearch:$APP_COMMIT_SHA"

docker push "$REGISTRY_PUSH/$PROJECT_PATH/backend:$APP_VERSION"
docker push "$REGISTRY_PUSH/$PROJECT_PATH/backend:$APP_COMMIT_SHA"
docker push "$REGISTRY_PUSH/$PROJECT_PATH/frontend:$APP_VERSION"
docker push "$REGISTRY_PUSH/$PROJECT_PATH/frontend:$APP_COMMIT_SHA"
docker push "$REGISTRY_PUSH/$PROJECT_PATH/elasticsearch:$APP_VERSION"
docker push "$REGISTRY_PUSH/$PROJECT_PATH/elasticsearch:$APP_COMMIT_SHA"

echo "done"
