#!/usr/bin/env bash

set -Eeuo pipefail

DEPLOY_DIR="${BELOG_DEPLOY_DIR:-${HOME}/belog}"
DOMAIN="${BELOG_DOMAIN:-}"
BEGIN_MARKER="# BEGIN BELOG MANAGED MAINTENANCE"
END_MARKER="# END BELOG MANAGED MAINTENANCE"
RENEWAL_SCRIPT="${DEPLOY_DIR}/renew-certificate.sh"
IMAGE_PRUNE_SCRIPT="${DEPLOY_DIR}/prune-images.sh"
RENEWAL_LOG="${DEPLOY_DIR}/certificate-renewal.log"
IMAGE_PRUNE_LOG="${DEPLOY_DIR}/image-prune.log"

if [[ ! "$DOMAIN" =~ ^[a-zA-Z0-9.-]+$ ]] || [[ "$DOMAIN" != *.* ]]; then
  echo "BELOG_DOMAIN must be a valid domain name." >&2
  exit 1
fi

for required_command in crontab docker; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    echo "Required command is not installed: $required_command" >&2
    exit 1
  fi
done

for required_script in "$RENEWAL_SCRIPT" "$IMAGE_PRUNE_SCRIPT"; do
  if [[ ! -x "$required_script" ]]; then
    echo "Maintenance script is missing or not executable: $required_script" >&2
    exit 1
  fi
done

current_crontab="$(crontab -l 2>/dev/null || true)"
updated_crontab="$(mktemp "${DEPLOY_DIR}/crontab.XXXXXX")"

cleanup() {
  rm -f "$updated_crontab"
}

trap cleanup EXIT

printf '%s\n' "$current_crontab" \
  | awk -v begin="$BEGIN_MARKER" -v end="$END_MARKER" '
      $0 == begin { managed = 1; next }
      $0 == end { managed = 0; next }
      !managed {
        lines[++count] = $0
        if ($0 !~ /^[[:space:]]*$/) {
          last_content = count
        }
      }
      END {
        for (line_number = 1; line_number <= last_content; line_number++) {
          print lines[line_number]
        }
      }
    ' \
  > "$updated_crontab"

{
  printf '\n%s\n' "$BEGIN_MARKER"
  printf 'PATH=/usr/local/bin:/usr/bin:/bin\n'
  printf '0 4 * * * BELOG_DOMAIN=%s "%s" >> "%s" 2>&1\n' \
    "$DOMAIN" "$RENEWAL_SCRIPT" "$RENEWAL_LOG"
  printf '30 4 * * * "%s" >> "%s" 2>&1\n' \
    "$IMAGE_PRUNE_SCRIPT" "$IMAGE_PRUNE_LOG"
  printf '%s\n' "$END_MARKER"
} >> "$updated_crontab"

crontab "$updated_crontab"
