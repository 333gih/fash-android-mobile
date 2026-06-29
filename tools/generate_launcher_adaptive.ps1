# Regenerates adaptive foreground + legacy mipmaps with launcher safe-zone padding.
# Run: powershell -ExecutionPolicy Bypass -File tools/generate_launcher_adaptive.ps1
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path $PSScriptRoot -Parent
$brandPath = Join-Path $repoRoot "app\src\main\res\drawable-nodpi\ic_launcher_brand.png"
$magenta = [System.Drawing.Color]::FromArgb(255, 250, 51, 92) # #FA335C

function New-AdaptiveForeground {
    param([int]$size = 432, [double]$scale = 0.58)
    $src = [System.Drawing.Image]::FromFile($brandPath)
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear($magenta)
    $inner = [int][Math]::Round($size * $scale)
    $x = [int](($size - $inner) / 2)
    $y = [int](($size - $inner) / 2)
    $g.DrawImage($src, $x, $y, $inner, $inner)
    $g.Dispose()
    $src.Dispose()
    return $bmp
}

function Save-LegacyMipmap {
    param([string]$folder, [int]$px)
    $bmp = New-AdaptiveForeground -size $px -scale 0.72
    $dir = Join-Path $repoRoot "app\src\main\res\mipmap-$folder"
    $bmp.Save((Join-Path $dir "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save((Join-Path $dir "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

$adaptive = New-AdaptiveForeground -size 432 -scale 0.58
$outBrand = Join-Path $repoRoot "app\src\main\res\drawable-nodpi\ic_launcher_brand.png"
$adaptive.Save($outBrand, [System.Drawing.Imaging.ImageFormat]::Png)
$adaptive.Dispose()
Write-Host "Updated ic_launcher_brand.png (432, safe zone)"

foreach ($entry in @(@{ folder = "mdpi"; px = 48 }, @{ folder = "hdpi"; px = 72 }, @{ folder = "xhdpi"; px = 96 }, @{ folder = "xxhdpi"; px = 144 }, @{ folder = "xxxhdpi"; px = 192 })) {
    Save-LegacyMipmap -folder $entry.folder -px $entry.px
    Write-Host "Updated mipmap-$($entry.folder)"
}

Write-Host "Done - FASH launcher icons regenerated"
