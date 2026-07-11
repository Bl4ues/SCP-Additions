[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

# Validate binary conversion tools before touching tracked files.
if (-not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    throw @'
ffmpeg was not found in PATH.
Install it with:

    winget install --id Gyan.FFmpeg -e

Open a new PowerShell window afterwards and run this script again.
'@
}
if (-not (Get-Command ffprobe -ErrorAction SilentlyContinue)) {
    throw 'ffprobe was not found in PATH. It is installed together with ffmpeg.'
}

# Apply the narrow Roboto exception to the Tesla Terminal permission label.
$terminalPath = Join-Path $repoRoot 'src\main\java\net\mcreator\scpadditions\client\gui\TeslaTerminalScreen.java'
$terminalText = Get-Content $terminalPath -Raw -Encoding UTF8
$lineEnding = if ($terminalText.Contains("`r`n")) { "`r`n" } else { "`n" }
$importAnchor = "import net.mcreator.scpadditions.ScpAdditionsMod;$lineEnding"
$fontImport = "import net.mcreator.scpadditions.client.ScpFonts;$lineEnding"

if (-not $terminalText.Contains($fontImport)) {
    if (-not $terminalText.Contains($importAnchor)) {
        throw 'TeslaTerminalScreen import anchor was not found.'
    }
    $terminalText = $terminalText.Replace($importAnchor, $importAnchor + $fontImport)
}

$oldDraw = 'guiGraphics.drawString(this.font, Component.literal(text), 0, 0, color, false);'
$newDraw = 'guiGraphics.drawString(this.font, Component.literal(text).withStyle(style -> style.withFont(ScpFonts.ROBOTO)), 0, 0, color, false);'
if ($terminalText.Contains($oldDraw)) {
    $terminalText = $terminalText.Replace($oldDraw, $newDraw)
} elseif (-not $terminalText.Contains($newDraw)) {
    throw 'Tesla Terminal permission draw call was not found.'
}

$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($terminalPath, $terminalText, $utf8WithoutBom)
Write-Host 'Applied Roboto only to Tesla Terminal GRANTED/DENIED.'

# Convert stereo SCP-173 scrape assets to mono so OpenAL can spatialize them.
$soundDir = Join-Path $repoRoot 'src\main\resources\assets\scpinventory\sounds'
$files = Get-ChildItem -Path $soundDir -Filter 'stone_scrap_*.ogg' -File | Sort-Object Name
if ($files.Count -eq 0) {
    throw "No stone_scrap_*.ogg files were found in $soundDir"
}

$converted = 0
foreach ($file in $files) {
    $channels = (& ffprobe -v error -select_streams a:0 -show_entries stream=channels -of 'csv=p=0' -- $file.FullName).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "ffprobe failed for $($file.Name)"
    }
    if ($channels -eq '1') {
        Write-Host "Already mono: $($file.Name)"
        continue
    }

    $temporary = Join-Path $file.DirectoryName ($file.BaseName + '.mono.ogg')
    try {
        & ffmpeg -hide_banner -loglevel error -y -i $file.FullName -ac 1 -c:a libvorbis -q:a 6 $temporary
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $temporary)) {
            throw "ffmpeg failed for $($file.Name)"
        }
        $resultChannels = (& ffprobe -v error -select_streams a:0 -show_entries stream=channels -of 'csv=p=0' -- $temporary).Trim()
        if ($LASTEXITCODE -ne 0 -or $resultChannels -ne '1') {
            throw "Converted file is not mono: $($file.Name)"
        }
        Move-Item -Force $temporary $file.FullName
        $converted++
        Write-Host "Converted to positional mono: $($file.Name)"
    }
    finally {
        Remove-Item $temporary -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "Converted scrape files: $converted"
Write-Host 'Run git status --short, then commit the Java and OGG changes.'
