#!/usr/bin/env bash

set -Eeuo pipefail

DEPLOY_DIR="${BELOG_DEPLOY_DIR:-${HOME}/belog}"
COMPOSE_FILE="${DEPLOY_DIR}/compose.yaml"
DOMAIN="${BELOG_DOMAIN:-}"

if [[ ! "$DOMAIN" =~ ^[a-zA-Z0-9.-]+$ ]] || [[ "$DOMAIN" != *.* ]]; then
  echo "BELOG_DOMAIN must be a valid domain name." >&2
  exit 1
fi

if [[ ! -r "$COMPOSE_FILE" ]]; then
  echo "Compose file is missing: $COMPOSE_FILE" >&2
  exit 1
fi

docker compose --project-name belog --file "$COMPOSE_FILE" run --rm certbot \
  renew \
  --cert-name "$DOMAIN" \
  --webroot \
  --webroot-path /var/www/certbot \
  --quiet

docker exec belog-nginx nginx -t
docker exec belog-nginx nginx -s reload
