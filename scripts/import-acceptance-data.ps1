param(
    [string]$ConfigFile = "E:\Store\SDJZU\毕设\JQPro\src\main\resources\application.yml",
    [string]$MysqlExe = "D:\DownLoad\mysql-8.0.33-winx64\bin\mysql.exe"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not (Test-Path $ConfigFile)) {
    throw "Config file not found: $ConfigFile"
}
if (-not (Test-Path $MysqlExe)) {
    throw "mysql client not found: $MysqlExe"
}

$content = Get-Content -Path $ConfigFile
$urlLine = ($content | Where-Object { $_ -match '^\s*url:\s*jdbc:mysql://' } | Select-Object -First 1)
$userLine = ($content | Where-Object { $_ -match '^\s*username:' } | Select-Object -First 1)
$passwordLine = ($content | Where-Object { $_ -match '^\s*password:' } | Select-Object -First 1)

if (-not $urlLine -or -not $userLine -or -not $passwordLine) {
    throw 'Failed to parse datasource config from application.yml'
}

$url = ($urlLine -replace '^\s*url:\s*', '').Trim()
$username = ($userLine -replace '^\s*username:\s*', '').Trim()
$password = ($passwordLine -replace '^\s*password:\s*', '').Trim()

$match = [regex]::Match($url, 'jdbc:mysql://(?<host>[^:/?]+)(:(?<port>\d+))?/(?<db>[^?]+)')
if (-not $match.Success) {
    throw "Unsupported jdbc url: $url"
}

$dbHost = $match.Groups['host'].Value
$port = if ($match.Groups['port'].Success) { $match.Groups['port'].Value } else { '3306' }
$database = $match.Groups['db'].Value

$sqlFiles = @(
    'E:\Store\SDJZU\毕设\JQPro\spec\02_Proposals\第4-6阶段_测试数据.sql',
    'E:\Store\SDJZU\毕设\JQPro\spec\02_Proposals\第7-9阶段_测试数据.sql'
)

foreach ($sqlFile in $sqlFiles) {
    if (-not (Test-Path $sqlFile)) {
        continue
    }
    Write-Host "==> Importing $sqlFile" -ForegroundColor Cyan
    $cmd = "`"$MysqlExe`" --default-character-set=utf8mb4 --host=$dbHost --port=$port --user=$username --password=$password $database < `"$sqlFile`""
    cmd /c $cmd
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to import [$sqlFile]"
    }
}

[pscustomobject]@{
    host     = $dbHost
    port     = $port
    database = $database
    username = $username
    imported = $sqlFiles | Where-Object { Test-Path $_ }
} | ConvertTo-Json -Depth 5
