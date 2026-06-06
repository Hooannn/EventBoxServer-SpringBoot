#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() {
  printf '[release] %s\n' "$*"
}

fail() {
  printf '[release] ERROR: %s\n' "$*" >&2
  exit 1
}

load_env_file() {
  local env_file="$1"

  [[ -f "$env_file" ]] || fail "ENV_FILE does not exist: $env_file"
  # shellcheck disable=SC1090
  set -a
  source "$env_file"
  set +a
}

require_env() {
  local name="$1"
  [[ -n "${!name:-}" ]] || fail "Missing required environment variable: $name"
}

extract_json_string_field() {
  local field="$1"
  sed -n 's/.*"'"$field"'":"\([^"]*\)".*/\1/p'
}

get_docker_hub_token() {
  local payload
  payload="$(printf '{"identifier":"%s","secret":"%s"}' "$DOCKER_USERNAME" "$DOCKER_PASSWORD")"

  curl -fsSL "$REGISTRY_AUTH_URL" \
    -H 'Content-Type: application/json' \
    -d "$payload" |
    extract_json_string_field 'access_token'
}

fetch_registry_tags() {
  local page=1
  local response next_page page_tags
  local tag_line

  while :; do
    response="$(
      curl -fsSL --get \
        "${REGISTRY_API_BASE}/namespaces/${IMAGE_NAMESPACE}/repositories/${IMAGE_REPOSITORY}/tags" \
        -H "Authorization: Bearer ${HUB_TOKEN}" \
        --data-urlencode "page_size=100" \
        --data-urlencode "page=${page}"
    )"

    page_tags=""
    while IFS= read -r tag_line; do
      [[ -n "$tag_line" ]] || continue
      page_tags+="${tag_line}"$'\n'
    done < <(
      printf '%s' "$response" |
        grep -o '"name":"[^"]*"' |
        sed 's/^"name":"//; s/"$//'
    )

    if [[ -n "$page_tags" ]]; then
      printf '%s' "$page_tags"
    fi

    next_page="$(
      printf '%s' "$response" |
        extract_json_string_field 'next'
    )"

    [[ -n "$next_page" ]] || break
    page=$((page + 1))
  done
}

compute_next_tag() {
  local best_major=-1
  local best_minor=-1
  local tag major minor
  local regex="^${TAG_PREFIX//./\\.}([0-9]+)\\.([0-9]+)$"

  while IFS= read -r tag; do
    [[ -n "$tag" ]] || continue
    if [[ "$tag" =~ $regex ]]; then
      major="${BASH_REMATCH[1]}"
      minor="${BASH_REMATCH[2]}"
      if ((major > best_major || (major == best_major && minor > best_minor))); then
        best_major="$major"
        best_minor="$minor"
      fi
    fi
  done

  if ((best_major < 0)); then
    printf '%s\n' "$INITIAL_TAG"
  else
    printf '%s%s.%s\n' "$TAG_PREFIX" "$best_major" "$((best_minor + 1))"
  fi
}

if [[ -n "${ENV_FILE:-}" ]]; then
  load_env_file "$ENV_FILE"
elif [[ -f .env ]]; then
  load_env_file .env
fi

require_env DOCKER_USERNAME
require_env DOCKER_PASSWORD
require_env IMAGE_NAME

TEST_COMMAND="${TEST_COMMAND:-./mvnw test}"
DOCKERFILE_PATH="${DOCKERFILE_PATH:-Dockerfile}"
BUILD_CONTEXT="${BUILD_CONTEXT:-.}"
TAG_PREFIX="${TAG_PREFIX:-v}"
INITIAL_TAG="${INITIAL_TAG:-${TAG_PREFIX}1.0}"
REGISTRY_API_BASE="${REGISTRY_API_BASE:-https://hub.docker.com/v2}"
REGISTRY_AUTH_URL="${REGISTRY_AUTH_URL:-https://hub.docker.com/v2/auth/token}"

case "$IMAGE_NAME" in
*/*) ;;
*) fail "IMAGE_NAME must be in namespace/repository form, got: $IMAGE_NAME" ;;
esac

IMAGE_NAMESPACE="${IMAGE_NAME%%/*}"
IMAGE_REPOSITORY="${IMAGE_NAME#*/}"

log "Running tests: ${TEST_COMMAND}"
bash -lc "$TEST_COMMAND"

log "Authenticating to Docker Hub"
HUB_TOKEN="$(get_docker_hub_token)"

log "Inspecting existing tags for ${IMAGE_NAME}"
NEXT_TAG="$(
  fetch_registry_tags |
    compute_next_tag
)"

log "Next tag resolved to ${IMAGE_NAME}:${NEXT_TAG}"

log "Logging in to Docker Hub as ${DOCKER_USERNAME}"
printf '%s' "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

log "Building image ${IMAGE_NAME}:${NEXT_TAG}"
docker buildx build --platform=linux/amd64 \
  -f "$DOCKERFILE_PATH" \
  -t "${IMAGE_NAME}:${NEXT_TAG}" \
  "$BUILD_CONTEXT"

log "Pushing image ${IMAGE_NAME}:${NEXT_TAG}"
docker push "${IMAGE_NAME}:${NEXT_TAG}"

log "Release complete"
