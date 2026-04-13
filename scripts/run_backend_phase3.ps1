$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$localAiEnv = Join-Path $root '.local\\deepseek.env.ps1'
if (Test-Path $localAiEnv) {
    Write-Host '[phase3] load local AI env'
    . $localAiEnv
}

$javaCandidates = @(
    $env:JAVA_HOME,
    'E:\environment\JDK17\jdk-17',
    'E:\environment\JDK21\jdk-21.0.2',
    'E:\JDK17',
    'E:\JDK21\jdk-21.0.2'
) | Where-Object { $_ }
$javaHome = $javaCandidates | Where-Object { Test-Path (Join-Path $_ 'bin\java.exe') } | Select-Object -First 1
$mvnCandidates = @(
    'C:\Users\JQ\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd',
    'E:\environment\apache-maven-3.9.9\bin\mvn.cmd',
    'E:\environment\apache-maven-3.9.6-bin\apache-maven-3.9.6\bin\mvn.cmd'
)
$mavenCmd = $mvnCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not $javaHome -or -not (Test-Path "$javaHome\bin\java.exe")) {
    throw "java.exe not found under JAVA_HOME: $javaHome"
}
if (-not $mavenCmd) {
    throw 'maven command not found'
}

$env:JAVA_HOME = $javaHome
& $mavenCmd -q -DskipTests package

$jarPath = Join-Path $root 'target\JQPro-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path $jarPath)) {
    throw "packaged jar not found: $jarPath"
}

& "$javaHome\bin\java.exe" '-Dfile.encoding=UTF-8' '-jar' $jarPath
