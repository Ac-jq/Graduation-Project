$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$localAiEnv = Join-Path $root '.local\deepseek.env.ps1'
if (Test-Path $localAiEnv) {
    Write-Host '[admin-ai] load local AI env'
    . $localAiEnv
}

$javaCandidates = @(
    'C:\Users\JQ\.jdks\jbr-17.0.12',
    'E:\environment\JDK17\jdk-17',
    'E:\environment\JDK21\jdk-21.0.2',
    $env:JAVA_HOME
) | Where-Object { $_ -and (Test-Path (Join-Path $_ 'bin\java.exe')) }

$mvnCandidates = @(
    'C:\Users\JQ\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd',
    'E:\environment\apache-maven-3.9.9\bin\mvn.cmd',
    'E:\environment\apache-maven-3.9.6-bin\apache-maven-3.9.6\bin\mvn.cmd'
) | Where-Object { Test-Path $_ }

$javaHome = $javaCandidates | Select-Object -First 1
$mavenCmd = $mvnCandidates | Select-Object -First 1

if (-not $javaHome) {
    throw 'java home not found'
}
if (-not $mavenCmd) {
    throw 'maven command not found'
}

$env:JAVA_HOME = $javaHome

$existing = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "[admin-ai] stop existing backend pid $($existing.OwningProcess)"
    Stop-Process -Id $existing.OwningProcess -Force
    Start-Sleep -Seconds 2
}

$outLog = Join-Path $root '.codex-admin-backend.out.log'
$errLog = Join-Path $root '.codex-admin-backend.err.log'
Remove-Item $outLog, $errLog -ErrorAction SilentlyContinue

Write-Host '[admin-ai] start backend with spring-boot:run on http://127.0.0.1:8080'
$process = Start-Process `
    -FilePath $mavenCmd `
    -ArgumentList 'spring-boot:run' `
    -WorkingDirectory $root `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -PassThru

Start-Sleep -Seconds 20
Write-Host "[admin-ai] backend pid: $($process.Id)"
