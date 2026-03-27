# Generates legacy mipmap/ic_launcher*.png from the same coral hanger silhouette (System.Drawing).
# Run from repo root: powershell -ExecutionPolicy Bypass -File tools/generate_launcher_mipmap.ps1
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$coral = [System.Drawing.Color]::FromArgb(255, 240, 93, 94) # #F05D5E
$white = [System.Drawing.Color]::FromArgb(255, 255, 255, 255)

function New-HangerIconBitmap {
    param([int]$size)
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.Clear($white)
    $brush = New-Object System.Drawing.SolidBrush($coral)
    $s = $size / 108.0

    # Hook (filled ellipse)
    $hookW = 14 * $s
    $hookH = 14 * $s
    $hookX = (54 * $s) - ($hookW / 2)
    $hookY = 12 * $s
    $g.FillEllipse($brush, $hookX, $hookY, $hookW, $hookH)

    # Outer hanger body (smooth closed curve through key points, 108-space)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $pts = @(
        [System.Drawing.PointF]::new(40 * $s, 34 * $s),
        [System.Drawing.PointF]::new(30 * $s, 52 * $s),
        [System.Drawing.PointF]::new(28 * $s, 72 * $s),
        [System.Drawing.PointF]::new(36 * $s, 92 * $s),
        [System.Drawing.PointF]::new(54 * $s, 82 * $s),
        [System.Drawing.PointF]::new(72 * $s, 92 * $s),
        [System.Drawing.PointF]::new(80 * $s, 72 * $s),
        [System.Drawing.PointF]::new(78 * $s, 52 * $s),
        [System.Drawing.PointF]::new(68 * $s, 34 * $s)
    )
    $path.AddClosedCurve($pts, 0.35)
    $g.FillPath($brush, $path)

    # Inner teardrop
    $tw = 12 * $s
    $th = 16 * $s
    $tx = (54 * $s) - ($tw / 2)
    $ty = 42 * $s
    $g.FillEllipse($brush, $tx, $ty, $tw, $th)

    $g.Dispose()
    return $bmp
}

function Save-Mipmap {
    param([string]$folder, [int]$px)
    $bmp = New-HangerIconBitmap -size $px
    $base = Join-Path $PSScriptRoot "..\app\src\main\res\mipmap-$folder"
    $bmp.Save((Join-Path $base "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save((Join-Path $base "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

Save-Mipmap "mdpi" 48
Save-Mipmap "hdpi" 72
Save-Mipmap "xhdpi" 96
Save-Mipmap "xxhdpi" 144
Save-Mipmap "xxxhdpi" 192
Write-Host "Wrote ic_launcher.png + ic_launcher_round.png for mdpi..xxxhdpi"
