# Verifies ic_stat_fash.png meets Android notification icon requirements.
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path $PSScriptRoot -Parent
$iconPath = Join-Path $repoRoot "app\src\main\res\drawable-nodpi\ic_stat_fash.png"
if (-not (Test-Path $iconPath)) {
    Write-Error "Missing ic_stat_fash.png - run tools/generate_launcher_adaptive.ps1"
}

$bmp = [System.Drawing.Bitmap]::FromFile($iconPath)
Write-Host "ic_stat_fash.png: $($bmp.Width)x$($bmp.Height)"

function Test-Corner($x, $y) {
    $c = $bmp.GetPixel($x, $y)
    Write-Host "Corner ${x},${y}: A=$($c.A) R=$($c.R) G=$($c.G) B=$($c.B)"
    if ($c.A -gt 32) {
        Write-Error "Corner must be transparent (notification icon background)."
    }
}

Test-Corner 0 0
Test-Corner ($bmp.Width - 1) 0
Test-Corner 0 ($bmp.Height - 1)
Test-Corner ($bmp.Width - 1) ($bmp.Height - 1)

$transparent = 0
$white = 0
$other = 0
for ($y = 0; $y -lt $bmp.Height; $y++) {
    for ($x = 0; $x -lt $bmp.Width; $x++) {
        $c = $bmp.GetPixel($x, $y)
        if ($c.A -lt 16) { $transparent++ }
        elseif ($c.R -ge 235 -and $c.G -ge 235 -and $c.B -ge 235) { $white++ }
        else { $other++ }
    }
}
Write-Host "Pixels: transparent=$transparent white=$white other_opaque=$other"
$bmp.Dispose()

if ($other -gt 0) {
    Write-Error "ic_stat_fash must be white silhouette on transparent only (found $other non-white opaque pixels)."
}
if ($white -lt 50) {
    Write-Error "ic_stat_fash has too few white pixels ($white) - icon may be invisible in status bar."
}
Write-Host "OK - ic_stat_fash passes notification icon checks."
