# Regenerates adaptive foreground (white mark on transparent), notification stat icon,
# legacy composite brand PNG, and legacy mipmaps with launcher safe-zone padding.
# Run: powershell -ExecutionPolicy Bypass -File tools/generate_launcher_adaptive.ps1
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path $PSScriptRoot -Parent
$iosIconPath = Join-Path $repoRoot "..\fash-ios-mobile\Fash\Assets.xcassets\AppIcon.appiconset\AppIcon-1024.png"
if (-not (Test-Path $iosIconPath)) {
    $iosIconPath = Join-Path $repoRoot "app\src\main\res\drawable-nodpi\ic_launcher_brand.png"
}
$resNodpi = Join-Path $repoRoot "app\src\main\res\drawable-nodpi"
$magenta = [System.Drawing.Color]::FromArgb(255, 250, 51, 92) # #FA335C
$white = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)

function Test-IsWhiteMarkPixel([System.Drawing.Color]$c) {
    return $c.A -gt 128 -and $c.R -gt 185 -and $c.G -gt 185 -and $c.B -gt 185
}

function Get-MarkBounds([System.Drawing.Bitmap]$bmp) {
    $minX = $bmp.Width; $minY = $bmp.Height; $maxX = 0; $maxY = 0
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            if (Test-IsWhiteMarkPixel $bmp.GetPixel($x, $y)) {
                if ($x -lt $minX) { $minX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    if ($maxX -lt $minX) { throw "No white hanger mark found in source icon." }
    return [PSCustomObject]@{
        X = $minX
        Y = $minY
        Width = $maxX - $minX + 1
        Height = $maxY - $minY + 1
    }
}

function New-TransparentMarkCanvas {
    param(
        [System.Drawing.Bitmap]$source,
        [System.Drawing.Rectangle]$bounds,
        [int]$size,
        [double]$markScale
    )
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $target = [int][Math]::Round($size * $markScale)
    $aspect = $bounds.Width / [double]$bounds.Height
    $drawW = $target
    $drawH = [int][Math]::Round($target / $aspect)
    if ($drawH -gt $target) {
        $drawH = $target
        $drawW = [int][Math]::Round($target * $aspect)
    }
    $x = [int](($size - $drawW) / 2)
    $y = [int](($size - $drawH) / 2)

    $mark = $source.Clone($bounds, $source.PixelFormat)
    $g.DrawImage($mark, $x, $y, $drawW, $drawH)
    $mark.Dispose()
    $g.Dispose()
    return $bmp
}

function New-CompositeBrand {
    param(
        [System.Drawing.Bitmap]$markCanvas
    )
    $size = $markCanvas.Width
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear($magenta)
    $g.DrawImage($markCanvas, 0, 0, $size, $size)
    $g.Dispose()
    return $bmp
}

function Save-Png([System.Drawing.Bitmap]$bmp, [string]$path) {
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
}

function Save-LegacyMipmap {
    param(
        [System.Drawing.Bitmap]$source,
        [System.Drawing.Rectangle]$bounds,
        [string]$folder,
        [int]$px,
        [double]$markScale
    )
    $mark = New-TransparentMarkCanvas -source $source -bounds $bounds -size $px -markScale $markScale
    $composite = New-CompositeBrand -markCanvas $mark
    $dir = Join-Path $repoRoot "app\src\main\res\mipmap-$folder"
    Save-Png $composite (Join-Path $dir "ic_launcher.png")
    Save-Png $composite (Join-Path $dir "ic_launcher_round.png")
    $mark.Dispose()
    $composite.Dispose()
}

Write-Host "Source icon: $iosIconPath"
$source = [System.Drawing.Bitmap]::FromFile($iosIconPath)
$bounds = Get-MarkBounds $source
Write-Host "Mark bounds: $($bounds.Width)x$($bounds.Height) on $($source.Width)x$($source.Height)"

# Adaptive foreground: white mark only, ~48% of canvas (inside 66% safe zone on OEM masks).
$foregroundMark = New-TransparentMarkCanvas -source $source -bounds $bounds -size 432 -markScale 0.48
Save-Png $foregroundMark (Join-Path $resNodpi "ic_launcher_foreground_mark.png")
Write-Host "Wrote ic_launcher_foreground_mark.png (432, transparent, scale 0.48)"

# Legacy composite + notification stat share the same mark proportions.
$brandComposite = New-CompositeBrand -markCanvas $foregroundMark
Save-Png $brandComposite (Join-Path $resNodpi "ic_launcher_brand.png")
Write-Host "Wrote ic_launcher_brand.png (432, composite)"

$statIcon = New-TransparentMarkCanvas -source $source -bounds $bounds -size 96 -markScale 0.62
Save-Png $statIcon (Join-Path $resNodpi "ic_stat_fash.png")
Write-Host "Wrote ic_stat_fash.png (96, notification status bar)"

$monochrome = New-TransparentMarkCanvas -source $source -bounds $bounds -size 432 -markScale 0.48
Save-Png $monochrome (Join-Path $resNodpi "ic_launcher_monochrome.png")
Write-Host "Wrote ic_launcher_monochrome.png (432, themed icon)"

foreach ($entry in @(
        @{ folder = "mdpi"; px = 48; scale = 0.54 },
        @{ folder = "hdpi"; px = 72; scale = 0.54 },
        @{ folder = "xhdpi"; px = 96; scale = 0.54 },
        @{ folder = "xxhdpi"; px = 144; scale = 0.54 },
        @{ folder = "xxxhdpi"; px = 192; scale = 0.54 }
    )) {
    Save-LegacyMipmap -source $source -bounds $bounds -folder $entry.folder -px $entry.px -markScale $entry.scale
    Write-Host "Updated mipmap-$($entry.folder)"
}

$foregroundMark.Dispose()
$brandComposite.Dispose()
$statIcon.Dispose()
$monochrome.Dispose()
$source.Dispose()

Write-Host "Done - FASH launcher + notification icons regenerated"
