param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$StudentAccount = "20230001",
    [string]$StudentPassword = "Jqpro@123",
    [string]$CounselorAccount = "teacher01",
    [string]$CounselorPassword = "Jqpro@123",
    [string]$AdminAccount = "admin",
    [string]$AdminPassword = "Jqpro@123",
    [string]$UnboundStudentAccount = "20230002",
    [string]$UnboundStudentPassword = "Jqpro@123",
    [string]$MysqlExe = "D:\DownLoad\mysql-8.0.33-winx64\bin\mysql.exe",
    [string]$ConfigFile = "E:\Store\SDJZU\毕设\JQPro\src\main\resources\application.yml"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $scriptDir 'common.ps1')

if (-not (Test-Path $MysqlExe)) {
    throw "mysql client not found: $MysqlExe"
}
if (-not (Test-Path $ConfigFile)) {
    throw "Config file not found: $ConfigFile"
}

$configLines = Get-Content -Path $ConfigFile
$urlLine = ($configLines | Where-Object { $_ -match '^\s*url:\s*jdbc:mysql://' } | Select-Object -First 1)
$userLine = ($configLines | Where-Object { $_ -match '^\s*username:' } | Select-Object -First 1)
$passwordLine = ($configLines | Where-Object { $_ -match '^\s*password:' } | Select-Object -First 1)
$jdbcUrl = ($urlLine -replace '^\s*url:\s*', '').Trim()
$dbUser = ($userLine -replace '^\s*username:\s*', '').Trim()
$dbPassword = ($passwordLine -replace '^\s*password:\s*', '').Trim()
$dbMatch = [regex]::Match($jdbcUrl, 'jdbc:mysql://(?<host>[^:/?]+)(:(?<port>\d+))?/(?<db>[^?]+)')
if (-not $dbMatch.Success) {
    throw "Unsupported jdbc url: $jdbcUrl"
}
$dbHost = $dbMatch.Groups['host'].Value
$dbPort = if ($dbMatch.Groups['port'].Success) { $dbMatch.Groups['port'].Value } else { '3306' }
$dbName = $dbMatch.Groups['db'].Value

Write-Step 'Import acceptance seed data'
& (Join-Path $scriptDir 'import-acceptance-data.ps1') -MysqlExe $MysqlExe | Out-Null

Write-Step 'Student login'
$studentLogin = Login-JqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $StudentPassword
$studentHeaders = New-AuthHeaders -Token $studentLogin.data.token
$studentCurrent = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/auth/current-user' -Headers $studentHeaders
Assert-ResultSuccess -Result $studentCurrent -ActionName 'student current user'
$studentUserId = [int64]$studentCurrent.data.userId

Write-Step 'Counselor login'
$counselorLogin = Login-JqPro -BaseUrl $BaseUrl -Account $CounselorAccount -Password $CounselorPassword
$counselorHeaders = New-AuthHeaders -Token $counselorLogin.data.token

Write-Step 'Admin login'
$adminLogin = Login-JqPro -BaseUrl $BaseUrl -Account $AdminAccount -Password $AdminPassword
$adminHeaders = New-AuthHeaders -Token $adminLogin.data.token

Write-Step 'Unbound student login'
$unboundStudentLogin = Login-JqPro -BaseUrl $BaseUrl -Account $UnboundStudentAccount -Password $UnboundStudentPassword
$unboundStudentHeaders = New-AuthHeaders -Token $unboundStudentLogin.data.token
$unboundCurrent = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/auth/current-user' -Headers $unboundStudentHeaders
Assert-ResultSuccess -Result $unboundCurrent -ActionName 'unbound student current user'
$unboundStudentUserId = [int64]$unboundCurrent.data.userId

Write-Step 'Create AI session and send encrypted messages'
$aiSession = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/student/ai-sessions' -Method 'POST' -Headers $studentHeaders -Body @{
    title = 'Phase7 security acceptance session'
}
Assert-ResultSuccess -Result $aiSession -ActionName 'create ai session'
$sessionId = [int64]$aiSession.data.sessionId
$aiSend = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/ai-sessions/$sessionId/messages" -Method 'POST' -Headers $studentHeaders -Body @{
    content = 'Phase7 security acceptance message: I feel stressed but I am still safe and working.'
}
Assert-ResultSuccess -Result $aiSend -ActionName 'send ai message'
$studentMessageId = [int64]$aiSend.data.studentMessage.messageId
$aiMessageId = [int64]$aiSend.data.aiMessage.messageId

