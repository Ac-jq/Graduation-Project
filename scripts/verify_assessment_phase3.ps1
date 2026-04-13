$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$base = 'http://127.0.0.1:8080'

function Login($account, $password) {
    $body = @{ account = $account; password = $password } | ConvertTo-Json
    return (Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json; charset=utf-8' -Body $body).data.token
}

function Assert-Equal($actual, $expected, $message) {
    if ($actual -ne $expected) {
        throw "$message. expected=$expected actual=$actual"
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
            throw "questionId=$($record.questionId) missing option with score $expectedScore"
        }
        $answers += [pscustomobject]@{
            questionId = [long]$record.questionId
            optionId   = [long]$option.id
        }
    }
    return ([pscustomobject]@{ answers = @($answers) } | ConvertTo-Json -Depth 6 -Compress)
}

function Verify-ScaleSeed($headers, $scale, $expectedTotal, $expectedHighMax) {
    $detail = (Invoke-RestMethod -Method Get -Uri "$base/api/scales/$($scale.id)" -Headers $headers).data
    Assert-Equal $detail.totalQuestions $expectedTotal "totalQuestions mismatch for $($scale.code)"
    Assert-Equal @($detail.scoringRules).Count 3 "rule count mismatch for $($scale.code)"

    $draft = (Invoke-RestMethod -Method Post -Uri "$base/api/scales/$($scale.id)/sessions/draft" -Headers $headers).data
    $records = @(Get-AllQuestions $headers $draft.sessionId $detail.pageSize $expectedTotal)
    Assert-Equal $records.Count $expectedTotal "question count mismatch for $($scale.code)"

    foreach ($record in $records) {
        Assert-Equal @($record.options).Count 4 "option count mismatch for questionId=$($record.questionId)"
        $scores = @($record.options | Sort-Object score | ForEach-Object { [int]$_.score })
        $scoreText = ($scores -join ',')
        Assert-Equal $scoreText '0,1,2,3' "option scores mismatch for questionId=$($record.questionId)"
    }

    $detailText = ($detail.scoringRules -join ' | ')
    if ($detailText -notmatch "HIGH: 10-$expectedHighMax") {
        throw "high rule summary mismatch for $($scale.code): $detailText"
    }
}

function Submit-ScaleCase($headers, $scale, $scorePlan, $expectedScore, $expectedLevel) {
    $detail = (Invoke-RestMethod -Method Get -Uri "$base/api/scales/$($scale.id)" -Headers $headers).data
    $draft = (Invoke-RestMethod -Method Post -Uri "$base/api/scales/$($scale.id)/sessions/draft" -Headers $headers).data
    $records = @(Get-AllQuestions $headers $draft.sessionId $detail.pageSize $detail.totalQuestions)
    $body = Build-AnswerBodyByPlan $records $scorePlan
    Invoke-RestMethod -Method Put -Uri "$base/api/scales/sessions/$($draft.sessionId)/answers" -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $body | Out-Null
    $submit = (Invoke-RestMethod -Method Post -Uri "$base/api/scales/sessions/$($draft.sessionId)/submit" -Headers $headers).data
    Assert-Equal $submit.totalScore $expectedScore "submit score mismatch for $($scale.code)"
    Assert-Equal $submit.levelCode $expectedLevel "submit level mismatch for $($scale.code)"
    $report = (Invoke-RestMethod -Method Get -Uri "$base/api/student/reports/$($submit.reportId)" -Headers $headers).data
    Assert-Equal $report.totalScore $expectedScore "report score mismatch for $($scale.code)"
    Assert-Equal $report.levelCode $expectedLevel "report level mismatch for $($scale.code)"
    if (-not $report.noticeText) {
        throw "missing noticeText for $($scale.code)"
    }
    return [pscustomobject]@{
        scaleCode  = $scale.code
        totalScore = $submit.totalScore
        levelCode  = $submit.levelCode
        reportId   = $submit.reportId
    }
}

$studentHeaders = @{ Authorization = (Login '20230001' 'Jqpro@123') }
$scales = (Invoke-RestMethod -Method Get -Uri "$base/api/scales" -Headers $studentHeaders).data
$phq9 = $scales | Where-Object { $_.code -eq 'PHQ9' } | Select-Object -First 1
$gad7 = $scales | Where-Object { $_.code -eq 'GAD7' } | Select-Object -First 1
if (-not $phq9) { throw 'PHQ9 not found in /api/scales' }
if (-not $gad7) { throw 'GAD7 not found in /api/scales' }

Verify-ScaleSeed $studentHeaders $phq9 9 27
Verify-ScaleSeed $studentHeaders $gad7 7 21

$phqLow = Submit-ScaleCase $studentHeaders $phq9 @(0,0,0,0,0,0,0,0,0) 0 'LOW'
$phqMedium = Submit-ScaleCase $studentHeaders $phq9 @(1,1,1,1,1,0,0,0,0) 5 'MEDIUM'
$phqHigh = Submit-ScaleCase $studentHeaders $phq9 @(3,3,3,3,0,0,0,0,0) 12 'HIGH'
$gadLow = Submit-ScaleCase $studentHeaders $gad7 @(0,0,0,0,0,0,0) 0 'LOW'
$gadMedium = Submit-ScaleCase $studentHeaders $gad7 @(1,1,1,1,1,0,0) 5 'MEDIUM'
$gadHigh = Submit-ScaleCase $studentHeaders $gad7 @(3,3,3,3,0,0,0) 12 'HIGH'

[ordered]@{
    scaleCodes = @($scales.code)
    verificationCases = @(
        $phqLow,
        $phqMedium,
        $phqHigh,
        $gadLow,
        $gadMedium,
        $gadHigh
    )
    verifiedAt = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
} | ConvertTo-Json -Depth 6
