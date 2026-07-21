# Google Sign-In (Android)

## Build variants vs Play Store

| How you run / ship | Gradle variant | Package name | Signing cert (local) |
|--------------------|----------------|--------------|----------------------|
| Android Studio **Run** | `devDebug` | `com.pc.fash_android_mobile.dev` | Debug keystore |
| `./gradlew installDevDebug` | `devDebug` | `com.pc.fash_android_mobile.dev` | Debug keystore |
| `./gradlew assembleProdRelease` | `prodRelease` | `com.pc.fash_android_mobile` | Upload/release keystore (see `SIGNING.md`) |
| **Google Play download** | `prodRelease` | `com.pc.fash_android_mobile` | **Play App Signing certificate** (not your upload key) |

`GOOGLE_WEB_CLIENT_ID` in `env/dev.env` and `env/prod.env` is injected at **compile time** into `BuildConfig.GOOGLE_WEB_CLIENT_ID`. It must be the **Web application** OAuth client id from Google Cloud Console (same project as Android clients).

## Why simulator works but Play Store shows DEVELOPER_ERROR

Google Sign-In validates **package name + SHA-1** of the certificate that actually signed the installed APK.

- **devDebug** uses `com.pc.fash_android_mobile.dev` + debug SHA-1 → works if that Android OAuth client exists in GCP.
- **Play Store** uses `com.pc.fash_android_mobile` + **Play App Signing SHA-1** → fails with `DEVELOPER_ERROR` if that SHA-1 is missing from the prod Android OAuth client.

Run locally to print keystore fingerprints:

```powershell
.\gradlew signingReport
```

Reference fingerprints from this project (re-run `signingReport` if keystores change):

| Keystore | SHA-1 |
|----------|-------|
| Debug (Studio Run) | `8A:2B:13:39:2E:6B:62:C2:C4:4F:D9:3B:00:0C:70:7F:EF:1B:3E:2B` |
| Upload / release (local `prodRelease`) | `5C:E7:3A:D0:24:DA:46:3B:63:9A:23:90:6E:A5:1A:A5:D9:50:20:09` |

## Fix Play Store DEVELOPER_ERROR

**Root cause:** Apps installed from Google Play are signed with Google's **App signing key**, not your upload/debug keystore. Google Sign-In validates `package + SHA-1`. iOS does not use this Android certificate check — that is why iOS can work while Play builds fail.

**Current repo status:** `app/google-services.json` only lists Web OAuth clients (`client_type: 3`), not Android clients (`client_type: 1`). That means Firebase has **not** synced Android OAuth clients yet — add all SHA-1 fingerprints below, then re-download the JSON.

