#!/usr/bin/env bash

set -Eeuo pipefail

DEPLOY_DIR="${BELOG_DEPLOY_DIR:-${HOME}/belog}"
HEALTH_CHECK_ATTEMPTS="${BELOG_HEALTH_CHECK_ATTEMPTS:-30}"
HEALTH_CHECK_INTERVAL_SECONDS="${BELOG_HEALTH_CHECK_INTERVAL_SECONDS:-2}"
HEALTH_CHECK_TIMEOUT_SECONDS="${BELOG_HEALTH_CHECK_TIMEOUT_SECONDS:-3}"
SPRING_PROFILE="${BELOG_SPRING_PROFILE:-dev}"
ENV_FILE="${DEPLOY_DIR}/.env.${SPRING_PROFILE}"
COMPOSE_FILE="${DEPLOY_DIR}/compose.yaml"
ACTIVE_COLOR_FILE="${DEPLOY_DIR}/active-color"
ACTIVE_IMAGE_FILE="${DEPLOY_DIR}/active-image"
NGINX_UPSTREAM_PATH="${DEPLOY_DIR}/nginx/belog-upstream.inc"

for command_name in docker curl; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is not installed: $command_name" >&2
    exit 1
  fi
done

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose plugin is not installed." >&2
  exit 1
fi

if [[ ! "$HEALTH_CHECK_ATTEMPTS" =~ ^[1-9][0-9]*$ ]] \
  || [[ ! "$HEALTH_CHECK_INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]] \
  || [[ ! "$HEALTH_CHECK_TIMEOUT_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Health check settings must be positive integers." >&2
  exit 1
fi

for required_file in "$ENV_FILE" "$COMPOSE_FILE" "$ACTIVE_COLOR_FILE" "$NGINX_UPSTREAM_PATH"; do
  if [[ ! -r "$required_file" ]]; then
    echo "Required rollback file is missing: $required_file" >&2
    exit 1
  fi
done

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

if ! docker container inspect "$previous_container" >/dev/null 2>&1; then
  echo "Previous service is not available for rollback: $previous_color" >&2
  exit 1
fi

image_for_container() {
  local container_name="$1"
  local image
  image="$(docker inspect --format '{{ index .Config.Labels "belog.image" }}' "$container_name" 2>/dev/null || true)"
  if [[ -z "$image" || "$image" == "<no value>" ]]; then
    echo "Container image metadata is missing: $container_name" >&2
    return 1
  fi
  printf '%s\n' "$image"
}

previous_image="$(image_for_container "$previous_container")"
active_image="$(image_for_container "$active_container" 2>/dev/null || true)"
if [[ -z "$active_image" ]]; then
  active_image="$previous_image"
fi

if [[ "$active_color" == blue ]]; then
  blue_image="$active_image"
  green_image="$previous_image"
else
  blue_image="$previous_image"
  green_image="$active_image"
fi

compose() {
  BELOG_BLUE_IMAGE="$blue_image" \
    BELOG_GREEN_IMAGE="$green_image" \
    BELOG_ENV_FILE="$ENV_FILE" \
    BELOG_SPRING_PROFILE="$SPRING_PROFILE" \
    docker compose --project-name belog --file "$COMPOSE_FILE" "$@"
}

health_check_url="http://127.0.0.1:${previous_port}/actuator/health"
upstream_backup=""
upstream_changes_staged=false
previous_service_started=false
rollback_succeeded=false

cleanup() {
  status=$?
  trap - EXIT

  if [[ "$rollback_succeeded" != true ]]; then
    if [[ "$upstream_changes_staged" == true && -n "$upstream_backup" ]]; then
      install -m 644 "$upstream_backup" "$NGINX_UPSTREAM_PATH" || true
      docker exec belog-nginx nginx -t >/dev/null 2>&1 \
        && docker exec belog-nginx nginx -s reload >/dev/null 2>&1 \
        || true
    fi

    if [[ "$previous_service_started" == true ]]; then
      compose stop --timeout 10 "$previous_color" >/dev/null 2>&1 || true
    fi
  fi

  if [[ -n "$upstream_backup" ]]; then
    rm -f "$upstream_backup"
  fi

  exit "$status"
}

trap cleanup EXIT

if [[ "$(docker inspect --format '{{.State.Running}}' "$previous_container")" != true ]]; then
  compose start "$previous_color" >/dev/null
  previous_service_started=true
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
    echo "Previous service stopped before becoming healthy." >&2
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

upstream_backup="$(mktemp "${DEPLOY_DIR}/rollback-upstream.XXXXXX")"
cp "$NGINX_UPSTREAM_PATH" "$upstream_backup"

printf 'server belog-%s:8080 max_fails=3 fail_timeout=10s;\n' "$previous_color" \
  > "$NGINX_UPSTREAM_PATH"
upstream_changes_staged=true

if ! compose run --rm --no-deps nginx nginx -t; then
  echo "Rollback Nginx configuration validation failed. Keeping $active_color active." >&2
  exit 1
fi

if ! docker exec belog-nginx nginx -t; then
  echo "Running Nginx configuration validation failed. Keeping $active_color active." >&2
  exit 1
fi

if ! docker exec belog-nginx nginx -s reload; then
  echo "Nginx reload failed. Restoring $active_color." >&2
  exit 1
fi

previous_image="$(image_for_container "$previous_container")"

printf '%s\n' "$previous_color" > "${ACTIVE_COLOR_FILE}.tmp"
mv "${ACTIVE_COLOR_FILE}.tmp" "$ACTIVE_COLOR_FILE"

printf '%s\n' "$previous_image" > "${ACTIVE_IMAGE_FILE}.tmp"
mv "${ACTIVE_IMAGE_FILE}.tmp" "$ACTIVE_IMAGE_FILE"

rollback_succeeded=true
compose stop --timeout 30 "$active_color" >/dev/null 2>&1 || true
echo "Rolled back from $active_color to $previous_color using $previous_image"
