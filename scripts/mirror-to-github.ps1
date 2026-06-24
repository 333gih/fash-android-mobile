# Mirror fash-android-mobile → GitHub (triggers Actions after push).
#
# Prerequisites:
#   gh auth login   # account with write access to fashandcurious14052026-dotcom
#
# Usage:
#   .\scripts\mirror-to-github.ps1
#   .\scripts\mirror-to-github.ps1 -Repo fashandcurious14052026-dotcom/fash-android-mobile -Branches develop,releases/1.0
#   .\scripts\mirror-to-github.ps1 -CreateRepo -Visibility public
#   .\scripts\mirror-to-github.ps1 -PushSecrets
param(
    [string]$Repo = "fashandcurious14052026-dotcom/fash-android-mobile",
    [string]$Branches = "develop,main,releases/1.0",
    [switch]$CreateRepo,
    [ValidateSet("public", "private")]
    [string]$Visibility = "public",
    [switch]$PushSecrets,
    [switch]$PushTags
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    Write-Error "Install GitHub CLI: https://cli.github.com/ then run: gh auth login"
}

$ghUser = (gh api user -q .login 2>$null)
if (-not $ghUser) {
    Write-Error "Not logged in. Run: gh auth login"
}
Write-Host "GitHub CLI user: $ghUser"

$repoExists = $true
gh repo view $Repo 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    $repoExists = $false
    if ($CreateRepo) {
        Write-Host "Creating GitHub repo $Repo ($Visibility)..."
        gh repo create $Repo --$Visibility --description "Fash Android — GitHub Actions CI (APK + Play Store)"
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Cannot create $Repo — login as org owner (fashandcurious14052026-dotcom), not $ghUser"
        }
        $repoExists = $true
    } else {
        Write-Error "Repo $Repo not found. Re-run with -CreateRepo or create it on github.com first."
    }
}

$remoteUrl = "https://github.com/$Repo.git"
$remotes = git remote
if ($remotes -notcontains "github") {
    git remote add github $remoteUrl
    Write-Host "Added remote github -> $remoteUrl"
} else {
    git remote set-url github $remoteUrl
}

gh auth setup-git 2>$null | Out-Null

$branchList = $Branches -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
foreach ($b in $branchList) {
    if (-not (git rev-parse --verify "refs/heads/$b" 2>$null)) {
        Write-Warning "Skip branch (not local): $b"
        continue
    }
    Write-Host "Pushing branch: $b"
    git push -u github "refs/heads/${b}:refs/heads/${b}"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "git push failed for $b — ensure gh auth login uses an account with write access to $Repo"
    }
}

if ($PushTags) {
    Write-Host "Pushing tags matching android/v*..."
    git tag -l "android/v*" | ForEach-Object {
        git push github "refs/tags/${_}:refs/tags/${_}"
    }
}

if ($PushSecrets) {
    $secretsScript = Join-Path $Root "scripts\push_github_android_secrets.ps1"
    if (-not (Test-Path $secretsScript)) {
        Write-Warning "push_github_android_secrets.ps1 not found — skip secrets"
    } elseif (-not (Test-Path "secrets\android-release.env")) {
        Write-Warning "secrets\android-release.env missing — skip secrets (copy from android-release.env.example)"
    } else {
        & $secretsScript -Repo $Repo
    }
}

Write-Host ""
Write-Host "Done. Check Actions:"
Write-Host "  gh run list -R $Repo --limit 5"
Write-Host ""
Write-Host "Manual release (Closed testing / Play):"
Write-Host "  gh workflow run android-release.yml -R $Repo --ref releases/1.0 -f upload_play=true -f play_track=FASH-production"
