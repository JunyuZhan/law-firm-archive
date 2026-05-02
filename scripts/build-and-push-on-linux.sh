#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
ENV_FILE="${ENV_FILE:-$ROOT_DIR/docker/.env}"

if [ ! -f "$ENV_FILE" ]; then
  echo "missing env file: $ENV_FILE"
  echo "copy docker/.env.example to docker/.env first"
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
REGISTRY_USERNAME="$(read_env_value REGISTRY_USERNAME)"
REGISTRY_PASSWORD="$(read_env_value REGISTRY_PASSWORD)"
PUSH_LATEST="$(read_env_value PUSH_LATEST)"
REGISTRY_PUSH="${REGISTRY_PUSH:-192.168.50.5:5050}"
if [ -z "$APP_VERSION" ]; then
  APP_VERSION=$(tr -d ' \r\n' < "$ROOT_DIR/VERSION")
fi
APP_COMMIT_SHA="${APP_COMMIT_SHA:-$(git -C "$ROOT_DIR" rev-parse --short HEAD)}"
PUSH_LATEST="${PUSH_LATEST:-false}"
PROJECT_PATH="law-firm-archive"

ARCH="$(uname -m)"
echo "building native images on architecture: $ARCH"
echo "target registry: $REGISTRY_PUSH"
echo "app version: $APP_VERSION"
echo "commit tag: $APP_COMMIT_SHA"

if [ -n "$REGISTRY_USERNAME" ] && [ -n "$REGISTRY_PASSWORD" ]; then
  echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY_PUSH" -u "$REGISTRY_USERNAME" --password-stdin
fi

build_and_push() {
  image_name="$1"
  dockerfile_path="$2"
  context_path="$3"

  docker build -f "$dockerfile_path" --build-arg "APP_VERSION=$APP_VERSION" -t "$REGISTRY_PUSH/$PROJECT_PATH/$image_name:$APP_VERSION" "$context_path"
  docker tag "$REGISTRY_PUSH/$PROJECT_PATH/$image_name:$APP_VERSION" "$REGISTRY_PUSH/$PROJECT_PATH/$image_name:$APP_COMMIT_SHA"

  docker push "$REGISTRY_PUSH/$PROJECT_PATH/$image_name:$APP_VERSION"
  docker push "$REGISTRY_PUSH/$PROJECT_PATH/$image_name:$APP_COMMIT_SHA"

  if [ "$PUSH_LATEST" = "true" ]; then
    docker tag "$REGISTRY_PUSH/$PROJECT_PATH/$image_name:$APP_VERSION" "$REGISTRY_PUSH/$PROJECT_PATH/$image_name:latest"
    docker push "$REGISTRY_PUSH/$PROJECT_PATH/$image_name:latest"
  fi
}

build_and_push backend "$ROOT_DIR/docker/Dockerfile" "$ROOT_DIR"
build_and_push frontend "$ROOT_DIR/docker/Dockerfile.frontend" "$ROOT_DIR"
build_and_push elasticsearch "$ROOT_DIR/docker/Dockerfile.elasticsearch" "$ROOT_DIR/docker"

echo "done"
