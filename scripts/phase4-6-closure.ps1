param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$StudentAccount = "20230001",
    [string]$StudentPassword = "Jqpro@123",
    [string]$CounselorAccount = "teacher01",
    [string]$CounselorPassword = "Jqpro@123",
    [string]$AdminAccount = "admin",
    [string]$AdminPassword = "Jqpro@123"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $scriptDir 'common.ps1')

Write-Step 'Student login'
$studentLogin = Login-JqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $StudentPassword
$studentHeaders = New-AuthHeaders -Token $studentLogin.data.token
$studentUserId = [int64]$studentLogin.data.userId

Write-Step 'Counselor login'
$counselorLogin = Login-JqPro -BaseUrl $BaseUrl -Account $CounselorAccount -Password $CounselorPassword
$counselorHeaders = New-AuthHeaders -Token $counselorLogin.data.token

Write-Step 'Admin login'
$adminLogin = Login-JqPro -BaseUrl $BaseUrl -Account $AdminAccount -Password $AdminPassword
$adminHeaders = New-AuthHeaders -Token $adminLogin.data.token

Write-Step 'Profile read and update'
$profile = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/student/profile/me' -Headers $studentHeaders
Assert-ResultSuccess -Result $profile -ActionName 'get student profile'
$updateProfile = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/student/profile/me' -Method 'PUT' -Headers $studentHeaders -Body @{
    college = 'Software College'
    grade = '2023'
    gender = 'Male'
    phone = '13800138079'
    emergencyContact = 'Parent Zhang'
    emergencyPhone = '13900139079'
}
Assert-ResultSuccess -Result $updateProfile -ActionName 'update student profile'

Write-Step 'Bound counselor student list'
$counselorStudents = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/counselor/students' -Headers $counselorHeaders
Assert-ResultSuccess -Result $counselorStudents -ActionName 'list counselor students'
Assert-True -Condition (@($counselorStudents.data | Where-Object { $_.studentUserId -eq $studentUserId }).Count -ge 1) -Message 'Bound student missing from counselor student list'

Write-Step 'Student complete one assessment and get report recommendation'
$scales = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/scales' -Headers $studentHeaders
Assert-ResultSuccess -Result $scales -ActionName 'list scales'
if ($scales.data.Count -lt 1) {
    throw 'No active scales available.'
}
$scaleId = [int64]$scales.data[0].id
$scaleDetail = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/$scaleId" -Headers $studentHeaders
Assert-ResultSuccess -Result $scaleDetail -ActionName 'scale detail'
$draftSession = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/$scaleId/sessions/draft" -Method 'POST' -Headers $studentHeaders
Assert-ResultSuccess -Result $draftSession -ActionName 'create draft session'
$sessionId = [int64]$draftSession.data.sessionId
$questionPage = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/sessions/$sessionId/questions?pageNum=1&pageSize=50" -Headers $studentHeaders
Assert-ResultSuccess -Result $questionPage -ActionName 'load scale questions'
$answers = @()
foreach ($question in $questionPage.data.records) {
    $selected = @($question.options | Sort-Object score, id)[-1]
    $answers += @{
        questionId = [int64]$question.questionId
        optionId = [int64]$selected.id
    }
}
$saveAnswers = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/sessions/$sessionId/answers" -Method 'PUT' -Headers $studentHeaders -Body @{ answers = $answers }
Assert-ResultSuccess -Result $saveAnswers -ActionName 'save scale answers'
$submitScale = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/scales/sessions/$sessionId/submit" -Method 'POST' -Headers $studentHeaders
Assert-ResultSuccess -Result $submitScale -ActionName 'submit scale'
$reportId = [int64]$submitScale.data.reportId
$studentReports = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/student/reports' -Headers $studentHeaders
$studentReportDetail = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/reports/$reportId" -Headers $studentHeaders
Assert-ResultSuccess -Result $studentReports -ActionName 'student report list'
Assert-ResultSuccess -Result $studentReportDetail -ActionName 'student report detail'
Assert-True -Condition ([bool]$studentReportDetail.data.recommendAppointment) -Message 'Report detail should recommend appointment for high-score acceptance case'
Assert-True -Condition (@($studentReportDetail.data.recommendedResources).Count -ge 1) -Message 'Report detail should contain recommended resources'

Write-Step 'Counselor read student reports'
$counselorReports = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/counselor/students/$studentUserId/reports" -Headers $counselorHeaders
$counselorReportDetail = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/counselor/students/$studentUserId/reports/$reportId" -Headers $counselorHeaders
Assert-ResultSuccess -Result $counselorReports -ActionName 'counselor report list'
Assert-ResultSuccess -Result $counselorReportDetail -ActionName 'counselor report detail'

