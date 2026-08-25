#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <image-ref> <nginx-config>" >&2
  exit 1
fi

IMAGE_REF="$1"
NGINX_CONFIG_SOURCE="$2"
DEPLOY_DIR="${BELOG_DEPLOY_DIR:-${HOME}/belog}"
NETWORK_NAME="${BELOG_NETWORK_NAME:-belog-network}"
HEALTH_CHECK_ATTEMPTS="${BELOG_HEALTH_CHECK_ATTEMPTS:-30}"
HEALTH_CHECK_INTERVAL_SECONDS="${BELOG_HEALTH_CHECK_INTERVAL_SECONDS:-2}"
HEALTH_CHECK_TIMEOUT_SECONDS="${BELOG_HEALTH_CHECK_TIMEOUT_SECONDS:-3}"
ENV_FILE="${DEPLOY_DIR}/.env.prod"
ACTIVE_COLOR_FILE="${DEPLOY_DIR}/active-color"
ACTIVE_IMAGE_FILE="${DEPLOY_DIR}/active-image"
CANDIDATE_COLOR_FILE="${DEPLOY_DIR}/candidate-color"
CANDIDATE_IMAGE_FILE="${DEPLOY_DIR}/candidate-image"
NGINX_CONFIG_PATH="/etc/nginx/conf.d/belog.conf"
NGINX_UPSTREAM_PATH="/etc/nginx/conf.d/belog-upstream.inc"

for command_name in docker curl nginx; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is not installed: $command_name" >&2
    exit 1
  fi
done

if [[ ! -r "$ENV_FILE" ]]; then
  echo "Production environment file is missing: $ENV_FILE" >&2
  exit 1
fi

if [[ ! -r "$NGINX_CONFIG_SOURCE" ]]; then
  echo "Nginx configuration file is missing: $NGINX_CONFIG_SOURCE" >&2
  exit 1
fi

