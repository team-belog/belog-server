#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 9 ]]; then
  echo "Usage: $0 <delay-seconds> <active-color-file> <expected-color> <previous-color> <compose-file> <env-file> <spring-profile> <blue-image> <green-image>" >&2
  exit 1
fi

DELAY_SECONDS="$1"
ACTIVE_COLOR_FILE="$2"
EXPECTED_COLOR="$3"
PREVIOUS_COLOR="$4"
COMPOSE_FILE="$5"
ENV_FILE="$6"
SPRING_PROFILE="$7"
BLUE_IMAGE="$8"
GREEN_IMAGE="$9"

if [[ ! "$DELAY_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Cleanup delay must be a positive integer." >&2
  exit 1
fi

if [[ ! "$EXPECTED_COLOR" =~ ^(blue|green)$ ]] \
  || [[ ! "$PREVIOUS_COLOR" =~ ^(blue|green)$ ]]; then
  echo "Invalid deployment color." >&2
  exit 1
fi

sleep "$DELAY_SECONDS"

current_color=""
if [[ -f "$ACTIVE_COLOR_FILE" ]]; then
  read -r current_color < "$ACTIVE_COLOR_FILE"
fi

if [[ "$current_color" != "$EXPECTED_COLOR" ]]; then
  echo "Skipping cleanup because the active environment changed to: ${current_color:-unknown}"
  exit 0
fi

if [[ -n "$(docker ps --quiet --filter "name=^/belog-${PREVIOUS_COLOR}$")" ]]; then
  BELOG_BLUE_IMAGE="$BLUE_IMAGE" \
    BELOG_GREEN_IMAGE="$GREEN_IMAGE" \
    BELOG_ENV_FILE="$ENV_FILE" \
    BELOG_SPRING_PROFILE="$SPRING_PROFILE" \
    docker compose --project-name belog --file "$COMPOSE_FILE" \
      stop --timeout 30 "$PREVIOUS_COLOR"
  echo "Stopped previous service after rollback window: $PREVIOUS_COLOR"
else
  echo "Previous service is already stopped or missing: $PREVIOUS_COLOR"
fi
