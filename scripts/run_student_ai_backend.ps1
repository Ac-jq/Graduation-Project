$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$localAiEnv = Join-Path $root '.local\deepseek.env.ps1'
if (Test-Path $localAiEnv) {
    . $localAiEnv
}

$env:JQPRO_AI_CHAT_ENABLED = 'true'

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

$existing = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue |
    Where-Object { $_.State -eq 'Listen' } |
    Select-Object -ExpandProperty OwningProcess -Unique

foreach ($processId in $existing) {
    if ($processId -and $processId -ne 0) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}

$outLog = Join-Path $root '.codex-student-ai-backend.out.log'
$errLog = Join-Path $root '.codex-student-ai-backend.err.log'

$process = Start-Process `
    -FilePath $mavenCmd `
    -ArgumentList '-q', 'spring-boot:run' `
    -WorkingDirectory $root `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -PassThru

Start-Sleep -Seconds 22
Write-Host "[student-ai] backend pid: $($process.Id)"
