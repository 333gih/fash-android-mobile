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

function Test-IsWhiteMarkPixel([System.Drawing.Color]$c) {
    # Strict threshold — ignore magenta anti-alias fringe so bounds match visible hanger only.
    return $c.A -gt 160 -and $c.R -ge 235 -and $c.G -ge 235 -and $c.B -ge 235
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

function New-WhiteSilhouetteBitmap {
    param(
        [System.Drawing.Bitmap]$source,
        [System.Drawing.Rectangle]$bounds
    )
    $cropW = $bounds.Width
    $cropH = $bounds.Height
    $silhouette = New-Object System.Drawing.Bitmap($cropW, $cropH)
    for ($y = 0; $y -lt $cropH; $y++) {
        for ($x = 0; $x -lt $cropW; $x++) {
            $c = $source.GetPixel($bounds.X + $x, $bounds.Y + $y)
            if (Test-IsWhiteMarkPixel $c) {
                $silhouette.SetPixel($x, $y, [System.Drawing.Color]::White)
            } else {
                $silhouette.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
            }
        }
    }
    return $silhouette
}

function New-TransparentMarkCanvas {
    param(
        [System.Drawing.Bitmap]$silhouette,
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
    $aspect = $silhouette.Width / [double]$silhouette.Height
    $drawW = $target
    $drawH = [int][Math]::Round($target / $aspect)
    if ($drawH -gt $target) {
        $drawH = $target
        $drawW = [int][Math]::Round($target * $aspect)
    }
    $x = [int](($size - $drawW) / 2)
    $y = [int](($size - $drawH) / 2)

    $g.DrawImage($silhouette, $x, $y, $drawW, $drawH)
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
        [System.Drawing.Bitmap]$silhouette,
        [string]$folder,
        [int]$px,
        [double]$markScale
    )
    $mark = New-TransparentMarkCanvas -silhouette $silhouette -size $px -markScale $markScale
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
Write-Host "Mark bounds: $($bounds.Width)x$($bounds.Height) on $($source.Width)x$($source.Height) ($([math]::Round($bounds.Width/$source.Width*100,1))% wide)"

$silhouette = New-WhiteSilhouetteBitmap -source $source -bounds $bounds

# PNG mark fills ~72% of asset; final on-screen size is capped by ic_launcher_foreground_image (48dp).
$foregroundMark = New-TransparentMarkCanvas -silhouette $silhouette -size 432 -markScale 0.72
Save-Png $foregroundMark (Join-Path $resNodpi "ic_launcher_foreground_mark.png")
Write-Host "Wrote ic_launcher_foreground_mark.png (432, transparent)"

$brandComposite = New-CompositeBrand -markCanvas $foregroundMark
Save-Png $brandComposite (Join-Path $resNodpi "ic_launcher_brand.png")
Write-Host "Wrote ic_launcher_brand.png (432, composite)"

$statIcon = New-TransparentMarkCanvas -silhouette $silhouette -size 96 -markScale 0.62
Save-Png $statIcon (Join-Path $resNodpi "ic_stat_fash.png")
Write-Host "Wrote ic_stat_fash.png (96, white silhouette for status bar)"

$monochrome = New-TransparentMarkCanvas -silhouette $silhouette -size 432 -markScale 0.72
Save-Png $monochrome (Join-Path $resNodpi "ic_launcher_monochrome.png")
Write-Host "Wrote ic_launcher_monochrome.png (432, themed icon)"

foreach ($entry in @(
        @{ folder = "mdpi"; px = 48; scale = 0.44 },
        @{ folder = "hdpi"; px = 72; scale = 0.44 },
        @{ folder = "xhdpi"; px = 96; scale = 0.44 },
        @{ folder = "xxhdpi"; px = 144; scale = 0.44 },
        @{ folder = "xxxhdpi"; px = 192; scale = 0.44 }
    )) {
    Save-LegacyMipmap -silhouette $silhouette -folder $entry.folder -px $entry.px -markScale $entry.scale
    Write-Host "Updated mipmap-$($entry.folder)"
}

$foregroundMark.Dispose()
$brandComposite.Dispose()
$statIcon.Dispose()
$monochrome.Dispose()
$silhouette.Dispose()
$source.Dispose()

Write-Host "Done - FASH launcher + notification icons regenerated"
