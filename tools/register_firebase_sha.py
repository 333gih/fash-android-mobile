#!/usr/bin/env python3
"""
Register SHA-1 fingerprints on Firebase Android apps and refresh app/google-services.json.

Why: google-services.json only gets Android OAuth clients (client_type 1) after SHA-1
fingerprints are registered in Firebase project fash-3526e.

Usage:
  python tools/register_firebase_sha.py --play-app-signing-sha1 XX:XX:...
  python tools/register_firebase_sha.py --dry-run

Requires: secrets/play-service-account.json (same GCP project as Firebase).
"""
from __future__ import annotations

import argparse
import base64
import json
import re
import sys
import time
from pathlib import Path

import google.auth.transport.requests
import requests
from google.oauth2 import service_account

REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SA = REPO_ROOT / "secrets" / "play-service-account.json"
OUT_JSON = REPO_ROOT / "app" / "google-services.json"

PROJECT_ID = "fash-3526e"
PROJECT_NUMBER = "598587496348"

APPS = {
    "com.pc.fash_android_mobile": "1:598587496348:android:90fabc2f0e9907ec6b5a59",
    "com.pc.fash_android_mobile.dev": "1:598587496348:android:ea5c44b324163d9a6b5a59",
}

# From `gradlew signingReport` — re-run if keystores change.
DEBUG_SHA1 = "8A:2B:13:39:2E:6B:62:C2:C4:4F:D9:3B:00:0C:70:7F:EF:1B:3E:2B"
UPLOAD_SHA1 = "5C:E7:3A:D0:24:DA:46:3B:63:9A:23:90:6E:A5:1A:A5:D9:50:20:09"

SHA1_RE = re.compile(r"^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){19}$")


def norm_sha1(value: str) -> str:
    value = value.strip().upper()
    if not SHA1_RE.match(value):
        raise ValueError(f"Invalid SHA-1 format: {value!r} (expected 20 hex pairs, colon-separated)")
    return value


def sha1_body(value: str) -> dict:
    return {"shaHash": norm_sha1(value).replace(":", "").lower(), "certType": "SHA_1"}


def auth_headers(sa_path: Path) -> dict[str, str]:
    creds = service_account.Credentials.from_service_account_file(
        str(sa_path),
        scopes=[
            "https://www.googleapis.com/auth/cloud-platform",
            "https://www.googleapis.com/auth/firebase",
        ],
    )
    creds.refresh(google.auth.transport.requests.Request())
    return {
        "Authorization": f"Bearer {creds.token}",
        "Content-Type": "application/json",
    }


def list_sha(headers: dict[str, str], app_id: str) -> list[str]:
    url = f"https://firebase.googleapis.com/v1beta1/projects/{PROJECT_ID}/androidApps/{app_id}/sha"
    resp = requests.get(url, headers=headers, timeout=60)
    resp.raise_for_status()
    data = resp.json()
    certs = data.get("certificates") or []
    return [c.get("shaHash", "").lower() for c in certs]


def add_sha(headers: dict[str, str], app_id: str, sha1: str, dry_run: bool) -> None:
    body = sha1_body(sha1)
    if dry_run:
        print(f"  [dry-run] would add {norm_sha1(sha1)} -> {app_id}")
        return
    url = f"https://firebase.googleapis.com/v1beta1/projects/{PROJECT_ID}/androidApps/{app_id}/sha"
    resp = requests.post(url, headers=headers, json=body, timeout=60)
    if resp.status_code == 409:
        print(f"  already registered {norm_sha1(sha1)} on {app_id}")
        return
    if not resp.ok:
        raise RuntimeError(
            f"Failed to add SHA-1 {norm_sha1(sha1)} to {app_id}: {resp.status_code} {resp.text}\n"
            "If INVALID_ARGUMENT: this package+SHA-1 may already exist in another GCP/Firebase project."
        )
    print(f"  added {norm_sha1(sha1)} -> {app_id}")


