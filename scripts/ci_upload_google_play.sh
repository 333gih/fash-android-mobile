#!/usr/bin/env bash
# Upload prod AAB to Google Play (Jenkins / local CI parity with GitHub Actions).
#
# Required env:
#   GOOGLE_PLAY_SERVICE_ACCOUNT_JSON  — full service account JSON (multiline OK)
# Optional:
#   PLAY_TRACK              — alpha | internal | beta | production (default: alpha)
#   PLAY_PACKAGE_NAME       — default com.pc.fash_android_mobile
#   PLAY_MAPPING_FILE       — R8 mapping.txt path
#
# Usage: bash scripts/ci_upload_google_play.sh path/to/app.aab
set -euo pipefail

AAB="${1:-}"
TRACK="${PLAY_TRACK:-alpha}"
PKG="${PLAY_PACKAGE_NAME:-com.pc.fash_android_mobile}"
MAPPING="${PLAY_MAPPING_FILE:-}"

if [[ -z "${AAB}" || ! -f "${AAB}" ]]; then
  echo "[ERROR] AAB not found: ${AAB:-<missing>}" >&2
  exit 1
fi

if [[ -z "${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON:-}" ]]; then
  echo "[ERROR] GOOGLE_PLAY_SERVICE_ACCOUNT_JSON is required" >&2
  exit 1
fi

JSON_FILE="${RUNNER_TEMP:-/tmp}/play-service-account-$$.json"
printf '%s\n' "${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON}" > "${JSON_FILE}"
chmod 600 "${JSON_FILE}"
trap 'rm -f "${JSON_FILE}"' EXIT

SUPPLY_ARGS=(
  supply
  --aab "${AAB}"
  --track "${TRACK}"
  --json_key "${JSON_FILE}"
  --package_name "${PKG}"
  --skip_upload_apk
  --skip_upload_metadata
  --skip_upload_images
  --skip_upload_screenshots
)

if [[ -n "${MAPPING}" && -f "${MAPPING}" ]]; then
  SUPPLY_ARGS+=(--mapping "${MAPPING}")
fi

run_supply() {
  fastlane "${SUPPLY_ARGS[@]}"
}

if command -v fastlane >/dev/null 2>&1; then
  echo "[INFO] Uploading to Google Play (track=${TRACK}) via fastlane supply"
  run_supply
  exit 0
fi

if command -v docker >/dev/null 2>&1; then
  echo "[INFO] fastlane not on PATH — using fastlanetools/fastlane Docker image"
  AAB_ABS="$(cd "$(dirname "${AAB}")" && pwd)/$(basename "${AAB}")"
  JSON_ABS="$(cd "$(dirname "${JSON_FILE}")" && pwd)/$(basename "${JSON_FILE}")"
  DOCKER_ARGS=(
    run --rm
    -v "${AAB_ABS}:/work/app.aab:ro"
    -v "${JSON_ABS}:/work/play.json:ro"
    fastlanetools/fastlane:latest
    supply
    --aab /work/app.aab
    --track "${TRACK}"
    --json_key /work/play.json
    --package_name "${PKG}"
    --skip_upload_apk
    --skip_upload_metadata
    --skip_upload_images
    --skip_upload_screenshots
  )
  if [[ -n "${MAPPING}" && -f "${MAPPING}" ]]; then
    MAP_ABS="$(cd "$(dirname "${MAPPING}")" && pwd)/$(basename "${MAPPING}")"
    DOCKER_ARGS+=(-v "${MAP_ABS}:/work/mapping.txt:ro" --mapping /work/mapping.txt)
  fi
  docker "${DOCKER_ARGS[@]}"
  exit 0
fi

echo "[ERROR] Install fastlane (gem install fastlane) or Docker for Play upload" >&2
exit 1
