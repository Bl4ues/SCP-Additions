[CmdletBinding()]
param(
    [string]$FfmpegRoot
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot

function Resolve-FfmpegTool {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Path
    }

    $candidateRoots = @(
        $FfmpegRoot,
        $env:FFMPEG_HOME,
        'C:\ffmpeg-8.0.1-essentials_build',
        'C:\ffmpeg'
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

    foreach ($root in $candidateRoots) {
        foreach ($relativePath in @("$Name.exe", "bin\$Name.exe")) {
            $candidate = Join-Path $root $relativePath
            if (Test-Path -LiteralPath $candidate -PathType Leaf) {
                return (Resolve-Path -LiteralPath $candidate).Path
            }
        }
    }

    throw @"
$Name was not found in PATH or in the known FFmpeg folders.
Run the script with the extracted FFmpeg folder explicitly:

    .\scripts\finalize-ui-and-scp173-audio.ps1 -FfmpegRoot "C:\ffmpeg-8.0.1-essentials_build"
"@
}

$ffmpegExe = Resolve-FfmpegTool -Name 'ffmpeg'
$ffprobeExe = Resolve-FfmpegTool -Name 'ffprobe'

Write-Host "Using ffmpeg:  $ffmpegExe"
Write-Host "Using ffprobe: $ffprobeExe"

# Minecraft/OpenAL can spatialize mono sounds. Stereo sound effects remain
# listener-centered even when level.playSound is given the entity coordinates.
$soundDir = Join-Path $repoRoot 'src\main\resources\assets\scpinventory\sounds'
$files = Get-ChildItem -Path $soundDir -Filter 'stone_scrap_*.ogg' -File | Sort-Object Name
if ($files.Count -eq 0) {
    throw "No stone_scrap_*.ogg files were found in $soundDir"
}

$converted = 0
foreach ($file in $files) {
    $channels = (& $ffprobeExe -v error -select_streams a:0 -show_entries stream=channels -of 'csv=p=0' -- $file.FullName).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "ffprobe failed for $($file.Name)"
    }
    if ($channels -eq '1') {
        Write-Host "Already mono: $($file.Name)"
        continue
    }

    $temporary = Join-Path $file.DirectoryName ($file.BaseName + '.mono.ogg')
    try {
        & $ffmpegExe -hide_banner -loglevel error -y -i $file.FullName -ac 1 -c:a libvorbis -q:a 6 $temporary
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $temporary)) {
            throw "ffmpeg failed for $($file.Name)"
        }

        $resultChannels = (& $ffprobeExe -v error -select_streams a:0 -show_entries stream=channels -of 'csv=p=0' -- $temporary).Trim()
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

Write-Host ''
Write-Host "Converted scrape files: $converted"
Write-Host 'Run git status --short, then commit and push the five OGG changes.'
