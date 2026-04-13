$ErrorActionPreference = 'Stop'

$mysql = 'D:\DownLoad\mysql-8.0.33-winx64\bin\mysql.exe'
if (-not (Test-Path $mysql)) {
    throw 'mysql client not found'
}

$baseUrl = 'http://127.0.0.1:8080/api'

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [string]$Token,
        [string]$Body
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = $Token
    }

    if ($Body) {
        return Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $Body
    }
    return Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers
}

function Invoke-Sql {
    param([Parameter(Mandatory = $true)][string]$Sql)
    & $mysql -uroot -p123456 -D jqpro -e $Sql | Out-Host
}

Write-Host '[verify-admin-ai] prepare dormant demo student'
Invoke-Sql @"
INSERT INTO sys_user (account, password_salt, password_hash, role_code, real_name, display_name, student_no, status, created_at, updated_at)
SELECT '20209998', password_salt, password_hash, 'STUDENT', 'Dormant Student', 'Dormant Student', '20209998', 'ACTIVE', '2025-01-10 09:00:00', NOW()
FROM sys_user
WHERE account = '20230001'
  AND NOT EXISTS (SELECT 1 FROM sys_user WHERE account = '20209998');

UPDATE sys_user
SET status = 'ACTIVE', created_at = '2025-01-10 09:00:00', updated_at = NOW()
WHERE account = '20209998';

INSERT INTO student_profile (user_id, avatar_url, college, grade, gender, phone, emergency_contact, emergency_phone, counselor_user_id)
SELECT u.id, 'https://example.com/avatar/dormant.png', 'Software College', '2020', 'UNKNOWN', '13800009998', 'Dormant Contact', '13900009998', 2
FROM sys_user u
WHERE u.account = '20209998'
  AND NOT EXISTS (SELECT 1 FROM student_profile sp WHERE sp.user_id = u.id);

INSERT INTO counselor_student (counselor_user_id, student_user_id)
SELECT 2, u.id
FROM sys_user u
WHERE u.account = '20209998'
  AND NOT EXISTS (SELECT 1 FROM counselor_student cs WHERE cs.counselor_user_id = 2 AND cs.student_user_id = u.id);

UPDATE mental_resource SET status = 'PUBLISHED', published_at = NOW(), updated_at = NOW() WHERE id = 23;
"@

Write-Host '[verify-admin-ai] login admin'
$login = Invoke-Api -Method Post -Uri "$baseUrl/auth/login" -Body (@{ account = 'admin'; password = 'Jqpro@123' } | ConvertTo-Json)
$adminToken = $login.data.token

$counselorNo = 'T' + (Get-Date -Format 'MMddHHmmss')
$counselorInstruction = "Create a counselor named Zhang San with counselorNo $counselorNo"

Write-Host '[verify-admin-ai] parse counselor create'
$createTask = Invoke-Api -Method Post -Uri "$baseUrl/admin/ai-tasks/parse" -Token $adminToken -Body (@{ instruction = $counselorInstruction } | ConvertTo-Json)
$createTaskId = $createTask.data.task.taskId
Write-Host '[verify-admin-ai] confirm counselor create'
$createConfirm = Invoke-Api -Method Post -Uri "$baseUrl/admin/ai-tasks/$createTaskId/confirm" -Token $adminToken

Write-Host '[verify-admin-ai] parse resource offline'
$resourceTask = Invoke-Api -Method Post -Uri "$baseUrl/admin/ai-tasks/parse" -Token $adminToken -Body '{"instruction":"Take resource id 23 offline"}'
$resourceTaskId = $resourceTask.data.task.taskId
Write-Host '[verify-admin-ai] confirm resource offline'
$resourceConfirm = Invoke-Api -Method Post -Uri "$baseUrl/admin/ai-tasks/$resourceTaskId/confirm" -Token $adminToken

Write-Host '[verify-admin-ai] parse batch disable'
$batchTask = Invoke-Api -Method Post -Uri "$baseUrl/admin/ai-tasks/parse" -Token $adminToken -Body '{"instruction":"Disable student accounts inactive for 3 months"}'
$batchTaskId = $batchTask.data.task.taskId
Write-Host '[verify-admin-ai] confirm batch disable'
$batchConfirm = Invoke-Api -Method Post -Uri "$baseUrl/admin/ai-tasks/$batchTaskId/confirm" -Token $adminToken

Write-Host '[verify-admin-ai] parse and cancel enable task'
$cancelTask = Invoke-Api -Method Post -Uri "$baseUrl/admin/ai-tasks/parse" -Token $adminToken -Body '{"instruction":"Enable account: 20209998"}'
$cancelTaskId = $cancelTask.data.task.taskId
$cancelResult = Invoke-Api -Method Post -Uri "$baseUrl/admin/ai-tasks/$cancelTaskId/cancel" -Token $adminToken

Write-Host '[verify-admin-ai] verify resource and users'
$dormantUser = Invoke-Api -Method Get -Uri "$baseUrl/admin/users?keyword=20209998" -Token $adminToken
$newCounselor = Invoke-Api -Method Get -Uri "$baseUrl/admin/users?keyword=$counselorNo" -Token $adminToken
$counselorLogin = Invoke-Api -Method Post -Uri "$baseUrl/auth/login" -Body (@{ account = $createConfirm.data.items[0].targetLabel; password = 'Jqpro@123' } | ConvertTo-Json)

Invoke-Sql "SELECT id, title, status FROM mental_resource WHERE id = 23;"

$summary = [ordered]@{
    counselorTask = $createConfirm.data.summaryText
    counselorAccount = $createConfirm.data.items[0].targetLabel
    counselorNo = $counselorNo
    resourceTask = $resourceConfirm.data.summaryText
    dormantStudentStatus = $dormantUser.data[0].status
    counselorCreatedStatus = $newCounselor.data[0].status
    counselorLoginRole = $counselorLogin.data.roleCode
    canceledTaskStatus = $cancelResult.data.confirmStatus
}

Write-Host '[verify-admin-ai] verification summary'
$summary | ConvertTo-Json -Depth 6 | Out-Host