Write-Step 'Admin user management roundtrip'
$users = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/users' -Headers $adminHeaders
Assert-ResultSuccess -Result $users -ActionName 'list admin users'
$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$newCounselor = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/users/counselors' -Method 'POST' -Headers $adminHeaders -Body @{
    account = "counselor$stamp"
    displayName = 'Stage46 Counselor'
    realName = 'Stage46 Counselor'
    counselorNo = "C$stamp"
}
Assert-ResultSuccess -Result $newCounselor -ActionName 'create counselor'
$newCounselorId = [int64]$newCounselor.data.userId
$disableUser = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/admin/users/$newCounselorId/disable" -Method 'POST' -Headers $adminHeaders
$enableUser = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/admin/users/$newCounselorId/enable" -Method 'POST' -Headers $adminHeaders
$resetPassword = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/admin/users/$newCounselorId/reset-password" -Method 'POST' -Headers $adminHeaders
Assert-ResultSuccess -Result $disableUser -ActionName 'disable counselor'
Assert-ResultSuccess -Result $enableUser -ActionName 'enable counselor'
Assert-ResultSuccess -Result $resetPassword -ActionName 'reset counselor password'
$createdCounselorLogin = Login-JqPro -BaseUrl $BaseUrl -Account $newCounselor.data.account -Password 'Jqpro@123'

Write-Step 'Admin scale management roundtrip'
$scaleCode = "ACC_AUTO_$stamp"
$scalePayload = @{
    code = $scaleCode
    name = "Stage46 Acceptance Scale $stamp"
    description = 'Automated acceptance scale'
    introduction = 'Used by automated acceptance script'
    pageSize = 2
    lowThreshold = 0
    mediumThreshold = 2
    highThreshold = 4
    questions = @(
        @{
            questionNo = 1
            content = 'Recently I feel pressure on study tasks.'
            requiredFlag = 1
            options = @(
                @{ optionCode = 'A'; content = 'Rarely'; score = 0; sortNo = 1 },
                @{ optionCode = 'B'; content = 'Often'; score = 2; sortNo = 2 }
            )
        },
        @{
            questionNo = 2
            content = 'I need psychological support to keep stable.'
            requiredFlag = 1
            options = @(
                @{ optionCode = 'A'; content = 'No'; score = 0; sortNo = 1 },
                @{ optionCode = 'B'; content = 'Yes'; score = 2; sortNo = 2 }
            )
        }
    )
}
$createScale = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/scales' -Method 'POST' -Headers $adminHeaders -Body $scalePayload
Assert-ResultSuccess -Result $createScale -ActionName 'create admin scale'
$adminScaleId = [int64]$createScale.data.scaleId
$activateScale = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/admin/scales/$adminScaleId/activate" -Method 'POST' -Headers $adminHeaders
Assert-ResultSuccess -Result $activateScale -ActionName 'activate admin scale'
$studentScalesAfterActivate = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/scales' -Headers $studentHeaders
Assert-ResultSuccess -Result $studentScalesAfterActivate -ActionName 'student list scales after activate'
Assert-True -Condition (@($studentScalesAfterActivate.data | Where-Object { $_.code -eq $scaleCode }).Count -ge 1) -Message 'Activated admin scale is not visible to student'
$deactivateScale = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/admin/scales/$adminScaleId/deactivate" -Method 'POST' -Headers $adminHeaders
Assert-ResultSuccess -Result $deactivateScale -ActionName 'deactivate admin scale'
$studentScalesAfterDeactivate = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/scales' -Headers $studentHeaders
Assert-ResultSuccess -Result $studentScalesAfterDeactivate -ActionName 'student list scales after deactivate'
Assert-True -Condition (@($studentScalesAfterDeactivate.data | Where-Object { $_.code -eq $scaleCode }).Count -eq 0) -Message 'Deactivated admin scale is still visible to student'

Write-Success 'Phase4-6 closure script passed'
[pscustomobject]@{
    studentUserId = $studentUserId
    updatedPhone = $updateProfile.data.phone
    boundStudentCount = $counselorStudents.data.Count
    scaleId = $scaleId
    sessionId = $sessionId
    reportId = $reportId
    reportLevelCode = $studentReportDetail.data.levelCode
    recommendedResourceCount = @($studentReportDetail.data.recommendedResources).Count
    recommendAppointment = [bool]$studentReportDetail.data.recommendAppointment
    createdCounselorId = $newCounselorId
    createdCounselorAccount = $newCounselor.data.account
    createdCounselorLogin = $createdCounselorLogin.data.account
    adminScaleId = $adminScaleId
    adminScaleCode = $scaleCode
} | ConvertTo-Json -Depth 10
