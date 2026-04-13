$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $root 'frontend'

Write-Host '[phase4] typecheck and build'
Push-Location $frontendDir
try {
    npm run build | Out-Host
} finally {
    Pop-Location
}

Write-Host '[phase4] verify preview server'
$index = Invoke-WebRequest -Uri 'http://127.0.0.1:4173/' -UseBasicParsing
if ($index.StatusCode -ne 200) {
    throw "preview server returned status $($index.StatusCode)"
}

if ($index.Content -notmatch '<div id="app"></div>') {
    throw 'preview server returned unexpected html shell'
}

Write-Host '[phase4] verify backend assessment workflow'
powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'verify_assessment_phase3.ps1') | Out-Host

Write-Host '[phase4] preview ok, backend assessment workflow ok'
