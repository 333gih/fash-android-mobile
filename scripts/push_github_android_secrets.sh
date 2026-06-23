#!/usr/bin/env bash
# Push Android CI secrets from secrets/android-release.env → GitHub Actions.
# Usage: ./scripts/push_github_android_secrets.sh [secrets/android-release.env] [owner/repo]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ENV_FILE="${1:-secrets/android-release.env}"
REPO="${2:-}"

if ! command -v gh >/dev/null 2>&1; then
  echo "error: install GitHub CLI (gh) and run: gh auth login" >&2
  exit 1
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "error: missing ${ENV_FILE} — copy secrets/android-release.env.example" >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a
source <(grep -v '^\s*#' "${ENV_FILE}" | grep -v '^\s*$' | sed 's/\r$//')
set +a

REPO_ARGS=()
if [[ -n "${REPO}" ]]; then
  REPO_ARGS=(-R "${REPO}")
fi

set_secret() {
  local name="$1"
  local value="${2:-}"
  if [[ -z "${value}" ]]; then
    echo "skip ${name} (empty)"
    return
  fi
  echo "set  ${name}"
  printf '%s' "${value}" | gh secret set "${name}" "${REPO_ARGS[@]}"
}

read_repo_file() {
  local rel="$1"
  local full="${ROOT}/${rel}"
  [[ -f "${full}" ]] || { echo "error: file not found: ${full}" >&2; exit 1; }
  cat "${full}"
}

file_to_b64() {
  local rel="$1"
  local full="${ROOT}/${rel}"
  [[ -f "${full}" ]] || { echo "error: file not found: ${full}" >&2; exit 1; }
  base64 < "${full}" | tr -d '\n'
}

DEV_ENV="${ANDROID_DEV_ENV:-}"
if [[ -z "${DEV_ENV}" && -n "${ANDROID_DEV_ENV_PATH:-}" ]]; then
  DEV_ENV="$(read_repo_file "${ANDROID_DEV_ENV_PATH}")"
fi

PROD_ENV="${ANDROID_PROD_ENV:-}"
if [[ -z "${PROD_ENV}" && -n "${ANDROID_PROD_ENV_PATH:-}" ]]; then
  PROD_ENV="$(read_repo_file "${ANDROID_PROD_ENV_PATH}")"
fi

KEYSTORE_B64="${ANDROID_UPLOAD_KEYSTORE_BASE64:-}"
if [[ -z "${KEYSTORE_B64}" && -n "${ANDROID_UPLOAD_KEYSTORE_PATH:-}" ]]; then
  if [[ -f "${ROOT}/${ANDROID_UPLOAD_KEYSTORE_PATH}" ]]; then
    KEYSTORE_B64="$(file_to_b64 "${ANDROID_UPLOAD_KEYSTORE_PATH}")"
  else
    echo "skip ANDROID_UPLOAD_KEYSTORE_BASE64 (keystore file missing)"
  fi
fi

PLAY_JSON="${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON:-}"
if [[ -z "${PLAY_JSON}" && -n "${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH:-}" ]]; then
  if [[ -f "${ROOT}/${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH}" ]]; then
    PLAY_JSON="$(read_repo_file "${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH}")"
  else
    echo "skip GOOGLE_PLAY_SERVICE_ACCOUNT_JSON (file missing)"
  fi
fi

set_secret "ANDROID_DEV_ENV" "${DEV_ENV}"
set_secret "ANDROID_PROD_ENV" "${PROD_ENV}"
set_secret "ANDROID_UPLOAD_KEYSTORE_BASE64" "${KEYSTORE_B64}"
set_secret "ANDROID_UPLOAD_KEYSTORE_PASSWORD" "${ANDROID_UPLOAD_KEYSTORE_PASSWORD:-}"
set_secret "ANDROID_UPLOAD_KEY_ALIAS" "${ANDROID_UPLOAD_KEY_ALIAS:-}"
set_secret "ANDROID_UPLOAD_KEY_PASSWORD" "${ANDROID_UPLOAD_KEY_PASSWORD:-}"
set_secret "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON" "${PLAY_JSON}"
set_secret "PLAY_EXPECTED_UPLOAD_SHA1" "${PLAY_EXPECTED_UPLOAD_SHA1:-}"

echo ""
echo "Done. Verify: gh secret list ${REPO_ARGS[*]}"
echo "Then: Actions → Android Build / Android Release → Run workflow"
