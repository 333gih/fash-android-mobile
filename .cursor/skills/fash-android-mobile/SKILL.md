---
name: fash-android-mobile
description: >-
  Develop and release the Fash Android app (Compose). Use when editing
  fash-android-mobile, Play upload, versionCode bumps, or GitHub Actions
  Android Release on releases/* branches. After every user-requested product
  fix on this repo, commit + push and follow gh until Google Play upload succeeds.
---

# Fash Android Mobile (Play release workflow)

## Context

- **Repo:** `fash-android-mobile` — Kotlin, Jetpack Compose.
- **Backend:** `core-service` — REST under `api/v1/`.
- **Release branch:** `releases/1.0` → push triggers **Android Release** → AAB → Google Play track `FASH-production` (closed testing).
- **Working CI remote (preferred):** `github333` → `333gih/fash-android-mobile` (also push `origin` GitLab). Confirm with recent successful runs if remotes change.

## Mandatory release loop (every product change)

When the user asks to fix / ship Android (or “commit push Google Play”):

1. **Bump** `app/build.gradle.kts`:
   - `versionCode` += 1 (never reuse)
   - `versionName` as needed (e.g. `1.0.100`)
2. **Commit** with message style `fix(android): <summary> (versionName / versionCode)`.
3. **Push both:**
   ```bash
   git push origin releases/1.0
   git push github333 releases/1.0
   ```
4. **Follow gh until Play upload succeeds**:
   ```bash
   gh run list -R 333gih/fash-android-mobile --workflow "Android Release" --branch releases/1.0 --limit 3
   gh run watch <run-id> -R 333gih/fash-android-mobile --exit-status
   ```
5. If CI fails: read logs, fix, **bump versionCode again**, commit, push, watch again until Play upload step is green.
6. Report the Actions URL + `versionName`/`versionCode` to the user.

## Branch → Play track

| Push | Track |
|------|--------|
| `releases/**` / `release/**` | `FASH-production` (closed testing) |
| `main` / `master` / tag `android/v*` | `production` |

Manual: `gh workflow run android-release.yml -R <repo> --ref releases/1.0 -f upload_play=true -f play_track=FASH-production`

## Chat / Message CTA conventions

- Preview **Message** must open chat (`onStartChatFromListing`), not PDP.
- If inbox already has a thread for the listing (`ChatViewModel.conversationIdForListingId`), label = `notification_action_open_chat` (“Mở chat”) and navigate to that conversation id.
- PDP Message button uses the same rule (`hasExistingChat`).

## Key paths

| Area | Path |
|------|------|
| Version | `app/build.gradle.kts` (`versionCode` / `versionName`) |
| CI | `.github/workflows/android-release.yml` |
| Chat VM | `ui/chat/ChatViewModel.kt` |
| Preview sheet | `ui/explore/ExploreListingPreviewSheet.kt` |
| Home preview wiring | `ui/home/HomeFeedContent.kt` |
| PDP | `ui/listing/ProductDetailScreen.kt` |
| Host chat open | `MainActivity.kt` |

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Play upload auth / API | Check Play Console service account secrets |
| versionCode conflict | Bump `versionCode` higher than last uploaded |
| Compile errors | Fix from Actions log; re-push with new versionCode |
