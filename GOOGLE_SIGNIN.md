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

1. Open [Google Play Console](https://play.google.com/console) → your app → **Setup → App signing**.
2. Copy **App signing key certificate** SHA-1 (and SHA-256 for App Links).
3. Open [Firebase Console](https://console.firebase.google.com/) → project **fash-3526e** → ⚙ **Project settings** → your Android app `com.pc.fash_android_mobile` → **Add fingerprint** → paste Play **App signing** SHA-1 (Firebase syncs OAuth clients in GCP).
4. Also add debug SHA-1 (`com.pc.fash_android_mobile.dev`) and upload-key SHA-1 for local/CI builds if missing.
5. Open [Google Cloud Console](https://console.cloud.google.com/) → project `fash-3526e` → **APIs & Services → Credentials** and confirm Android OAuth clients exist for:
   - Package: `com.pc.fash_android_mobile` + Play **App signing** SHA-1
   - Package: `com.pc.fash_android_mobile.dev` + debug SHA-1
6. Confirm **Web application** client id matches `GOOGLE_WEB_CLIENT_ID` in env files.
7. Confirm `fash-auth-service` `GOOGLE_OAUTH_CLIENT_IDS` includes the same Web client id.
8. Ship a new Play release (OAuth/Firebase changes propagate in minutes; users need the updated build only if app code changed).

Quick local helper:

```powershell
powershell -ExecutionPolicy Bypass -File tools/print_google_signin_fingerprints.ps1
```

## Auth-service

Server must accept ID tokens from the Web client id listed in `GOOGLE_OAUTH_CLIENT_IDS`.
