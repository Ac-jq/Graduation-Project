param(
    [string]$JavaExe = "E:\environment\JDK21\jdk-21.0.2\bin\java.exe"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'target\JQPro-0.0.1-SNAPSHOT.jar'
$layersDir = Join-Path $projectRoot '.codex-tmp\layers2'
$targetClasses = Join-Path $projectRoot 'target\classes'

if (-not (Test-Path $JavaExe)) {
    throw "Java executable not found: $JavaExe"
}
if (-not (Test-Path $targetClasses)) {
    throw "Compiled classes not found: $targetClasses"
}
if (-not (Test-Path $layersDir)) {
    if (-not (Test-Path $jarPath)) {
        throw "Runtime layers not found and jar missing: $jarPath"
    }
    Write-Host '==> Extract backend runtime layers from jar' -ForegroundColor Cyan
    & $JavaExe '-Djarmode=layertools' '-jar' $jarPath 'extract' '--destination' $layersDir
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to extract backend runtime layers.'
    }
}

[pscustomobject]@{
    javaExe = $JavaExe
    targetClasses = $targetClasses
    layersDir = $layersDir
} | ConvertTo-Json -Depth 4
