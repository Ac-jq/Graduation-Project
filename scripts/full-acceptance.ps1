param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$MysqlExe = "D:\DownLoad\mysql-8.0.33-winx64\bin\mysql.exe"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host '==> Import acceptance data' -ForegroundColor Cyan
$importResult = powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir 'import-acceptance-data.ps1') -MysqlExe $MysqlExe | Out-String

Write-Host '==> Run phase1 auth/profile acceptance' -ForegroundColor Cyan
$phase1Auth = powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir 'phase1-auth-profile.ps1') -BaseUrl $BaseUrl | Out-String

Write-Host '==> Run phase1 assessment/report acceptance' -ForegroundColor Cyan
$phase1Assessment = powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir 'phase1-assessment-report.ps1') -BaseUrl $BaseUrl | Out-String

Write-Host '==> Run phase2 AI/appointment acceptance' -ForegroundColor Cyan
$phase2 = powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir 'phase2-aichat-appointment.ps1') -BaseUrl $BaseUrl -MysqlExe $MysqlExe | Out-String

Write-Host '==> Run phase3 resource governance acceptance' -ForegroundColor Cyan
$phase3 = powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir 'phase3-resource-governance.ps1') -BaseUrl $BaseUrl | Out-String

Write-Host '==> Run phase4-6 closure acceptance' -ForegroundColor Cyan
$phase46 = powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir 'phase4-6-closure.ps1') -BaseUrl $BaseUrl | Out-String

Write-Host '==> Run phase7 security regression acceptance' -ForegroundColor Cyan
$phase7 = powershell -ExecutionPolicy Bypass -File (Join-Path $scriptDir 'phase7-security-regression.ps1') -BaseUrl $BaseUrl -MysqlExe $MysqlExe | Out-String

[pscustomobject]@{
    imported = $importResult.Trim()
    phase1Auth = $phase1Auth.Trim()
    phase1Assessment = $phase1Assessment.Trim()
    phase2 = $phase2.Trim()
    phase3 = $phase3.Trim()
    phase46 = $phase46.Trim()
    phase7 = $phase7.Trim()
} | ConvertTo-Json -Depth 5
