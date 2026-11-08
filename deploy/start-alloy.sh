#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <spring-profile> [alloy-config]" >&2
  exit 1
fi

SPRING_PROFILE="$1"
DEPLOY_DIR="${BELOG_DEPLOY_DIR:-${HOME}/belog}"
COMPOSE_FILE="${DEPLOY_DIR}/compose.yaml"
ENV_FILE="${DEPLOY_DIR}/.env.${SPRING_PROFILE}"
ALLOY_ENV_FILE="${DEPLOY_DIR}/.env.alloy.${SPRING_PROFILE}"
ALLOY_DIR="${DEPLOY_DIR}/alloy"
ALLOY_CONFIG_PATH="${ALLOY_DIR}/config.alloy"
ALLOY_CONFIG_SOURCE="${2:-$ALLOY_CONFIG_PATH}"
ALLOY_CONTAINER_NAME="belog-alloy"
ALLOY_STARTUP_CHECK_SECONDS="${BELOG_ALLOY_STARTUP_CHECK_SECONDS:-15}"

if [[ ! "$SPRING_PROFILE" =~ ^[a-zA-Z0-9_-]+$ ]]; then
  echo "Spring profile contains unsupported characters: $SPRING_PROFILE" >&2
  exit 1
fi

if [[ ! "$ALLOY_STARTUP_CHECK_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "BELOG_ALLOY_STARTUP_CHECK_SECONDS must be a positive integer." >&2
  exit 1
fi

for required_file in \
  "$COMPOSE_FILE" \
  "$ENV_FILE" \
  "$ALLOY_ENV_FILE" \
  "$ALLOY_CONFIG_SOURCE"; do
  if [[ ! -f "$required_file" || ! -r "$required_file" ]]; then
    echo "Required monitoring file is missing: $required_file" >&2
    exit 1
  fi
done

compose() {
  BELOG_ENV_FILE="$ENV_FILE" \
    BELOG_ALLOY_ENV_FILE="$ALLOY_ENV_FILE" \
    BELOG_SPRING_PROFILE="$SPRING_PROFILE" \
    docker compose --project-name belog --file "$COMPOSE_FILE" "$@"
}

alloy_image="$(compose config --images alloy)"

if ! docker run --rm \
  --volume "${ALLOY_CONFIG_SOURCE}:/tmp/candidate.alloy:ro" \
  "$alloy_image" validate /tmp/candidate.alloy; then
  echo "Grafana Alloy configuration validation failed. Keeping the existing Alloy configuration and container." >&2
  exit 1
fi

if [[ "$ALLOY_CONFIG_SOURCE" != "$ALLOY_CONFIG_PATH" ]]; then
  install -d -m 755 "$ALLOY_DIR"
  install -m 644 "$ALLOY_CONFIG_SOURCE" "$ALLOY_CONFIG_PATH"
fi

compose up --detach --no-deps --force-recreate alloy
sleep "$ALLOY_STARTUP_CHECK_SECONDS"

if [[ "$(docker inspect --format '{{.State.Running}}' "$ALLOY_CONTAINER_NAME" 2>/dev/null)" != true ]] \
  || [[ "$(docker inspect --format '{{.RestartCount}}' "$ALLOY_CONTAINER_NAME" 2>/dev/null)" != 0 ]]; then
  echo "Grafana Alloy did not stay running after ${ALLOY_STARTUP_CHECK_SECONDS} seconds." >&2
  docker logs --tail 50 "$ALLOY_CONTAINER_NAME" >&2 || true
  exit 1
fi

alloy_error_logs="$(docker logs "$ALLOY_CONTAINER_NAME" 2>&1 | grep 'level=error' || true)"
if [[ -n "$alloy_error_logs" ]]; then
  echo "Grafana Alloy reported errors within ${ALLOY_STARTUP_CHECK_SECONDS} seconds after startup." >&2
  printf '%s\n' "$alloy_error_logs" | tail -n 20 >&2
  exit 1
fi

echo "Grafana Alloy is running without startup errors"
