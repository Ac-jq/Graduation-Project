param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$StudentAccount = "20230001",
    [string]$DefaultPassword = "Jqpro@123",
    [string]$TempPassword = "Jqpro@123-temp"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $scriptDir "common.ps1")

Write-Step "Detect current usable password"
$studentLogin = Try-LoginJqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $DefaultPassword
$currentPassword = $DefaultPassword
if ($null -eq $studentLogin) {
    $studentLogin = Try-LoginJqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $TempPassword
    $currentPassword = $TempPassword
}
if ($null -eq $studentLogin) {
    throw "Cannot log in with either default or temp password for [$StudentAccount]."
}

if ($currentPassword -ne $DefaultPassword) {
    Write-Step "Password is in temp state, restore default first"
    $headers = New-AuthHeaders -Token $studentLogin.data.token
    $restoreResult = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/auth/change-password" -Method "POST" -Headers $headers -Body @{
        oldPassword     = $TempPassword
        newPassword     = $DefaultPassword
        confirmPassword = $DefaultPassword
    }
    Assert-ResultSuccess -Result $restoreResult -ActionName "restore default password"
    $studentLogin = Login-JqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $DefaultPassword
}

$studentHeaders = New-AuthHeaders -Token $studentLogin.data.token

Write-Step "Fetch current user"
$currentUser = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/auth/current-user" -Headers $studentHeaders
Assert-ResultSuccess -Result $currentUser -ActionName "current user"

Write-Step "Fetch student profile"
$profile = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/profile/me" -Headers $studentHeaders
Assert-ResultSuccess -Result $profile -ActionName "get profile"

Write-Step "Update allowed student profile fields"
$updatedProfile = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/profile/me" -Method "PUT" -Headers $studentHeaders -Body @{
    avatarUrl        = "https://example.com/avatar/phase1-student.png"
    college          = "Software College"
    grade            = "2023"
    gender           = "Male"
    phone            = "13800138000"
    emergencyContact = "Parent Zhang"
    emergencyPhone   = "13900139000"
}
Assert-ResultSuccess -Result $updatedProfile -ActionName "update profile"

Write-Step "Password roundtrip: default -> temp"
$changeToTemp = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/auth/change-password" -Method "POST" -Headers $studentHeaders -Body @{
    oldPassword     = $DefaultPassword
    newPassword     = $TempPassword
    confirmPassword = $TempPassword
}
Assert-ResultSuccess -Result $changeToTemp -ActionName "change to temp password"

Write-Step "Verify temp password login"
$tempLogin = Login-JqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $TempPassword
$tempHeaders = New-AuthHeaders -Token $tempLogin.data.token

Write-Step "Restore default password"
$changeBack = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/auth/change-password" -Method "POST" -Headers $tempHeaders -Body @{
    oldPassword     = $TempPassword
    newPassword     = $DefaultPassword
    confirmPassword = $DefaultPassword
}
Assert-ResultSuccess -Result $changeBack -ActionName "restore default password"

Write-Step "Log in again with default password and logout"
$finalLogin = Login-JqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $DefaultPassword
$finalHeaders = New-AuthHeaders -Token $finalLogin.data.token
$logoutResult = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/auth/logout" -Method "POST" -Headers $finalHeaders
Assert-ResultSuccess -Result $logoutResult -ActionName "logout"

Write-Success "Phase1 auth/profile script passed"
[pscustomobject]@{
    account           = $StudentAccount
    userId            = $currentUser.data.userId
    roleCode          = $currentUser.data.roleCode
    displayName       = $currentUser.data.displayName
    studentNo         = $currentUser.data.studentNo
    avatarUrl         = $updatedProfile.data.avatarUrl
    profilePhone      = $updatedProfile.data.phone
    counselorUserId   = $updatedProfile.data.counselorUserId
    passwordRoundtrip = "passed"
    logout            = "passed"
} | ConvertTo-Json -Depth 10
