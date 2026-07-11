[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$soundDir = Join-Path $repoRoot 'src\main\resources\assets\scpinventory\sounds'
$files = Get-ChildItem -Path $soundDir -Filter 'stone_scrap_*.ogg' -File | Sort-Object Name

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
Write-Host "Converted files: $converted"
Write-Host "All SCP-173 scrape assets are now mono and can be spatialized by Minecraft/OpenAL."
