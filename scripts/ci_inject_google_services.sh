#!/usr/bin/env bash
# Optional: replace app/google-services.json from CI secret (after Firebase fingerprint update).
# Env: GOOGLE_SERVICES_JSON — full JSON file contents (multiline).
set -euo pipefail

if [[ -z "${GOOGLE_SERVICES_JSON:-}" ]]; then
  echo "GOOGLE_SERVICES_JSON not set — using committed app/google-services.json"
  exit 0
fi

printf '%s\n' "${GOOGLE_SERVICES_JSON}" > app/google-services.json
echo "Wrote app/google-services.json from GOOGLE_SERVICES_JSON secret"
