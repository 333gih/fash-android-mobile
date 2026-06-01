#!/usr/bin/env bash
# Writes upload keystore + local.properties for CI (GitHub Actions).
# Expects: ANDROID_UPLOAD_KEYSTORE_BASE64, ANDROID_UPLOAD_KEYSTORE_PASSWORD,
#          ANDROID_UPLOAD_KEY_ALIAS, ANDROID_UPLOAD_KEY_PASSWORD
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

for var in KEYSTORE_B64 STORE_PW KEY_ALIAS KEY_PW; do
  if [[ -z "${!var:-}" ]]; then
    echo "::error::Missing env ${var} (map from GitHub secrets ANDROID_UPLOAD_*)."
    exit 1
  fi
done

KEYSTORE_PATH="${RUNNER_TEMP:-/tmp}/fash-upload.keystore"
echo "${KEYSTORE_B64}" | base64 --decode > "${KEYSTORE_PATH}"

REL_PATH="ci-upload.keystore"
cp "${KEYSTORE_PATH}" "${ROOT}/${REL_PATH}"

{
  echo "FASH_RELEASE_STORE_FILE=${REL_PATH}"
  echo "FASH_RELEASE_STORE_PASSWORD=${STORE_PW}"
  echo "FASH_RELEASE_KEY_ALIAS=${KEY_ALIAS}"
  echo "FASH_RELEASE_KEY_PASSWORD=${KEY_PW}"
} >> local.properties

echo "Release signing configured (alias=${KEY_ALIAS}, store=${REL_PATH})"