if [[ ! "$HEALTH_CHECK_ATTEMPTS" =~ ^[1-9][0-9]*$ ]] \
  || [[ ! "$HEALTH_CHECK_INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]] \
  || [[ ! "$HEALTH_CHECK_TIMEOUT_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Health check settings must be positive integers." >&2
  exit 1
fi

ROOT_COMMAND=()
if [[ "$EUID" -ne 0 ]]; then
  if ! command -v sudo >/dev/null 2>&1 || ! sudo -n true; then
    echo "Passwordless sudo is required to update and reload Nginx." >&2
    exit 1
  fi
  ROOT_COMMAND=(sudo -n)
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
health_check_url="http://127.0.0.1:${target_port}/actuator/health"
nginx_backup_dir=""
nginx_config_existed=false
nginx_upstream_existed=false
nginx_changes_staged=false
container_started=false
deployment_succeeded=false

restore_nginx_configuration() {
  if [[ "$nginx_config_existed" == true ]]; then
    "${ROOT_COMMAND[@]}" install -m 644 \
      "${nginx_backup_dir}/belog.conf" "$NGINX_CONFIG_PATH"
  else
    "${ROOT_COMMAND[@]}" rm -f "$NGINX_CONFIG_PATH"
  fi

  if [[ "$nginx_upstream_existed" == true ]]; then
    "${ROOT_COMMAND[@]}" install -m 644 \
      "${nginx_backup_dir}/belog-upstream.inc" "$NGINX_UPSTREAM_PATH"
  else
    "${ROOT_COMMAND[@]}" rm -f "$NGINX_UPSTREAM_PATH"
  fi
}

cleanup() {
  status=$?
  trap - EXIT

  if [[ "$deployment_succeeded" != true ]]; then
    if [[ "$nginx_changes_staged" == true ]]; then
      restore_nginx_configuration || true
      "${ROOT_COMMAND[@]}" nginx -t >/dev/null 2>&1 \
        && "${ROOT_COMMAND[@]}" nginx -s reload >/dev/null 2>&1 \
        || true
    fi

    if [[ "$container_started" == true ]]; then
      docker rm --force "$container_name" >/dev/null 2>&1 || true
    fi

    rm -f \
      "$CANDIDATE_COLOR_FILE" \
      "$CANDIDATE_IMAGE_FILE" \
      "${DEPLOY_DIR}/belog-upstream.inc.candidate"
  fi

  if [[ -n "$nginx_backup_dir" ]]; then
    "${ROOT_COMMAND[@]}" rm -f \
      "${nginx_backup_dir}/belog.conf" \
      "${nginx_backup_dir}/belog-upstream.inc"
    rmdir "$nginx_backup_dir" 2>/dev/null || true
  fi

  exit "$status"
}

trap cleanup EXIT

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
container_started=true

printf '%s\n' "$target_color" > "$CANDIDATE_COLOR_FILE"
printf '%s\n' "$IMAGE_REF" > "$CANDIDATE_IMAGE_FILE"

health_check_succeeded=false
for ((attempt = 1; attempt <= HEALTH_CHECK_ATTEMPTS; attempt++)); do
  if curl --fail --silent --show-error \
    --max-time "$HEALTH_CHECK_TIMEOUT_SECONDS" \
    "$health_check_url" >/dev/null; then
    health_check_succeeded=true
    break
  fi

  echo "Health check attempt ${attempt}/${HEALTH_CHECK_ATTEMPTS} failed."
  if [[ "$(docker inspect --format '{{.State.Running}}' "$container_name" 2>/dev/null)" != true ]]; then
    echo "Candidate container stopped before becoming healthy." >&2
    break
  fi

  if ((attempt < HEALTH_CHECK_ATTEMPTS)); then
    sleep "$HEALTH_CHECK_INTERVAL_SECONDS"
  fi
done

if [[ "$health_check_succeeded" != true ]]; then
  echo "Health check failed: $health_check_url" >&2
  docker logs --tail 100 "$container_name" >&2 || true
  exit 1
fi

nginx_backup_dir="$(mktemp -d "${DEPLOY_DIR}/nginx-backup.XXXXXX")"

if "${ROOT_COMMAND[@]}" test -f "$NGINX_CONFIG_PATH"; then
  nginx_config_existed=true
  "${ROOT_COMMAND[@]}" cp "$NGINX_CONFIG_PATH" "${nginx_backup_dir}/belog.conf"
fi

if "${ROOT_COMMAND[@]}" test -f "$NGINX_UPSTREAM_PATH"; then
  nginx_upstream_existed=true
  "${ROOT_COMMAND[@]}" cp "$NGINX_UPSTREAM_PATH" \
    "${nginx_backup_dir}/belog-upstream.inc"
fi

printf 'server 127.0.0.1:%s max_fails=3 fail_timeout=10s;\n' "$target_port" \
  > "${DEPLOY_DIR}/belog-upstream.inc.candidate"

nginx_changes_staged=true
"${ROOT_COMMAND[@]}" install -m 644 "$NGINX_CONFIG_SOURCE" "$NGINX_CONFIG_PATH"
"${ROOT_COMMAND[@]}" install -m 644 \
  "${DEPLOY_DIR}/belog-upstream.inc.candidate" "$NGINX_UPSTREAM_PATH"

if ! "${ROOT_COMMAND[@]}" nginx -t; then
  echo "Nginx configuration validation failed. Keeping the existing environment active." >&2
  exit 1
fi

if ! "${ROOT_COMMAND[@]}" nginx -s reload; then
  echo "Nginx reload failed. Restoring the previous configuration." >&2
  exit 1
fi

printf '%s\n' "$target_color" > "${ACTIVE_COLOR_FILE}.tmp"
mv "${ACTIVE_COLOR_FILE}.tmp" "$ACTIVE_COLOR_FILE"

printf '%s\n' "$IMAGE_REF" > "${ACTIVE_IMAGE_FILE}.tmp"
mv "${ACTIVE_IMAGE_FILE}.tmp" "$ACTIVE_IMAGE_FILE"

rm -f \
  "$CANDIDATE_COLOR_FILE" \
  "$CANDIDATE_IMAGE_FILE" \
  "${DEPLOY_DIR}/belog-upstream.inc.candidate"

deployment_succeeded=true
echo "Activated $IMAGE_REF on $target_color via 127.0.0.1:$target_port"
