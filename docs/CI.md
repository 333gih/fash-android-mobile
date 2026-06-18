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

### Android Build (`android-build.yml`) — chỉ develop

| Trigger | Kết quả |
|---|---|
| Push / PR **`develop`** | APK **dev** (`assembleDevRelease`) |
| `main`, `releases/**` | **Không** chạy workflow này |

### Android Release (`android-release.yml`) — prod + Play

| Push branch | Play track | Mục đích |
|---|---|---|
| `releases/**`, `release/**` | `alpha` | **Closed testing** |
| `main`, `master` | `production` | **Production** trên Play |
| Tag `android/v*` | `production` | Release theo tag |
| Manual | Chọn track | Override |

Output: `app-prod-release.aab` + `mapping.txt` (R8).

> **Mirror GitLab → GitHub:** chỉ mirror/push đúng nhánh cần CI. Mirror đồng thời `develop` khi bạn push `releases/*` vẫn kích hoạt Android Build trên `develop` (nếu nhánh develop thay đổi trên remote).

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

4. **Bật Google Play Android Developer API** trên cùng GCP project đã liên kết Play Console (bắt buộc, nếu không CI fail ở bước Upload to Google Play):

   - [Enable API (project number)](https://console.developers.google.com/apis/api/androidpublisher.googleapis.com/overview?project=598587496348)
   - Hoặc GCP Console → project `fash-3526e` → **APIs & Services → Library** → tìm **Google Play Android Developer API** → **Enable**
   - Đợi 2–5 phút sau khi bật rồi chạy lại **Android Release**.

5. **Đổi upload key** (alias `upload`): Play Console → **App signing** → request upload key reset → upload file **`upload-cert.pem`** (export bên dưới). Không commit file `.pem`.

```powershell
keytool -export -rfc -alias upload -file upload-cert.pem -keystore secrets/upload.keystore -storepass "YOUR_STORE_PASSWORD"
keytool -list -v -keystore secrets/upload.keystore -alias upload -storepass "YOUR_STORE_PASSWORD"
# SHA-256 (đối chiếu trên Play): 49:7D:86:D6:8E:7B:B7:56:B2:CD:95:1E:51:16:C7:F7:13:4C:DA:92:68:46:AA:F3:67:B3:D1:2F:E1:97:56:5F
```

6. Play Console → **Users and permissions** → invite `play-publisher@fash-3526e.iam.gserviceaccount.com` (quyền release testing).

7. Lần đầu: upload AAB thủ công một bản để hoàn tất checklist app (content rating, privacy, v.v.) — API upload vẫn cần app đã được tạo trên Play.

**Package name (prod):** `com.pc.fash_android_mobile` (không có `.dev`).

### Troubleshooting CI

| Lỗi | Cách xử lý |
|---|---|
| `No key with alias 'upload' found` | Chạy `keytool -list -keystore secrets/upload.keystore` — chọn đúng alias, cập nhật `ANDROID_UPLOAD_KEY_ALIAS`, rồi `push_github_android_secrets.ps1`. |
| `AAB was signed with the wrong key` | SHA1 AAB không khớp upload key đã đăng ký trên Play. Dùng alias **`upload`** (82:9D:…) thì cần **đổi upload key** trên Play Console (Setup → App signing → Request upload key reset) hoặc dùng alias **`key0`** (5C:E7:…) nếu giữ cert cũ. |
| `Android Developer API has not been used... or it is disabled` | Bật **Google Play Android Developer API** (mục 4 ở trên). |
| `Unable to resolve action r0adkll/upload-google-play-action` | Dùng `r0adkll/upload-google-play@v1.1.3` (đã sửa trong workflow). |

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
3. Mirror sang GitHub → **Android Release** (không chạy Android Build trên develop).
4. Play Console → **Closed testing** → testers / QA.
5. Merge `main` → push `main` → **Android Release** upload track **production**.

Tag tùy chọn: `android/v1.0.8` → build AAB, không auto-upload (trừ khi đổi workflow).

## 6. Local parity

```powershell
.\gradlew :app:bundleProdRelease
```

Xem thêm [SIGNING.md](../SIGNING.md).
