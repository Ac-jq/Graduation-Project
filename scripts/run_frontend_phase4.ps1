$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $root 'frontend'
$distDir = Join-Path $frontendDir 'dist'

Write-Host '[phase4] install frontend deps'
Push-Location $frontendDir
try {
    npm install | Out-Host
    Write-Host '[phase4] build frontend'
    npm run build | Out-Host
} finally {
    Pop-Location
}

if (-not (Test-Path $distDir)) {
    throw 'frontend dist directory not found'
}

Write-Host '[phase4] start preview server on http://127.0.0.1:4173'
$process = Start-Process -FilePath 'cmd.exe' `
    -ArgumentList '/c', 'npm run preview -- --host 127.0.0.1 --port 4173' `
    -WorkingDirectory $frontendDir `
    -PassThru

Start-Sleep -Seconds 5

Write-Host "[phase4] preview pid: $($process.Id)"
