#!/usr/bin/env bash

set -Eeuo pipefail

IMAGE_MAX_AGE="${BELOG_IMAGE_MAX_AGE:-168h}"

if [[ ! "$IMAGE_MAX_AGE" =~ ^[1-9][0-9]*h$ ]]; then
  echo "BELOG_IMAGE_MAX_AGE must be a positive number of hours." >&2
  exit 1
fi

docker image prune \
  --all \
  --force \
  --filter "until=${IMAGE_MAX_AGE}" \
  --filter "label=org.com.belog.application=belog-server"
