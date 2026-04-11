param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$StudentAccount = "20230001",
    [string]$StudentPassword = "Jqpro@123",
    [string]$CounselorAccount = "teacher01",
    [string]$CounselorPassword = "Jqpro@123"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $scriptDir "common.ps1")

Write-Step "Student login"
$studentLogin = Login-JqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $StudentPassword
$studentHeaders = New-AuthHeaders -Token $studentLogin.data.token

Write-Step "Counselor login"
$counselorLogin = Login-JqPro -BaseUrl $BaseUrl -Account $CounselorAccount -Password $CounselorPassword
$counselorHeaders = New-AuthHeaders -Token $counselorLogin.data.token

Write-Step "Get student current user"
$currentUser = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/auth/current-user" -Headers $studentHeaders
Assert-ResultSuccess -Result $currentUser -ActionName "student current user"
$studentUserId = [int64]$currentUser.data.userId

Write-Step "List scales"
$scales = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales" -Headers $studentHeaders
Assert-ResultSuccess -Result $scales -ActionName "list scales"
if ($scales.data.Count -lt 1) {
    throw "No active scales available."
}
$scaleId = [int64]$scales.data[0].id

Write-Step "Get scale detail"
$scaleDetail = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/$scaleId" -Headers $studentHeaders
Assert-ResultSuccess -Result $scaleDetail -ActionName "scale detail"

Write-Step "Create or get draft session"
$draftSession = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/$scaleId/sessions/draft" -Method "POST" -Headers $studentHeaders
Assert-ResultSuccess -Result $draftSession -ActionName "draft session"
$sessionId = [int64]$draftSession.data.sessionId

Write-Step "Load all questions"
$questionPage = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/sessions/$sessionId/questions?pageNum=1&pageSize=50" -Headers $studentHeaders
Assert-ResultSuccess -Result $questionPage -ActionName "question page"
if ($questionPage.data.records.Count -lt 1) {
    throw "No questions found in current scale."
}

Write-Step "Build max-score answers"
$answers = @()
foreach ($question in $questionPage.data.records) {
    $selected = @($question.options | Sort-Object score, id)[-1]
    $answers += @{
        questionId = [int64]$question.questionId
        optionId   = [int64]$selected.id
    }
}

Write-Step "Save answers"
$saveResult = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/sessions/$sessionId/answers" -Method "PUT" -Headers $studentHeaders -Body @{
    answers = $answers
}
Assert-ResultSuccess -Result $saveResult -ActionName "save answers"

Write-Step "Submit scale"
$submitResult = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/sessions/$sessionId/submit" -Method "POST" -Headers $studentHeaders
Assert-ResultSuccess -Result $submitResult -ActionName "submit scale"
$reportId = [int64]$submitResult.data.reportId

Write-Step "Student list reports"
$studentReports = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/reports" -Headers $studentHeaders
Assert-ResultSuccess -Result $studentReports -ActionName "student report list"

Write-Step "Student get report detail"
$studentReportDetail = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/reports/$reportId" -Headers $studentHeaders
Assert-ResultSuccess -Result $studentReportDetail -ActionName "student report detail"

Write-Step "Counselor list bound student reports"
$counselorReports = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/counselor/students/$studentUserId/reports" -Headers $counselorHeaders
Assert-ResultSuccess -Result $counselorReports -ActionName "counselor report list"

Write-Step "Counselor get bound student report detail"
$counselorReportDetail = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/counselor/students/$studentUserId/reports/$reportId" -Headers $counselorHeaders
Assert-ResultSuccess -Result $counselorReportDetail -ActionName "counselor report detail"

Write-Success "Phase1 assessment/report script passed"
[pscustomobject]@{
    scaleId            = $scaleId
    scaleName          = $scaleDetail.data.name
    sessionId          = $sessionId
    reportId           = $reportId
    totalScore         = $submitResult.data.totalScore
    levelCode          = $submitResult.data.levelCode
    studentReports     = $studentReports.data.Count
    counselorReports   = $counselorReports.data.Count
    aiInterpretationOk = [bool](-not [string]::IsNullOrWhiteSpace($studentReportDetail.data.aiInterpretation))
} | ConvertTo-Json -Depth 10
