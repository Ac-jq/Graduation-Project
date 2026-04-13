$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$base = 'http://127.0.0.1:8080'

function Login($account, $password) {
    $body = @{ account = $account; password = $password } | ConvertTo-Json -Compress
    return (Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType "application/json; charset=utf-8" -Body $body).data.token
}

function Assert-True($condition, $message) {
    if (-not $condition) {
        throw $message
    }
}

function Assert-Equal($actual, $expected, $message) {
    if ($actual -ne $expected) {
        throw "$message expected=$expected actual=$actual"
    }
}

function Get-AllQuestions($headers, $sessionId, $pageSize, $expectedTotal) {
    $pageNum = 1
    $records = @()
    do {
        $resp = (Invoke-RestMethod -Method Get -Uri "$base/api/scales/sessions/$sessionId/questions?pageNum=$pageNum&pageSize=$pageSize" -Headers $headers).data
        $records += @($resp.records)
        $pageNum++
    } while ($records.Count -lt $expectedTotal)
    return $records
}

function Build-AnswerBodyByPlan($records, $scorePlan) {
    $answers = @()
    for ($i = 0; $i -lt $records.Count; $i++) {
        $record = $records[$i]
        $expectedScore = [int]$scorePlan[$i]
        $option = $record.options | Where-Object { [int]$_.score -eq $expectedScore } | Select-Object -First 1
        if (-not $option) {
            throw "missing option score=$expectedScore questionId=$($record.questionId)"
        }
        $answers += [pscustomobject]@{
            questionId = [long]$record.questionId
            optionId   = [long]$option.id
        }
    }
    return ([pscustomobject]@{ answers = @($answers) } | ConvertTo-Json -Depth 6 -Compress)
}

function Submit-ScaleCase($headers, $scale, $scorePlan, $expectedLevel) {
    $detail = (Invoke-RestMethod -Method Get -Uri "$base/api/scales/$($scale.id)" -Headers $headers).data
    $draft = (Invoke-RestMethod -Method Post -Uri "$base/api/scales/$($scale.id)/sessions/draft" -Headers $headers).data
    $records = @(Get-AllQuestions $headers $draft.sessionId $detail.pageSize $detail.totalQuestions)
    $body = Build-AnswerBodyByPlan $records $scorePlan
    Invoke-RestMethod -Method Put -Uri "$base/api/scales/sessions/$($draft.sessionId)/answers" -Headers $headers -ContentType "application/json; charset=utf-8" -Body $body | Out-Null
    $submit = (Invoke-RestMethod -Method Post -Uri "$base/api/scales/sessions/$($draft.sessionId)/submit" -Headers $headers).data
    Assert-Equal $submit.levelCode $expectedLevel "submit level mismatch for $($scale.code)"
    Assert-True (-not [string]::IsNullOrWhiteSpace($submit.noticeText)) "submit noticeText missing for $($scale.code)"
    return $submit.reportId
}

function Assert-SafeReport($report, $roleTag) {
    Assert-True (-not [string]::IsNullOrWhiteSpace($report.noticeText)) "$roleTag missing noticeText"
    Assert-True (-not [string]::IsNullOrWhiteSpace($report.summaryText)) "$roleTag missing summaryText"
    Assert-True (-not [string]::IsNullOrWhiteSpace($report.aiInterpretation)) "$roleTag missing aiInterpretation"
    Assert-True (-not [string]::IsNullOrWhiteSpace($report.recommendationNote)) "$roleTag missing recommendationNote"
    Assert-True ($report.aiInterpretation.Length -gt 30) "$roleTag aiInterpretation too short"
    Assert-True ($report.summaryText.Length -gt 10) "$roleTag summaryText too short"
}

$studentHeaders = @{ Authorization = (Login '20230001' 'Jqpro@123') }
$counselorHeaders = @{ Authorization = (Login 'teacher01' 'Jqpro@123') }

$scales = (Invoke-RestMethod -Method Get -Uri "$base/api/scales" -Headers $studentHeaders).data
$phq9 = $scales | Where-Object { $_.code -eq 'PHQ9' } | Select-Object -First 1
Assert-True ($null -ne $phq9) 'PHQ9 not found'

$lowReportId = Submit-ScaleCase $studentHeaders $phq9 @(0,0,0,0,0,0,0,0,0) 'LOW'
$mediumReportId = Submit-ScaleCase $studentHeaders $phq9 @(1,1,1,1,1,0,0,0,0) 'MEDIUM'
$highReportId = Submit-ScaleCase $studentHeaders $phq9 @(3,3,3,3,0,0,0,0,0) 'HIGH'

$studentLow = (Invoke-RestMethod -Method Get -Uri "$base/api/student/reports/$lowReportId" -Headers $studentHeaders).data
$studentMedium = (Invoke-RestMethod -Method Get -Uri "$base/api/student/reports/$mediumReportId" -Headers $studentHeaders).data
$studentHigh = (Invoke-RestMethod -Method Get -Uri "$base/api/student/reports/$highReportId" -Headers $studentHeaders).data

Assert-SafeReport $studentLow 'student low report'
Assert-SafeReport $studentMedium 'student medium report'
Assert-SafeReport $studentHigh 'student high report'

Assert-True ($studentHigh.recommendAppointment -eq $true) 'high report should recommend appointment'
Assert-True ($studentLow.recommendAppointment -eq $false) 'low report should not recommend appointment'
Assert-True ($studentLow.noticeText -eq $studentMedium.noticeText) 'notice text should stay consistent'
Assert-True ($studentMedium.noticeText -eq $studentHigh.noticeText) 'notice text should stay consistent across levels'

$counselorHigh = (Invoke-RestMethod -Method Get -Uri "$base/api/counselor/students/$($studentHigh.studentUserId)/reports/$highReportId" -Headers $counselorHeaders).data
Assert-SafeReport $counselorHigh 'counselor high report'
Assert-Equal $counselorHigh.levelCode 'HIGH' 'counselor report level mismatch'

[ordered]@{
    lowReportId = $lowReportId
    mediumReportId = $mediumReportId
    highReportId = $highReportId
    noticeLength = $studentHigh.noticeText.Length
    aiInterpretationLength = $studentHigh.aiInterpretation.Length
    verifiedAt = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
} | ConvertTo-Json -Depth 6
