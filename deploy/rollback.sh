#!/usr/bin/env bash

set -Eeuo pipefail

DEPLOY_DIR="${BELOG_DEPLOY_DIR:-${HOME}/belog}"
HEALTH_CHECK_ATTEMPTS="${BELOG_HEALTH_CHECK_ATTEMPTS:-30}"
HEALTH_CHECK_INTERVAL_SECONDS="${BELOG_HEALTH_CHECK_INTERVAL_SECONDS:-2}"
HEALTH_CHECK_TIMEOUT_SECONDS="${BELOG_HEALTH_CHECK_TIMEOUT_SECONDS:-3}"
ACTIVE_COLOR_FILE="${DEPLOY_DIR}/active-color"
ACTIVE_IMAGE_FILE="${DEPLOY_DIR}/active-image"
NGINX_UPSTREAM_PATH="/etc/nginx/conf.d/belog-upstream.inc"

for command_name in docker curl nginx; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is not installed: $command_name" >&2
    exit 1
  fi
done

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

if [[ ! -f "$ACTIVE_COLOR_FILE" ]]; then
  echo "Active environment state is missing: $ACTIVE_COLOR_FILE" >&2
  exit 1
fi

read -r active_color < "$ACTIVE_COLOR_FILE"

case "$active_color" in
  blue)
    previous_color="green"
    previous_port="8082"
    ;;
  green)
    previous_color="blue"
    previous_port="8081"
    ;;
  *)
    echo "Invalid active environment: $active_color" >&2
    exit 1
    ;;
esac

active_container="belog-${active_color}"
previous_container="belog-${previous_color}"
health_check_url="http://127.0.0.1:${previous_port}/actuator/health"
upstream_backup=""
upstream_changes_staged=false
previous_container_started=false
rollback_succeeded=false

cleanup() {
  status=$?
  trap - EXIT

  if [[ "$rollback_succeeded" != true ]]; then
    if [[ "$upstream_changes_staged" == true && -n "$upstream_backup" ]]; then
      "${ROOT_COMMAND[@]}" install -m 644 "$upstream_backup" "$NGINX_UPSTREAM_PATH" || true
      "${ROOT_COMMAND[@]}" nginx -t >/dev/null 2>&1 \
        && "${ROOT_COMMAND[@]}" nginx -s reload >/dev/null 2>&1 \
        || true
    fi

    if [[ "$previous_container_started" == true ]]; then
      docker stop --time 10 "$previous_container" >/dev/null 2>&1 || true
    fi
  fi

  if [[ -n "$upstream_backup" ]]; then
    "${ROOT_COMMAND[@]}" rm -f "$upstream_backup"
  fi

  rm -f "${DEPLOY_DIR}/belog-upstream.inc.rollback"

  exit "$status"
}

trap cleanup EXIT

if ! docker container inspect "$previous_container" >/dev/null 2>&1; then
  echo "Previous container is not available for rollback: $previous_container" >&2
  exit 1
fi

if [[ "$(docker inspect --format '{{.State.Running}}' "$previous_container")" != true ]]; then
  docker start "$previous_container" >/dev/null
  previous_container_started=true
fi

health_check_succeeded=false
for ((attempt = 1; attempt <= HEALTH_CHECK_ATTEMPTS; attempt++)); do
  if curl --fail --silent --show-error \
    --max-time "$HEALTH_CHECK_TIMEOUT_SECONDS" \
    "$health_check_url" >/dev/null; then
    health_check_succeeded=true
    break
  fi

  echo "Rollback health check attempt ${attempt}/${HEALTH_CHECK_ATTEMPTS} failed."
  if [[ "$(docker inspect --format '{{.State.Running}}' "$previous_container" 2>/dev/null)" != true ]]; then
    echo "Previous container stopped before becoming healthy." >&2
    break
  fi

  if ((attempt < HEALTH_CHECK_ATTEMPTS)); then
    sleep "$HEALTH_CHECK_INTERVAL_SECONDS"
  fi
done

if [[ "$health_check_succeeded" != true ]]; then
  echo "Previous environment failed health check: $health_check_url" >&2
  docker logs --tail 100 "$previous_container" >&2 || true
  exit 1
fi

if ! "${ROOT_COMMAND[@]}" test -f "$NGINX_UPSTREAM_PATH"; then
  echo "Current Nginx upstream configuration is missing: $NGINX_UPSTREAM_PATH" >&2
  exit 1
fi

upstream_backup="$(mktemp "${DEPLOY_DIR}/rollback-upstream.XXXXXX")"
"${ROOT_COMMAND[@]}" cp "$NGINX_UPSTREAM_PATH" "$upstream_backup"

printf 'server 127.0.0.1:%s max_fails=3 fail_timeout=10s;\n' "$previous_port" \
  > "${DEPLOY_DIR}/belog-upstream.inc.rollback"

upstream_changes_staged=true
"${ROOT_COMMAND[@]}" install -m 644 \
  "${DEPLOY_DIR}/belog-upstream.inc.rollback" "$NGINX_UPSTREAM_PATH"

if ! "${ROOT_COMMAND[@]}" nginx -t; then
  echo "Rollback Nginx configuration validation failed. Keeping $active_color active." >&2
  exit 1
fi

if ! "${ROOT_COMMAND[@]}" nginx -s reload; then
  echo "Rollback Nginx reload failed. Restoring $active_color." >&2
  exit 1
fi

previous_image="$(docker inspect --format '{{ index .Config.Labels "belog.image" }}' "$previous_container")"
if [[ -z "$previous_image" ]]; then
  echo "Previous container image metadata is missing." >&2
  exit 1
fi

printf '%s\n' "$previous_color" > "${ACTIVE_COLOR_FILE}.tmp"
mv "${ACTIVE_COLOR_FILE}.tmp" "$ACTIVE_COLOR_FILE"

printf '%s\n' "$previous_image" > "${ACTIVE_IMAGE_FILE}.tmp"
mv "${ACTIVE_IMAGE_FILE}.tmp" "$ACTIVE_IMAGE_FILE"

rm -f "${DEPLOY_DIR}/belog-upstream.inc.rollback"
rollback_succeeded=true

docker stop --time 30 "$active_container" >/dev/null 2>&1 || true
echo "Rolled back from $active_color to $previous_color using $previous_image"