Write-Step 'Verify encrypted storage in MySQL'
$prefixRows = & $MysqlExe --default-character-set=utf8mb4 --host=$dbHost --port=$dbPort --user=$dbUser --password=$dbPassword $dbName -N -e "select id, left(content_text, 5) from ai_chat_message where id in ($studentMessageId,$aiMessageId) order by id;"
Assert-True -Condition ($LASTEXITCODE -eq 0) -Message 'Failed to query ai_chat_message from mysql'
$prefixCheck = @($prefixRows | Where-Object { $_ -match '^\d+\s+enc::' }).Count
Assert-True -Condition ($prefixCheck -eq 2) -Message 'AI chat messages are not stored with enc:: prefix'

Write-Step 'Verify encrypted messages remain readable to student and counselor'
$studentMessages = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/ai-sessions/$sessionId/messages" -Headers $studentHeaders
$counselorMessages = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/counselor/students/$studentUserId/ai-sessions/$sessionId/messages" -Headers $counselorHeaders
Assert-ResultSuccess -Result $studentMessages -ActionName 'student ai messages'
Assert-ResultSuccess -Result $counselorMessages -ActionName 'counselor ai messages'
Assert-True -Condition ($studentMessages.data.Count -ge 2) -Message 'Student cannot read encrypted AI messages'
Assert-True -Condition ($counselorMessages.data.Count -ge 2) -Message 'Counselor cannot read encrypted AI messages'

Write-Step 'Verify role-based permission isolation'
$studentToAdmin = Invoke-JqProWebRequest -BaseUrl $BaseUrl -Path '/api/admin/users' -Headers $studentHeaders
Assert-HttpStatus -Response $studentToAdmin -ExpectedStatusCodes @(403) -ActionName 'student access admin users'
$studentToCounselor = Invoke-JqProWebRequest -BaseUrl $BaseUrl -Path '/api/counselor/appointments' -Headers $studentHeaders
Assert-HttpStatus -Response $studentToCounselor -ExpectedStatusCodes @(403) -ActionName 'student access counselor appointments'
$counselorToStudentProfile = Invoke-JqProWebRequest -BaseUrl $BaseUrl -Path '/api/student/profile/me' -Headers $counselorHeaders
Assert-HttpStatus -Response $counselorToStudentProfile -ExpectedStatusCodes @(403) -ActionName 'counselor access student profile endpoint'
$counselorToUnboundReports = Invoke-JqProWebRequest -BaseUrl $BaseUrl -Path "/api/counselor/students/$unboundStudentUserId/reports" -Headers $counselorHeaders
Assert-HttpStatus -Response $counselorToUnboundReports -ExpectedStatusCodes @(400,403) -ActionName 'counselor access unbound student reports'

Write-Step 'Verify outsider student cannot access another appointment chat'
$studentAppointments = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/student/appointments' -Headers $studentHeaders
Assert-ResultSuccess -Result $studentAppointments -ActionName 'student appointment list'
$targetAppointment = @($studentAppointments.data | Where-Object { $_.appointmentId } | Select-Object -First 1)
Assert-True -Condition ($null -ne $targetAppointment) -Message 'No student appointment available for chat isolation check'
$outsiderChatAccess = Invoke-JqProWebRequest -BaseUrl $BaseUrl -Path "/api/chat/appointments/$($targetAppointment.appointmentId)/session" -Headers $unboundStudentHeaders
Assert-HttpStatus -Response $outsiderChatAccess -ExpectedStatusCodes @(400,403) -ActionName 'outsider student access private chat'

Write-Step 'Verify admin endpoint remains accessible'
$adminUsers = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/users' -Headers $adminHeaders
Assert-ResultSuccess -Result $adminUsers -ActionName 'admin list users'

Write-Success 'Phase7 security regression script passed'
[pscustomobject]@{
    studentUserId = $studentUserId
    unboundStudentUserId = $unboundStudentUserId
    aiSessionId = $sessionId
    studentMessageId = $studentMessageId
    aiMessageId = $aiMessageId
    mysqlEncryptedRows = $prefixCheck
    studentMessageCount = $studentMessages.data.Count
    counselorMessageCount = $counselorMessages.data.Count
    studentToAdminStatus = $studentToAdmin.StatusCode
    studentToCounselorStatus = $studentToCounselor.StatusCode
    counselorToStudentProfileStatus = $counselorToStudentProfile.StatusCode
    counselorToUnboundReportsStatus = $counselorToUnboundReports.StatusCode
    outsiderChatStatus = $outsiderChatAccess.StatusCode
    adminUserCount = $adminUsers.data.Count
} | ConvertTo-Json -Depth 10