1. Open [Google Play Console](https://play.google.com/console) → your app → **Setup → App signing**.
2. Copy **App signing key certificate** SHA-1 (and SHA-256 for App Links).
3. Open [Firebase Console](https://console.firebase.google.com/) → project **fash-3526e** → ⚙ **Project settings** → your Android app `com.pc.fash_android_mobile` → **Add fingerprint** → paste Play **App signing** SHA-1 (Firebase syncs OAuth clients in GCP).
4. Also add debug SHA-1 (`com.pc.fash_android_mobile.dev`) and upload-key SHA-1 for local/CI builds if missing.
5. **Re-download** `google-services.json` from Firebase (must include `oauth_client` entries with `client_type: 1` for each package).
6. Commit the updated JSON **or** set GitHub secret `GOOGLE_SERVICES_JSON` (full file) for CI inject.
7. Open [Google Cloud Console](https://console.cloud.google.com/) → project `fash-3526e` → **APIs & Services → Credentials** and confirm Android OAuth clients exist for:
   - Package: `com.pc.fash_android_mobile` + Play **App signing** SHA-1
   - Package: `com.pc.fash_android_mobile.dev` + debug SHA-1
6. Confirm **Web application** client id matches `GOOGLE_WEB_CLIENT_ID` in env files.
7. Confirm `fash-auth-service` `GOOGLE_OAUTH_CLIENT_IDS` includes the same Web client id.
8. Ship a new Play release (OAuth/Firebase changes propagate in minutes; users need the updated build only if app code changed).

Quick local helper:

```powershell
powershell -ExecutionPolicy Bypass -File tools/print_google_signin_fingerprints.ps1
```

## “Unauthorized” / “no auth” / MISSING_TOKEN after choosing Google account

Two different failure points:

| When it fails | Typical toast / log | Cause | Fix |
|---------------|---------------------|-------|-----|
| **Before** account picker | `DEVELOPER_ERROR` (code 10) | `google-services.json` has **no Android OAuth client** (`client_type: 1`) — current repo only has Web (`client_type: 3`) | Add **Play App signing SHA-1** in Firebase → re-download JSON (see above) |
| **After** Google returns | `Unauthorized`, `Thiếu Bearer token`, `MISSING_TOKEN` | App or gateway hit **api-core** instead of **api-auth**, or Kong does not treat `POST /vi/api/v1/auth/social-login` as public | Confirm `AUTH_SERVICE_BASE_URL=https://api-auth.fashandcurious.com/` in `env/prod.env` |
| **After** Google returns | `SOCIAL_AUTH_DISABLED` | `fash-auth-service` missing `GOOGLE_OAUTH_CLIENT_IDS` at runtime | Set env on deployed auth-service (must match `GOOGLE_WEB_CLIENT_ID`) |
| **After** Google returns | `SOCIAL_AUTH_FAILED` | Web client id mismatch or invalid id_token | Align `GOOGLE_WEB_CLIENT_ID` (app) = `GOOGLE_OAUTH_CLIENT_IDS` (auth-service) |

### Verify from your machine (no real Google token needed)

```powershell
# 1) Auth host must NOT return 401 MISSING_TOKEN (public route)
curl.exe -sS -X POST "https://api-auth.fashandcurious.com/vi/api/v1/auth/social-login" `
  -H "Content-Type: application/json" `
  -d "{\"provider\":\"google\",\"provider_token\":\"test\",\"application_id\":\"web\",\"client_channel\":\"fash_android_app\"}"
# Expect: 401 SOCIAL_AUTH_FAILED or 400 — NOT Kong MISSING_TOKEN

# 2) Wrong host (api-core) — should 401 if misconfigured
curl.exe -sS -X POST "https://api-core.fashandcurious.com/vi/api/v1/auth/social-login" ...
# Expect: 401 Unauthorized — do NOT point the app here for login
```

### Verify on device (logcat)

Filter `GoogleSignIn` and `AuthRepository`:

```text
adb logcat -s GoogleSignIn AuthRepository
```

- `DEVELOPER_ERROR` → note **package + SHA-1** in toast/log → add SHA-1 in Firebase.
- `social-login failed url=... code=MISSING_TOKEN` → wrong host or Kong public-auth list.
- `code=SOCIAL_AUTH_DISABLED` → ops: set `GOOGLE_OAUTH_CLIENT_IDS` on auth-service pod.

## Auth-service

Server must accept ID tokens from the Web client id listed in `GOOGLE_OAUTH_CLIENT_IDS`.

## iOS vs Android credentials (cannot swap)

| Credential | Android | iOS |
|------------|---------|-----|
| `GOOGLE_WEB_CLIENT_ID` (Web OAuth) | ✅ `requestIdToken` + server verify | ✅ `serverClientID` + server verify |
| `GOOGLE_IOS_CLIENT_ID` | ❌ not used | ✅ native `clientID` in GIDSignIn |
| Android OAuth client (package + SHA-1) | ✅ required for Play / debug | ❌ not used |

Both apps already share the **same Web client id** in `env/prod.env`. iOS `GOOGLE_IOS_CLIENT_ID` cannot fix Android — add **Play App signing SHA-1** to Firebase/GCP instead.

## “Cannot reach Google server” on Android (network toast)

Older builds ran an OkHttp HEAD probe to `accounts.google.com` before opening the picker. Some mobile networks / Private DNS block that probe while Google Play services sign-in still works (iOS never had this probe). **1.0.23+** removes the probe — only checks Internet + Google Play services, then opens the account picker like iOS.
