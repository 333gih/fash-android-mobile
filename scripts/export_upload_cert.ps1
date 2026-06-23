# Export upload certificate for Google Play Console (upload key reset).
# Reads passwords/alias from secrets/android-release.env - do not commit output.
#
# Usage:
#   .\scripts\export_upload_cert.ps1
#   .\scripts\export_upload_cert.ps1 -OutFile upload-cert.pem
param(
    [string]$EnvFile = "secrets/android-release.env",
    [string]$OutFile = "upload-cert.pem"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

function Read-DotEnv([string]$Path) {
    $map = @{}
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $eq = $line.IndexOf("=")
        if ($eq -lt 1) { return }
        $key = $line.Substring(0, $eq).Trim()
        $val = $line.Substring($eq + 1).Trim()
        $map[$key] = $val
    }
    return $map
}

if (-not (Test-Path $EnvFile)) {
    Write-Error "Missing $EnvFile"
}

$envMap = Read-DotEnv $EnvFile
$keystore = Join-Path $Root ($envMap["ANDROID_UPLOAD_KEYSTORE_PATH"] -replace "/", "\")
$alias = $envMap["ANDROID_UPLOAD_KEY_ALIAS"]
$storePass = $envMap["ANDROID_UPLOAD_KEYSTORE_PASSWORD"]

if (-not (Test-Path $keystore)) {
    Write-Error "Keystore not found: $keystore"
}

$keytool = (Get-Command keytool -ErrorAction SilentlyContinue).Source
if (-not $keytool) {
    $keytool = Join-Path $env:JAVA_HOME "bin\keytool.exe"
}
if (-not (Test-Path $keytool)) {
    Write-Error "keytool not found - install JDK 17+"
}

& $keytool -export -rfc -alias $alias -file $OutFile -keystore $keystore -storepass $storePass
Write-Host "Exported $OutFile (alias=$alias)"
Write-Host ""
Write-Host "Next: Play Console -> Setup -> App signing -> Request upload key reset -> upload $OutFile"
Write-Host "After Google approves, set PLAY_EXPECTED_UPLOAD_SHA1 in secrets/android-release.env and re-run push_github_android_secrets.ps1"
