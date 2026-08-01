# Register Firebase SHA-1 fingerprints and refresh app/google-services.json.
# Usage:
#   .\tools\register_firebase_sha.ps1 -PlayAppSigningSha1 "AA:BB:..."
#   .\tools\register_firebase_sha.ps1 -DryRun
param(
    [string]$PlayAppSigningSha1 = "",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

$py = @(
    "py", "-3.10",
    "tools/register_firebase_sha.py"
)
if ($PlayAppSigningSha1) {
    $py += @("--play-app-signing-sha1", $PlayAppSigningSha1)
}
if ($DryRun) {
    $py += "--dry-run"
}

& @py
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not $DryRun) {
    Write-Host ""
    Write-Host "Verify:" -ForegroundColor Cyan
    bash scripts/ci_verify_google_services_json.sh
}
