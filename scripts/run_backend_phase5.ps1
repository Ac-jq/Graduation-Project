$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$localAiEnv = Join-Path $root '.local\\deepseek.env.ps1'
if (Test-Path $localAiEnv) {
    Write-Host '[phase5] load local AI env'
    . $localAiEnv
}

$javaCandidates = @(
    'C:\Users\JQ\.jdks\jbr-17.0.12',
    'E:\environment\JDK21\jdk-21.0.2',
    'E:\environment\JDK17\jdk-17',
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

Write-Host '[phase5] package backend'
& $mavenCmd -q -DskipTests package

$jarPath = Join-Path $root 'target\JQPro-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path $jarPath)) {
    throw "packaged jar not found: $jarPath"
}

$outLog = Join-Path $root '.codex-backend.out.log'
$errLog = Join-Path $root '.codex-backend.err.log'

Write-Host '[phase5] start backend on http://127.0.0.1:8080'
$process = Start-Process `
    -FilePath (Join-Path $javaHome 'bin\java.exe') `
    -ArgumentList '-Dfile.encoding=UTF-8', '-jar', $jarPath `
    -WorkingDirectory $root `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -PassThru

Start-Sleep -Seconds 18
Write-Host "[phase5] backend pid: $($process.Id)"
