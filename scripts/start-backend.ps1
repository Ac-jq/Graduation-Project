param(
    [string]$JavaExe = "E:\environment\JDK21\jdk-21.0.2\bin\java.exe",
    [switch]$ForceRestart
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'prepare-runtime.ps1') -JavaExe $JavaExe | Out-Null

$existing = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($existing) {
    if (-not $ForceRestart) {
        [pscustomobject]@{
            port = 8080
            owningProcess = $existing.OwningProcess
            restarted = $false
        } | ConvertTo-Json -Depth 4
        exit 0
    }
    Stop-Process -Id $existing.OwningProcess -Force
    Start-Sleep -Seconds 2
}

$cp = "$(Join-Path $projectRoot 'target\classes');$(Join-Path $projectRoot '.codex-tmp\layers2\dependencies\BOOT-INF\lib\*');$(Join-Path $projectRoot '.codex-tmp\layers2\snapshot-dependencies\BOOT-INF\lib\*')"
$job = Start-Job -ScriptBlock {
    param($javaPath, $cwd, $classPath)
    Start-Process -FilePath $javaPath -ArgumentList '-cp', $classPath, 'sdu.jiaq.jqpro.JqProApplication' -WorkingDirectory $cwd -WindowStyle Hidden
} -ArgumentList $JavaExe, $projectRoot, $cp
Start-Sleep -Seconds 2
Receive-Job -Id $job.Id -Keep | Out-Null
$listener = $null
for ($attempt = 0; $attempt -lt 15; $attempt++) {
    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        break
    }
    Start-Sleep -Seconds 1
}
if (-not $listener) {
    throw 'Backend failed to start on 8080.'
}

[pscustomobject]@{
    port = 8080
    owningProcess = $listener.OwningProcess
    restarted = [bool]$ForceRestart
} | ConvertTo-Json -Depth 4
