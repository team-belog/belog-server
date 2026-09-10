#!/usr/bin/env bash

set -Eeuo pipefail

if [[ $# -ne 8 ]]; then
  echo "Usage: $0 <image-ref> <compose-file> <nginx-config> <nginx-bootstrap-config> <cleanup-script> <renewal-script> <image-prune-script> <maintenance-config-script>" >&2
  exit 1
fi

IMAGE_REF="$1"
COMPOSE_FILE_SOURCE="$2"
NGINX_CONFIG_SOURCE="$3"
NGINX_BOOTSTRAP_CONFIG_SOURCE="$4"
CLEANUP_SCRIPT_SOURCE="$5"
RENEWAL_SCRIPT_SOURCE="$6"
IMAGE_PRUNE_SCRIPT_SOURCE="$7"
MAINTENANCE_CONFIG_SCRIPT_SOURCE="$8"
DEPLOY_DIR="${BELOG_DEPLOY_DIR:-${HOME}/belog}"
COMPOSE_FILE="${DEPLOY_DIR}/compose.yaml"
NGINX_DIR="${DEPLOY_DIR}/nginx"
NGINX_CONFIG_PATH="${NGINX_DIR}/belog.conf"
NGINX_UPSTREAM_PATH="${NGINX_DIR}/belog-upstream.inc"
HEALTH_CHECK_ATTEMPTS="${BELOG_HEALTH_CHECK_ATTEMPTS:-30}"
HEALTH_CHECK_INTERVAL_SECONDS="${BELOG_HEALTH_CHECK_INTERVAL_SECONDS:-2}"
HEALTH_CHECK_TIMEOUT_SECONDS="${BELOG_HEALTH_CHECK_TIMEOUT_SECONDS:-3}"
ROLLBACK_WINDOW_SECONDS="${BELOG_ROLLBACK_WINDOW_SECONDS:-600}"
SPRING_PROFILE="${BELOG_SPRING_PROFILE:-prod}"
DOMAIN="${BELOG_DOMAIN:-}"
CERTBOT_EMAIL="${BELOG_CERTBOT_EMAIL:-}"
ENV_FILE="${DEPLOY_DIR}/.env.${SPRING_PROFILE}"
ACTIVE_COLOR_FILE="${DEPLOY_DIR}/active-color"
ACTIVE_IMAGE_FILE="${DEPLOY_DIR}/active-image"
CANDIDATE_COLOR_FILE="${DEPLOY_DIR}/candidate-color"
CANDIDATE_IMAGE_FILE="${DEPLOY_DIR}/candidate-image"
CERTIFICATE_DIR="${DEPLOY_DIR}/certbot/conf/live/${DOMAIN}"

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

if [[ ! "$SPRING_PROFILE" =~ ^[a-zA-Z0-9_-]+$ ]]; then
  echo "Spring profile contains unsupported characters: $SPRING_PROFILE" >&2
  exit 1
fi

if [[ ! "$DOMAIN" =~ ^[a-zA-Z0-9.-]+$ ]] || [[ "$DOMAIN" != *.* ]]; then
  echo "BELOG_DOMAIN must be a valid domain name." >&2
  exit 1
fi

for required_file in \
  "$ENV_FILE" \
  "$COMPOSE_FILE_SOURCE" \
  "$NGINX_CONFIG_SOURCE" \
  "$NGINX_BOOTSTRAP_CONFIG_SOURCE" \
  "$CLEANUP_SCRIPT_SOURCE" \
  "$RENEWAL_SCRIPT_SOURCE" \
  "$IMAGE_PRUNE_SCRIPT_SOURCE" \
  "$MAINTENANCE_CONFIG_SCRIPT_SOURCE" \
  "${NGINX_DIR}/swagger.htpasswd"; do
  if [[ ! -r "$required_file" ]]; then
    echo "Required deployment file is missing: $required_file" >&2
    exit 1
  fi
done

if [[ ! "$HEALTH_CHECK_ATTEMPTS" =~ ^[1-9][0-9]*$ ]] \
  || [[ ! "$HEALTH_CHECK_INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]] \
  || [[ ! "$HEALTH_CHECK_TIMEOUT_SECONDS" =~ ^[1-9][0-9]*$ ]] \
  || [[ ! "$ROLLBACK_WINDOW_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Health check and rollback settings must be positive integers." >&2
  exit 1
fi

mkdir -p \
  "$DEPLOY_DIR" \
  "$NGINX_DIR" \
  "${DEPLOY_DIR}/certbot/conf" \
  "${DEPLOY_DIR}/certbot/www"
install -m 600 "$COMPOSE_FILE_SOURCE" "$COMPOSE_FILE"

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

image_for_container() {
  local container_name="$1"
  local image
  image="$(docker inspect --format '{{ index .Config.Labels "belog.image" }}' "$container_name" 2>/dev/null || true)"
  if [[ -z "$image" || "$image" == "<no value>" ]]; then
    image="$IMAGE_REF"
  fi
  printf '%s\n' "$image"
}

blue_image="$(image_for_container belog-blue)"
green_image="$(image_for_container belog-green)"
if [[ "$target_color" == blue ]]; then
  blue_image="$IMAGE_REF"
else
  green_image="$IMAGE_REF"
fi

compose() {
  BELOG_BLUE_IMAGE="$blue_image" \
    BELOG_GREEN_IMAGE="$green_image" \
    BELOG_ENV_FILE="$ENV_FILE" \
    BELOG_SPRING_PROFILE="$SPRING_PROFILE" \
    docker compose --project-name belog --file "$COMPOSE_FILE" "$@"
}

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
    install -m 644 "${nginx_backup_dir}/belog.conf" "$NGINX_CONFIG_PATH"
  else
    rm -f "$NGINX_CONFIG_PATH"
  fi

  if [[ "$nginx_upstream_existed" == true ]]; then
    install -m 644 "${nginx_backup_dir}/belog-upstream.inc" "$NGINX_UPSTREAM_PATH"
  else
    rm -f "$NGINX_UPSTREAM_PATH"
  fi
}

restore_running_nginx() {
  if docker container inspect belog-nginx >/dev/null 2>&1; then
    compose up --detach --no-deps nginx >/dev/null 2>&1 || true
    docker exec belog-nginx nginx -t >/dev/null 2>&1 \
      && docker exec belog-nginx nginx -s reload >/dev/null 2>&1 \
      || true
  fi
}

cleanup() {
  status=$?
  trap - EXIT

  if [[ "$deployment_succeeded" != true ]]; then
    if [[ "$nginx_changes_staged" == true ]]; then
      restore_nginx_configuration || true
      restore_running_nginx
    fi

    if [[ "$container_started" == true ]]; then
      compose stop --timeout 10 "$target_color" >/dev/null 2>&1 || true
    fi

    rm -f "$CANDIDATE_COLOR_FILE" "$CANDIDATE_IMAGE_FILE"
  fi

  if [[ -n "$nginx_backup_dir" ]]; then
    rm -rf "$nginx_backup_dir"
  fi

  exit "$status"
}

trap cleanup EXIT

compose pull "$target_color"
compose rm --force --stop "$target_color" >/dev/null 2>&1 || true
compose up --detach --no-deps "$target_color"
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

if [[ -f "$NGINX_CONFIG_PATH" ]]; then
  nginx_config_existed=true
  cp "$NGINX_CONFIG_PATH" "${nginx_backup_dir}/belog.conf"
fi

if [[ -f "$NGINX_UPSTREAM_PATH" ]]; then
  nginx_upstream_existed=true
  cp "$NGINX_UPSTREAM_PATH" "${nginx_backup_dir}/belog-upstream.inc"
fi

render_nginx_config() {
  local source_path="$1"
  local target_path="$2"

  sed "s/__BELOG_DOMAIN__/${DOMAIN}/g" "$source_path" > "$target_path"
  if grep -q '__BELOG_DOMAIN__' "$target_path"; then
    echo "Nginx domain placeholder was not fully rendered." >&2
    return 1
  fi
}

certificate_exists() {
  [[ -r "${CERTIFICATE_DIR}/fullchain.pem" && -r "${CERTIFICATE_DIR}/privkey.pem" ]]
}

if ! certificate_exists; then
  if [[ -z "$CERTBOT_EMAIL" || "$CERTBOT_EMAIL" != *@*.* ]]; then
    echo "BELOG_CERTBOT_EMAIL must be set to issue the initial certificate." >&2
    exit 1
  fi

  bootstrap_config="${nginx_backup_dir}/belog-bootstrap.conf.rendered"
  render_nginx_config "$NGINX_BOOTSTRAP_CONFIG_SOURCE" "$bootstrap_config"
  install -m 644 "$bootstrap_config" "$NGINX_CONFIG_PATH"
  nginx_changes_staged=true

  if ! compose run --rm --no-deps nginx nginx -t; then
    echo "Bootstrap Nginx configuration validation failed." >&2
    exit 1
  fi

  if ! compose up --detach --no-deps nginx; then
    echo "Bootstrap Nginx container failed to start." >&2
    exit 1
  fi

  if ! docker exec belog-nginx nginx -t \
    || ! docker exec belog-nginx nginx -s reload; then
    echo "Bootstrap Nginx configuration activation failed." >&2
    exit 1
  fi

  if ! compose run --rm --no-deps certbot \
    certonly \
    --webroot \
    --webroot-path /var/www/certbot \
    --domain "$DOMAIN" \
    --email "$CERTBOT_EMAIL" \
    --agree-tos \
    --no-eff-email \
    --non-interactive; then
    echo "Initial certificate issuance failed for: $DOMAIN" >&2
    exit 1
  fi

  if ! certificate_exists; then
    echo "Certificate files are missing after initial issuance: $CERTIFICATE_DIR" >&2
    exit 1
  fi
fi

rendered_nginx_config="${nginx_backup_dir}/belog.conf.rendered"
render_nginx_config "$NGINX_CONFIG_SOURCE" "$rendered_nginx_config"
install -m 644 "$rendered_nginx_config" "$NGINX_CONFIG_PATH"
printf 'server belog-%s:8080 max_fails=3 fail_timeout=10s;\n' "$target_color" \
  > "$NGINX_UPSTREAM_PATH"
nginx_changes_staged=true

if ! compose run --rm --no-deps nginx nginx -t; then
  echo "Nginx configuration validation failed. Keeping the existing environment active." >&2
  exit 1
fi

if ! compose up --detach --no-deps nginx; then
  echo "Nginx container failed to start. Keeping the existing environment active." >&2
  exit 1
fi

if ! docker exec belog-nginx nginx -t; then
  echo "Running Nginx configuration validation failed. Keeping the existing environment active." >&2
  exit 1
fi

if ! docker exec belog-nginx nginx -s reload; then
  echo "Nginx reload failed. Restoring the previous upstream." >&2
  exit 1
fi

if [[ -n "$active_color" ]]; then
  installed_cleanup_script="${DEPLOY_DIR}/stop-previous-container.sh"
  cleanup_log="${DEPLOY_DIR}/cleanup-${active_color}.log"
  install -m 700 "$CLEANUP_SCRIPT_SOURCE" "$installed_cleanup_script"
  nohup "$installed_cleanup_script" \
    "$ROLLBACK_WINDOW_SECONDS" \
    "$ACTIVE_COLOR_FILE" \
    "$target_color" \
    "$active_color" \
    "$COMPOSE_FILE" \
    "$ENV_FILE" \
    "$SPRING_PROFILE" \
    "$blue_image" \
    "$green_image" \
    > "$cleanup_log" 2>&1 &
fi

printf '%s\n' "$target_color" > "${ACTIVE_COLOR_FILE}.tmp"
mv "${ACTIVE_COLOR_FILE}.tmp" "$ACTIVE_COLOR_FILE"

printf '%s\n' "$IMAGE_REF" > "${ACTIVE_IMAGE_FILE}.tmp"
mv "${ACTIVE_IMAGE_FILE}.tmp" "$ACTIVE_IMAGE_FILE"

rm -f "$CANDIDATE_COLOR_FILE" "$CANDIDATE_IMAGE_FILE"

installed_renewal_script="${DEPLOY_DIR}/renew-certificate.sh"
installed_image_prune_script="${DEPLOY_DIR}/prune-images.sh"
installed_maintenance_config_script="${DEPLOY_DIR}/configure-maintenance.sh"
install -m 700 "$RENEWAL_SCRIPT_SOURCE" "$installed_renewal_script"
install -m 700 "$IMAGE_PRUNE_SCRIPT_SOURCE" "$installed_image_prune_script"
install -m 700 "$MAINTENANCE_CONFIG_SCRIPT_SOURCE" "$installed_maintenance_config_script"

deployment_succeeded=true
echo "Activated $IMAGE_REF on $target_color through the Nginx container"

BELOG_DOMAIN="$DOMAIN" "$installed_maintenance_config_script"
echo "Configured certificate renewal and image cleanup schedules"
