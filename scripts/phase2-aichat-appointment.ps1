param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$StudentAccount = "20230001",
    [string]$StudentPassword = "Jqpro@123",
    [string]$CounselorAccount = "teacher01",
    [string]$CounselorPassword = "Jqpro@123",
    [string]$MysqlExe = "D:\DownLoad\mysql-8.0.33-winx64\bin\mysql.exe"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $scriptDir "common.ps1")

Write-Step "Student login"
$studentLogin = Login-JqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $StudentPassword
$studentHeaders = New-AuthHeaders -Token $studentLogin.data.token

Write-Step "Counselor login"
$counselorLogin = Login-JqPro -BaseUrl $BaseUrl -Account $CounselorAccount -Password $CounselorPassword
$counselorHeaders = New-AuthHeaders -Token $counselorLogin.data.token
$counselorUserId = [int64]$counselorLogin.data.userId

Write-Step "Get student current user"
$studentCurrent = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/auth/current-user" -Headers $studentHeaders
Assert-ResultSuccess -Result $studentCurrent -ActionName "student current user"
$studentUserId = [int64]$studentCurrent.data.userId

Write-Step "Create AI session"
$aiSession = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/ai-sessions" -Method "POST" -Headers $studentHeaders -Body @{
    title = "Phase2 acceptance AI session"
}
Assert-ResultSuccess -Result $aiSession -ActionName "create AI session"
$aiSessionId = [int64]$aiSession.data.sessionId

Write-Step "Send student AI message"
$aiSend = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/ai-sessions/$aiSessionId/messages" -Method "POST" -Headers $studentHeaders -Body @{
    content = "I am under study pressure, but I am still trying to adjust."
}
Assert-ResultSuccess -Result $aiSend -ActionName "send AI message"

Write-Step "List student AI messages"
$studentAiMessages = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/ai-sessions/$aiSessionId/messages" -Headers $studentHeaders
Assert-ResultSuccess -Result $studentAiMessages -ActionName "student AI messages"

Write-Step "Counselor list bound student AI sessions"
$counselorAiSessions = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/counselor/students/$studentUserId/ai-sessions" -Headers $counselorHeaders
Assert-ResultSuccess -Result $counselorAiSessions -ActionName "counselor AI sessions"

Write-Step "Counselor list bound student AI messages"
$counselorAiMessages = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/counselor/students/$studentUserId/ai-sessions/$aiSessionId/messages" -Headers $counselorHeaders
Assert-ResultSuccess -Result $counselorAiMessages -ActionName "counselor AI messages"

Write-Step "List open appointment slots"
$slots = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/appointments/slots" -Headers $studentHeaders
Assert-ResultSuccess -Result $slots -ActionName "open appointment slots"
if ($slots.data.Count -lt 1) {
    if (-not (Test-Path $MysqlExe)) {
        throw "No open appointment slots found, and mysql client was not found at [$MysqlExe]."
    }
    Write-Step "No open slots found, insert one temporary slot for repeatable acceptance"
    $sql = "INSERT INTO consult_appointment_slot (counselor_user_id, start_time, end_time, status) VALUES ($counselorUserId, DATE_ADD(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 60 MINUTE), 'OPEN');"
    & $MysqlExe -h 127.0.0.1 -uroot -p123456 jqpro -e $sql | Out-Null
    $slots = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/appointments/slots" -Headers $studentHeaders
    Assert-ResultSuccess -Result $slots -ActionName "open appointment slots after seed"
    if ($slots.data.Count -lt 1) {
        throw "No open appointment slots found even after slot seed."
    }
}
$slotId = [int64]$slots.data[0].slotId

Write-Step "Create anonymous appointment"
$appointment = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/appointments" -Method "POST" -Headers $studentHeaders -Body @{
    slotId       = $slotId
    issueSummary = "Need an anonymous counseling session about study pressure."
}
Assert-ResultSuccess -Result $appointment -ActionName "create appointment"
$appointmentId = [int64]$appointment.data.appointmentId

Write-Step "Counselor list appointments"
$counselorAppointments = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/counselor/appointments" -Headers $counselorHeaders
Assert-ResultSuccess -Result $counselorAppointments -ActionName "counselor appointments"

Write-Step "Counselor accept appointment"
$accept = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/counselor/appointments/$appointmentId/accept" -Method "POST" -Headers $counselorHeaders -Body @{
    resultMessage = "Please enter the chat room at the scheduled time."
}
Assert-ResultSuccess -Result $accept -ActionName "accept appointment"

Write-Step "Student list notifications"
$notifications = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/notifications" -Headers $studentHeaders
Assert-ResultSuccess -Result $notifications -ActionName "student notifications"

if ($notifications.data.Count -gt 0) {
    Write-Step "Mark first notification as read"
    $firstNotificationId = [int64]$notifications.data[0].notificationId
    $markRead = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/notifications/$firstNotificationId/read" -Method "POST" -Headers $studentHeaders
    Assert-ResultSuccess -Result $markRead -ActionName "mark one notification read"
}

Write-Step "Mark all notifications as read"
$markAllRead = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/notifications/read-all" -Method "POST" -Headers $studentHeaders
Assert-ResultSuccess -Result $markAllRead -ActionName "mark all notifications read"

Write-Step "Get chat session"
$chatSession = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/chat/appointments/$appointmentId/session" -Headers $studentHeaders
Assert-ResultSuccess -Result $chatSession -ActionName "chat session"

Write-Step "List chat history"
$chatMessages = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/chat/appointments/$appointmentId/messages" -Headers $studentHeaders
Assert-ResultSuccess -Result $chatMessages -ActionName "chat messages"

$websocketUrl = ("ws://{0}/ws/consult-chat?token={1}&appointmentId={2}" -f $BaseUrl.Replace("http://", "").Replace("https://", ""), $studentLogin.data.token, $appointmentId)

Write-Success "Phase2 AI/appointment script passed"
[pscustomobject]@{
    aiSessionId               = $aiSessionId
    aiRiskLevel               = $aiSend.data.riskLevel
    studentAiMessageCount     = $studentAiMessages.data.Count
    counselorAiMessageCount   = $counselorAiMessages.data.Count
    slotId                    = $slotId
    appointmentId             = $appointmentId
    appointmentStatus         = $accept.data.status
    anonymousName             = $accept.data.anonymousName
    notificationCount         = $notifications.data.Count
    chatSessionId             = $chatSession.data.chatSessionId
    chatSessionStatus         = $chatSession.data.status
    chatHistoryCount          = $chatMessages.data.Count
    websocketManualTestUrl    = $websocketUrl
    websocketManualTestRemark = "Use a WebSocket client and send {""content"":""test message""} during the valid appointment window."
} | ConvertTo-Json -Depth 10
