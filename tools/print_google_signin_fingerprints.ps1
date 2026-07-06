# Prints SHA-1 fingerprints for Google Sign-In + checklist for Play Store builds.
# Run: powershell -ExecutionPolicy Bypass -File tools/print_google_signin_fingerprints.ps1
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path $PSScriptRoot -Parent
Set-Location $repoRoot

Write-Host "=== FASH Android — Google Sign-In fingerprints ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Running gradlew signingReport (debug + upload/release keystores)..."
.\gradlew.bat signingReport | Select-String -Pattern "Variant:|SHA1:" 

Write-Host ""
Write-Host "=== Required in Firebase / Google Cloud (project fash-3526e) ===" -ForegroundColor Yellow
Write-Host "1. Firebase Console -> Project settings -> Your apps -> Android (com.pc.fash_android_mobile)"
Write-Host "   Add fingerprint SHA-1 for EACH certificate below:"
Write-Host "   - Debug (Android Studio Run / devDebug)"
Write-Host "   - Upload key (local prodRelease / CI AAB)"
Write-Host "   - **App signing key** (Play Console -> Setup -> App signing)  <-- REQUIRED for Play Store installs"
Write-Host ""
Write-Host "2. Web OAuth client id (GOOGLE_WEB_CLIENT_ID):"
Write-Host "   598587496348-3m7us7ap54ia5clqvag38ctqbuqefasf.apps.googleusercontent.com"
Write-Host ""
Write-Host "3. fash-auth-service GOOGLE_OAUTH_CLIENT_IDS must include the same Web client id."
Write-Host ""
Write-Host "4. After adding Play App signing SHA-1, re-download google-services.json (optional) and ship a new Play build."
Write-Host ""
Write-Host "See GOOGLE_SIGNIN.md for full steps."
