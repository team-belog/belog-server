#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <image-ref>" >&2
  exit 1
fi

IMAGE_REF="$1"
DEPLOY_DIR="${BELOG_DEPLOY_DIR:-${HOME}/belog}"
NETWORK_NAME="${BELOG_NETWORK_NAME:-belog-network}"
ENV_FILE="${DEPLOY_DIR}/.env.prod"
ACTIVE_COLOR_FILE="${DEPLOY_DIR}/active-color"
CANDIDATE_COLOR_FILE="${DEPLOY_DIR}/candidate-color"
CANDIDATE_IMAGE_FILE="${DEPLOY_DIR}/candidate-image"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed on the deployment host." >&2
  exit 1
fi

if [[ ! -r "$ENV_FILE" ]]; then
  echo "Production environment file is missing: $ENV_FILE" >&2
  exit 1
fi

mkdir -p "$DEPLOY_DIR"

docker network inspect "$NETWORK_NAME" >/dev/null 2>&1 \
  || docker network create "$NETWORK_NAME"

active_color=""
if [[ -f "$ACTIVE_COLOR_FILE" ]]; then
  read -r active_color < "$ACTIVE_COLOR_FILE"
fi

case "$active_color" in
  "")
    target_color="blue"
    target_port="8081"
    ;;
  blue)
    target_color="green"
    target_port="8082"
    ;;
  green)
    target_color="blue"
    target_port="8081"
    ;;
  *)
    echo "Invalid active environment: $active_color" >&2
    exit 1
    ;;
esac

container_name="belog-${target_color}"

docker pull "$IMAGE_REF"
docker rm --force "$container_name" >/dev/null 2>&1 || true
docker run --detach \
  --name "$container_name" \
  --network "$NETWORK_NAME" \
  --restart unless-stopped \
  --env-file "$ENV_FILE" \
  --env SPRING_PROFILES_ACTIVE=prod \
  --env SERVER_PORT=8080 \
  --publish "127.0.0.1:${target_port}:8080" \
  --label "belog.environment=${target_color}" \
  --label "belog.image=${IMAGE_REF}" \
  "$IMAGE_REF"

printf '%s\n' "$target_color" > "${CANDIDATE_COLOR_FILE}.tmp"
mv "${CANDIDATE_COLOR_FILE}.tmp" "$CANDIDATE_COLOR_FILE"

printf '%s\n' "$IMAGE_REF" > "${CANDIDATE_IMAGE_FILE}.tmp"
mv "${CANDIDATE_IMAGE_FILE}.tmp" "$CANDIDATE_IMAGE_FILE"

echo "Deployed $IMAGE_REF to $target_color on 127.0.0.1:$target_port"
