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

# Minecraft/OpenAL spatializes mono effects. Stereo effects remain centered on
# the listener even when level.playSound is called with block/entity coordinates.
$relativePositionalSounds = @(
    # SCP Unity heavy/manual doors
    'src\main\resources\assets\scp_unity_extra_blocks\sounds\closing.ogg',
    'src\main\resources\assets\scp_unity_extra_blocks\sounds\open.ogg',
    'src\main\resources\assets\scp_unity_extra_blocks\sounds\default_open.ogg',
    'src\main\resources\assets\scp_unity_extra_blocks\sounds\default_close.ogg',
    'src\main\resources\assets\scp_unity_extra_blocks\sounds\bathroom_open.ogg',
    'src\main\resources\assets\scp_unity_extra_blocks\sounds\bathroom_close.ogg',
    'src\main\resources\assets\scp_unity_extra_blocks\sounds\office_open.ogg',
    'src\main\resources\assets\scp_unity_extra_blocks\sounds\office_close.ogg',

    # Tesla Gate and terminal machinery
    'src\main\resources\assets\scp_additions\sounds\teslaactivate.ogg',
    'src\main\resources\assets\scp_additions\sounds\overcharge.ogg',
    'src\main\resources\assets\scp_additions\sounds\teslaready.ogg',
    'src\main\resources\assets\scp_additions\sounds\teslarecharge.ogg',
    'src\main\resources\assets\scp_additions\sounds\turningon.ogg',
    'src\main\resources\assets\scp_additions\sounds\turningoff.ogg',
    'src\main\resources\assets\scp_additions\sounds\overrideon.ogg',
    'src\main\resources\assets\scp_additions\sounds\terminalloop.ogg',
    'src\main\resources\assets\scp_additions\sounds\terminalon.ogg',
    'src\main\resources\assets\scp_additions\sounds\terminaloff.ogg',

    # Additions doors and containment machinery
    'src\main\resources\assets\scp_additions\sounds\dooropen.ogg',
    'src\main\resources\assets\scp_additions\sounds\doorclose.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp902opening.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp902closing.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp059box.ogg',
    'src\main\resources\assets\scp_additions\sounds\decontamination.ogg',
    'src\main\resources\assets\scp_additions\sounds\spray.ogg',
    'src\main\resources\assets\scp_additions\sounds\button.ogg',

    # SCP-914 machinery
    'src\main\resources\assets\scp_additions\sounds\scp914doorclose.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp914dooropen.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp914key.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp914refining.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp914dial.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp914inside.ogg',

    # SCP-294 machine sounds
    'src\main\resources\assets\scp_additions\sounds\scp294enter.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp294pouring.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp294emptycup.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp294outofrange.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp294on.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp294off.ogg',
    'src\main\resources\assets\scp_additions\sounds\scp294coinslot.ogg'
)

$files = New-Object System.Collections.Generic.List[System.IO.FileInfo]
foreach ($relativePath in $relativePositionalSounds) {
    $fullPath = Join-Path $repoRoot $relativePath
    if (Test-Path -LiteralPath $fullPath -PathType Leaf) {
        $files.Add((Get-Item -LiteralPath $fullPath))
    }
}

$stoneScrapeDir = Join-Path $repoRoot 'src\main\resources\assets\scpinventory\sounds'
if (Test-Path -LiteralPath $stoneScrapeDir -PathType Container) {
    Get-ChildItem -LiteralPath $stoneScrapeDir -Filter 'stone_scrap_*.ogg' -File |
        Sort-Object Name |
        ForEach-Object { $files.Add($_) }
}

$files = $files | Sort-Object FullName -Unique
if ($files.Count -eq 0) {
    throw 'No positional OGG files were found. Verify that the repository assets are present.'
}

function Convert-ToPositionalMono {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.FileInfo]$File
    )

    $channels = (& $ffprobeExe -v error -select_streams a:0 -show_entries stream=channels -of 'csv=p=0' -- $File.FullName).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "ffprobe failed for $($File.FullName)"
    }
    if ($channels -eq '1') {
        Write-Host "Already mono: $($File.Name)"
        return $false
    }

    $temporary = Join-Path $File.DirectoryName ($File.BaseName + '.mono.ogg')
    try {
        & $ffmpegExe -hide_banner -loglevel error -y -i $File.FullName -ac 1 -c:a libvorbis -q:a 6 $temporary
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $temporary)) {
            throw "ffmpeg failed for $($File.FullName)"
        }

        $resultChannels = (& $ffprobeExe -v error -select_streams a:0 -show_entries stream=channels -of 'csv=p=0' -- $temporary).Trim()
        if ($LASTEXITCODE -ne 0 -or $resultChannels -ne '1') {
            throw "Converted file is not mono: $($File.FullName)"
        }

        Move-Item -Force -LiteralPath $temporary -Destination $File.FullName
        Write-Host "Converted to positional mono: $($File.FullName)"
        return $true
    }
    finally {
        Remove-Item -LiteralPath $temporary -Force -ErrorAction SilentlyContinue
    }
}

$converted = 0
foreach ($file in $files) {
    if (Convert-ToPositionalMono -File $file) {
        $converted++
    }
}

Write-Host ''
Write-Host "Positional files checked: $($files.Count)"
Write-Host "Files converted this run: $converted"
Write-Host 'Run git status --short, then commit every modified OGG file.'
