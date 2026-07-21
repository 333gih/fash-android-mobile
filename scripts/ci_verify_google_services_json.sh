#!/usr/bin/env bash
# Fail release builds when google-services.json lacks Android OAuth clients (client_type 1).
# Firebase only adds those entries after SHA-1 fingerprints (incl. Play App signing key) are registered.
#
# Usage: bash scripts/ci_verify_google_services_json.sh [path/to/google-services.json]
set -euo pipefail

JSON="${1:-app/google-services.json}"
if [[ ! -f "${JSON}" ]]; then
  echo "::error::Missing ${JSON}"
  exit 1
fi

python3 - "${JSON}" <<'PY'
import json
import sys

path = sys.argv[1]
with open(path, encoding="utf-8") as f:
    data = json.load(f)

required_packages = {
    "com.pc.fash_android_mobile",
    "com.pc.fash_android_mobile.dev",
}

found_android: dict[str, list[str]] = {p: [] for p in required_packages}

for client in data.get("client", []):
    pkg = client.get("client_info", {}).get("android_client_info", {}).get("package_name", "")
    if pkg not in required_packages:
        continue
    for oauth in client.get("oauth_client", []):
        if oauth.get("client_type") == 1:
            cid = oauth.get("client_id", "")
            if cid:
                found_android[pkg].append(cid)

missing = [p for p, ids in found_android.items() if not ids]
if missing:
    print("::warning::google-services.json is missing Android OAuth clients (client_type 1) for:")
    for p in missing:
        print(f"  - {p}")
    print("")
    print("Google Sign-In on Play Store will fail with DEVELOPER_ERROR until fixed:")
    print("  1. Play Console → Setup → App signing → copy App signing key SHA-1")
    print("  2. Firebase fash-3526e → Project settings → Android app → Add fingerprint")
    print("  3. Re-download google-services.json and commit or set GOOGLE_SERVICES_JSON secret")
    print("  See GOOGLE_SIGNIN.md")
    # Warning only until Play App signing SHA-1 is registered and GOOGLE_SERVICES_JSON secret is refreshed.
    sys.exit(0)

print("google-services.json: Android OAuth clients present for prod + dev packages")
for pkg, ids in found_android.items():
    print(f"  {pkg}: {len(ids)} android client(s)")
PY