def fetch_google_services_json(headers: dict[str, str]) -> dict:
    """Download the combined google-services.json Firebase generates after SHA registration."""
    # Any Android app config returns the full multi-client google-services.json payload.
    app_id = APPS["com.pc.fash_android_mobile"]
    url = f"https://firebase.googleapis.com/v1beta1/projects/{PROJECT_ID}/androidApps/{app_id}/config"
    resp = requests.get(url, headers=headers, timeout=60)
    resp.raise_for_status()
    data = resp.json()
    encoded = data.get("configFileContents")
    if not encoded:
        raise RuntimeError("Firebase config response missing configFileContents")
    raw = base64.b64decode(encoded).decode("utf-8")
    return json.loads(raw)


def build_google_services_json(headers: dict[str, str]) -> dict:
    return fetch_google_services_json(headers)


def has_android_oauth(data: dict) -> bool:
    required = set(APPS.keys())
    found: dict[str, bool] = {p: False for p in required}
    for client in data.get("client", []):
        pkg = client.get("client_info", {}).get("android_client_info", {}).get("package_name", "")
        if pkg not in required:
            continue
        for oauth in client.get("oauth_client", []):
            if oauth.get("client_type") == 1 and oauth.get("client_id"):
                found[pkg] = True
    return all(found.values())


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--service-account", type=Path, default=DEFAULT_SA)
    parser.add_argument(
        "--play-app-signing-sha1",
        help="Play Console -> Setup -> App signing -> App signing key SHA-1 (required for Play Store Google Sign-In)",
    )
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--out", type=Path, default=OUT_JSON)
    args = parser.parse_args()

    if not args.service_account.is_file():
        print(f"Missing service account: {args.service_account}", file=sys.stderr)
        return 1

    headers = auth_headers(args.service_account)

    print("Current Firebase SHA certificates:")
    for package_name, app_id in APPS.items():
        shas = list_sha(headers, app_id)
        print(f"  {package_name}: {len(shas)} cert(s)")
        for s in shas:
            print(f"    - {s}")

    print("\nRegistering SHA-1 fingerprints...")
    add_sha(headers, APPS["com.pc.fash_android_mobile.dev"], DEBUG_SHA1, args.dry_run)
    add_sha(headers, APPS["com.pc.fash_android_mobile"], UPLOAD_SHA1, args.dry_run)
    if args.play_app_signing_sha1:
        add_sha(
            headers,
            APPS["com.pc.fash_android_mobile"],
            args.play_app_signing_sha1,
            args.dry_run,
        )
    else:
        print(
            "\nWARNING: --play-app-signing-sha1 not set.\n"
            "  Google Sign-In on Play Store installs will still fail until you add Play App signing SHA-1.\n"
            "  Play Console -> Setup -> App signing -> App signing key certificate -> SHA-1"
        )

    if args.dry_run:
        return 0

    print("\nWaiting for Firebase to sync OAuth clients...")
    merged = None
    for attempt in range(1, 13):
        time.sleep(5)
        merged = build_google_services_json(headers)
        if has_android_oauth(merged):
            print(f"  Android OAuth clients ready after {attempt * 5}s")
            break
        print(f"  attempt {attempt}/12: client_type 1 not ready yet...")
    else:
        print(
            "ERROR: google-services.json still missing Android OAuth clients (client_type 1).\n"
            "  - Confirm SHA-1 was added in Firebase Console (project fash-3526e)\n"
            "  - Add Play App signing SHA-1 if shipping Play Store builds\n"
            "  - Re-download from Firebase Console -> Project settings -> google-services.json",
            file=sys.stderr,
        )
        return 2

    args.out.write_text(json.dumps(merged, indent=2) + "\n", encoding="utf-8")
    print(f"\nWrote {args.out}")
    print("Next:")
    print("  1. bash scripts/ci_verify_google_services_json.sh")
    print("  2. Set GOOGLE_SERVICES_JSON in secrets/android-release.env and run push_github_android_secrets.ps1")
    print("  3. Commit + push + new Play release")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
