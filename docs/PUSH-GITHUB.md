# Push Android app lên GitHub (Actions CI)

GitLab `origin` là remote chính; GitHub `github` dùng cho **GitHub Actions** (build APK / AAB / Play).

## Trạng thái hiện tại

| Item | Giá trị |
|------|---------|
| GitHub org | `fashandcurious14052026-dotcom` |
| Repo CI | `fashandcurious14052026-dotcom/fash-android-mobile` |
| Remote local | `git remote -v` → `github` |
| Workflows | `.github/workflows/android-build.yml`, `android-release.yml` |

> Repo Android trên GitHub **cần được tạo một lần** (iOS đã có; Android có thể chưa tồn tại).

## Bước 1 — Đăng nhập đúng tài khoản GitHub

Máy hiện tại phải login **tài khoản có quyền write** vào org (không phải tài khoản cá nhân khác):

```powershell
gh auth login
# Chọn: GitHub.com → HTTPS → Login with browser
# Tài khoản: fashandcurious14052026-dotcom (hoặc user được invite admin repo)
gh auth status
```

Nếu thấy `Permission denied to 333gih` → đăng xuất và login lại:

```powershell
gh auth logout
gh auth login
```

## Bước 2 — Tạo repo + push code

Từ thư mục `fash-android-mobile`:

```powershell
# Tạo repo (lần đầu) + push develop, main, releases/1.0
.\scripts\mirror-to-github.ps1 -CreateRepo -Visibility public

# Lần sau (repo đã có):
.\scripts\mirror-to-github.ps1
```

Hoặc thủ công:

```powershell
gh repo create fashandcurious14052026-dotcom/fash-android-mobile --public
git push -u github develop
git push -u github main
git push -u github releases/1.0
```

## Bước 3 — Secrets (bắt buộc cho release / Play)

```powershell
cp secrets/android-release.env.example secrets/android-release.env
# Điền keystore, env paths, play-service-account.json

.\scripts\push_github_android_secrets.ps1 -Repo fashandcurious14052026-dotcom/fash-android-mobile
# Hoặc mirror kèm secrets:
.\scripts\mirror-to-github.ps1 -PushSecrets
```

Kiểm tra:

```powershell
gh secret list -R fashandcurious14052026-dotcom/fash-android-mobile
```

## Bước 4 — Chạy CI

| Mục đích | Cách |
|----------|------|
| Dev APK | Push `develop` → **Android Build** |
| AAB + Play Closed testing | Push `releases/x.y.z` → **Android Release** (track `alpha`) |
| Thủ công | `gh workflow run android-release.yml -R fashandcurious14052026-dotcom/fash-android-mobile --ref releases/1.0 -f upload_play=true -f play_track=alpha` |

Theo dõi:

```powershell
gh run list -R fashandcurious14052026-dotcom/fash-android-mobile --limit 5
gh run watch -R fashandcurious14052026-dotcom/fash-android-mobile
```

## GitLab mirror (tùy chọn)

Thay vì script, có thể bật **GitLab → GitHub mirror** (Settings → Repository → Mirroring):

- URL: `https://github.com/fashandcurious14052026-dotcom/fash-android-mobile.git`
- Direction: Push
- PAT `repo` scope

Chi tiết signing / Play: [CI.md](./CI.md).
