#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 4 ]]; then
  echo "Usage: $0 <delay-seconds> <active-color-file> <expected-color> <container-name>" >&2
  exit 1
fi

DELAY_SECONDS="$1"
ACTIVE_COLOR_FILE="$2"
EXPECTED_COLOR="$3"
CONTAINER_NAME="$4"

if [[ ! "$DELAY_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Cleanup delay must be a positive integer." >&2
  exit 1
fi

if [[ ! "$EXPECTED_COLOR" =~ ^(blue|green)$ ]] \
  || [[ ! "$CONTAINER_NAME" =~ ^belog-(blue|green)$ ]]; then
  echo "Invalid deployment color or container name." >&2
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

if [[ -n "$(docker ps --quiet --filter "name=^/${CONTAINER_NAME}$")" ]]; then
  docker stop --time 30 "$CONTAINER_NAME"
  echo "Stopped previous container after rollback window: $CONTAINER_NAME"
else
  echo "Previous container is already stopped or missing: $CONTAINER_NAME"
fi
