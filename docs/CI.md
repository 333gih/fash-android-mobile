# CI: GitLab → GitHub → Build & Play Store (Android)

> **Trước khi push release:** tăng `versionCode` / `versionName` trong `app/build.gradle.kts` (Play từ chối trùng `versionCode`).

Luồng khuyến nghị (giống iOS):

```text
Windows / bất kỳ máy nào
        │
        ▼ git push
   GitLab (origin) — fash-android-mobile
        │
        ▼ mirror (cấu hình trên GitLab)
   GitHub
        │
        ▼ GitHub Actions (ubuntu-latest + JDK 17)
   APK (build) hoặc AAB + Google Play (release)
```

## 1. GitLab mirror → GitHub

Trên GitLab (**Settings → Repository → Mirroring repositories**):

| Field | Giá trị |
|---|---|
| Git repository URL | `https://github.com/<user>/fash-android-mobile.git` |
| Mirror direction | Push |
| Authentication | GitHub PAT (`repo`) hoặc deploy key |

Branch mirror: `main`, `master`, `develop`, `release/**`, `releases/**`.

File workflow nằm tại `.github/workflows/` — chỉ chạy sau khi repo GitHub đã có các file này (push/mirror).

## 2. Bật GitHub Actions

1. **Settings → Actions → General** → Allow actions.
2. Thêm **Secrets** (mục 4).
3. Tab **Actions** → **Android Build** / **Android Release**.

### Android Build (`android-build.yml`)

| Branch / PR target | Flavor | Gradle task |
|---|---|---|
| `develop` | dev | `assembleDevRelease` |
| `main` / `master` | prod | `assembleProdRelease` |
| PR vào `release/**` | prod | `assembleProdRelease` |

Artifact: APK signed (upload key nếu có secrets, không thì debug key).

### Android Release (`android-release.yml`)

| Trigger | Play upload |
|---|---|
| Push `release/**` / `releases/**` | Có — AAB → Play **Closed testing** (API track `alpha`) |
| Push `main` / `master` / tag `android/v*` | Chỉ AAB artifact |
| Run workflow thủ công | Chọn upload + track |

Output: `app-prod-release.aab` + `mapping.txt` (R8).

## 3. Secrets trên GitHub

### Đẩy secrets bằng script (giống iOS)

1. Copy `secrets/android-release.env.example` → `secrets/android-release.env`.
2. Điền đường dẫn `env/dev.env`, `env/prod.env`, upload keystore, Play JSON key.
3. Cài [GitHub CLI](https://cli.github.com/) → `gh auth login`.
4. Chạy từ thư mục repo:

```powershell
.\scripts\push_github_android_secrets.ps1
# hoặc repo khác:
.\scripts\push_github_android_secrets.ps1 -Repo phuckhoa33/fash-android-mobile
```

```bash
./scripts/push_github_android_secrets.sh
./scripts/push_github_android_secrets.sh secrets/android-release.env owner/fash-android-mobile
```

5. Kiểm tra: `gh secret list`.

### Env (bắt buộc cho build)

File `env/*.env` **không** commit (`.gitignore`). Copy nội dung file local vào secret **multiline**:

| Secret | Nội dung |
|---|---|
| `ANDROID_DEV_ENV` | Toàn bộ `env/dev.env` |
| `ANDROID_PROD_ENV` | Toàn bộ `env/prod.env` |

### Upload keystore (bắt buộc cho Play)

Tạo keystore upload (Play Console hoặc Android Studio). Encode:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("path\to\upload.keystore")) | Set-Clipboard
```

| Secret | Mô tả |
|---|---|
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | File `.jks` / `.keystore` dạng base64 |
| `ANDROID_UPLOAD_KEYSTORE_PASSWORD` | Store password |
| `ANDROID_UPLOAD_KEY_ALIAS` | Key alias (mặc định thường `upload`) |
| `ANDROID_UPLOAD_KEY_PASSWORD` | Key password |

CI ghi `local.properties` + `ci-upload.keystore` (gitignored) qua `scripts/ci_prepare_release_signing.sh`.

### Google Play API (upload tự động)

1. [Google Play Console](https://play.google.com/console) → **Setup → API access**.
2. Liên kết Google Cloud project → tạo **service account** với quyền release (thường *Release manager* hoặc tùy org).
3. Tải JSON key → secret:

| Secret | Mô tả |
|---|---|
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Toàn bộ file JSON (multiline) |

4. Lần đầu: upload AAB thủ công một bản để hoàn tất checklist app (content rating, privacy, v.v.) — API upload vẫn cần app đã được tạo trên Play.

**Package name (prod):** `com.pc.fash_android_mobile` (không có `.dev`).

## 4. So với iOS

| | iOS | Android |
|---|---|---|
| Runner | `macos-*` (đắt, chậm queue) | `ubuntu-latest` |
| Artifact store | TestFlight / IPA | Google Play / AAB |
| Signing | Cert + provisioning profile | Upload keystore |
| Env | Xcode schemes / project.yml | `env/dev.env`, `env/prod.env` |

## 5. Release branch workflow (gợi ý)

Giống iOS:

1. Bump `versionCode` + `versionName` trong `app/build.gradle.kts`.
2. Push nhánh `releases/x.y.z` (hoặc `release/x.y.z`).
3. Mirror sang GitHub → **Android Release** build `app-prod-release.aab` và upload **Closed testing** (API track `alpha`).
4. Play Console → **Testing → Closed testing** → xác nhận bản + testers; promote khi QA xong.

Tag tùy chọn: `android/v1.0.8` → build AAB, không auto-upload (trừ khi đổi workflow).

## 6. Local parity

```powershell
.\gradlew :app:bundleProdRelease
```

Xem thêm [SIGNING.md](../SIGNING.md).
