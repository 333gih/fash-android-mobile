# Release signing (why APK was `*-unsigned`)

Gradle only produces a **signed** release APK when a **signing config** is applied.

- **Before:** release had no keystore → output was `app-*-release-unsigned.apk`.
- **Now:** if you do **not** configure a release keystore, release builds **reuse the debug keystore** so the APK is **signed** and installable (fine for internal testing; **not** for Play Store upload with your upload key).

## Option A — Use your upload keystore (Play Store / real releases)

1. Create or use a `.jks` / `.keystore` file (Android Studio: *Build → Generate Signed App Bundle or APK* can create one).

2. Add these to **`local.properties`** (already gitignored) at the project root, **paths relative to project root**:

```properties
FASH_RELEASE_STORE_FILE=path/to/your-upload-keystore.jks
FASH_RELEASE_STORE_PASSWORD=your_store_password
FASH_RELEASE_KEY_ALIAS=your_key_alias
FASH_RELEASE_KEY_PASSWORD=your_key_password
```

3. Rebuild:

```powershell
.\gradlew assembleProdRelease
```

Output: `app\build\outputs\apk\prod\release\app-prod-release.apk` (signed).

## Option B — No keystore (internal / CI quick install)

Leave the `FASH_*` entries unset. The project **signs release with the debug key** automatically — you get a **signed** APK, not `unsigned`, but it’s still the **debug certificate** (don’t ship to Play as production).

## Same keys in `gradle.properties`

You can set the same `FASH_RELEASE_*` keys in `gradle.properties` instead of `local.properties` (avoid committing secrets).
