param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$StudentAccount = "20230001",
    [string]$StudentPassword = "Jqpro@123",
    [string]$AdminAccount = "admin",
    [string]$AdminPassword = "Jqpro@123"
)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $scriptDir 'common.ps1')

Write-Step 'Student login'
$studentLogin = Login-JqPro -BaseUrl $BaseUrl -Account $StudentAccount -Password $StudentPassword
$studentHeaders = New-AuthHeaders -Token $studentLogin.data.token

Write-Step 'Admin login'
$adminLogin = Login-JqPro -BaseUrl $BaseUrl -Account $AdminAccount -Password $AdminPassword
$adminHeaders = New-AuthHeaders -Token $adminLogin.data.token

Write-Step 'Prepare resource category'
$categories = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/resource-categories' -Headers $adminHeaders
Assert-ResultSuccess -Result $categories -ActionName 'list resource categories'
$category = @($categories.data | Where-Object { $_.name -eq 'Stage3 Acceptance Category' } | Select-Object -First 1)
if (-not $category) {
    $category = (Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/resource-categories' -Method 'POST' -Headers $adminHeaders -Body @{
        name = 'Stage3 Acceptance Category'
        description = 'Acceptance category for automated resource governance'
        sortNo = 31
        status = 'ACTIVE'
    }).data
}

Write-Step 'Prepare resource tag'
$tags = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/resource-tags' -Headers $adminHeaders
Assert-ResultSuccess -Result $tags -ActionName 'list resource tags'
$tag = @($tags.data | Where-Object { $_.name -eq 'stage3-acceptance' } | Select-Object -First 1)
if (-not $tag) {
    $tag = (Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/resource-tags' -Method 'POST' -Headers $adminHeaders -Body @{
        name = 'stage3-acceptance'
        description = 'Acceptance tag for automated resource governance'
    }).data
}

$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$resourceTitle = "Phase3 Acceptance Resource $stamp"

Write-Step 'Create admin resource'
$createdResource = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/resources' -Method 'POST' -Headers $adminHeaders -Body @{
    title = $resourceTitle
    summaryText = 'Automated acceptance resource for stage 3.'
    resourceType = 'ARTICLE'
    contentUrl = "https://example.com/phase3/$stamp"
    categoryId = [int64]$category.categoryId
    tagIds = @([int64]$tag.tagId)
}
Assert-ResultSuccess -Result $createdResource -ActionName 'create admin resource'
$resourceId = [int64]$createdResource.data.resourceId

Write-Step 'Publish resource'
$publishResource = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/admin/resources/$resourceId/publish" -Method 'POST' -Headers $adminHeaders
Assert-ResultSuccess -Result $publishResource -ActionName 'publish resource'

Write-Step 'Student list published resources'
$resourceKeyword = [uri]::EscapeDataString($resourceTitle)
$studentResources = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/resources?keyword=$resourceKeyword" -Headers $studentHeaders
Assert-ResultSuccess -Result $studentResources -ActionName 'student list resources'
Assert-True -Condition (@($studentResources.data | Where-Object { $_.resourceId -eq $resourceId }).Count -ge 1) -Message 'Published resource is not visible to student'

Write-Step 'Student get resource detail and favorite roundtrip'
$resourceDetail = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/resources/$resourceId" -Headers $studentHeaders
Assert-ResultSuccess -Result $resourceDetail -ActionName 'student resource detail'
$addFavorite = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/favorites/$resourceId" -Method 'POST' -Headers $studentHeaders
Assert-ResultSuccess -Result $addFavorite -ActionName 'add favorite'
$favorites = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/student/favorites' -Headers $studentHeaders
Assert-ResultSuccess -Result $favorites -ActionName 'list favorites'
Assert-True -Condition (@($favorites.data | Where-Object { $_.resourceId -eq $resourceId }).Count -ge 1) -Message 'Favorite resource not found after add'
$removeFavorite = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/student/favorites/$resourceId" -Method 'DELETE' -Headers $studentHeaders
Assert-ResultSuccess -Result $removeFavorite -ActionName 'remove favorite'

Write-Step 'Admin statistics endpoints'
$overview = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/statistics/overview' -Headers $adminHeaders
$assessmentStats = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/statistics/assessments' -Headers $adminHeaders
$resourceStats = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/statistics/resources' -Headers $adminHeaders
$appointmentStats = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/statistics/appointments' -Headers $adminHeaders
$exportRows = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/statistics/export?dimension=college' -Headers $adminHeaders
Assert-ResultSuccess -Result $overview -ActionName 'overview stats'
Assert-ResultSuccess -Result $assessmentStats -ActionName 'assessment stats'
Assert-ResultSuccess -Result $resourceStats -ActionName 'resource stats'
Assert-ResultSuccess -Result $appointmentStats -ActionName 'appointment stats'
Assert-ResultSuccess -Result $exportRows -ActionName 'export stats'

Write-Step 'Admin AI task cancel flow'
$parseCancel = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/ai-tasks/parse' -Method 'POST' -Headers $adminHeaders -Body @{
    instruction = "offline resource `"$resourceTitle`""
}
Assert-ResultSuccess -Result $parseCancel -ActionName 'parse cancel task'
$cancelTaskId = [int64]$parseCancel.data.task.taskId
$cancelTask = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/admin/ai-tasks/$cancelTaskId/cancel" -Method 'POST' -Headers $adminHeaders
Assert-ResultSuccess -Result $cancelTask -ActionName 'cancel ai task'

Write-Step 'Admin AI task confirm offline and republish flow'
$parseOffline = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/ai-tasks/parse' -Method 'POST' -Headers $adminHeaders -Body @{
    instruction = "offline resource `"$resourceTitle`""
}
Assert-ResultSuccess -Result $parseOffline -ActionName 'parse offline task'
$offlineTaskId = [int64]$parseOffline.data.task.taskId
$confirmOffline = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/admin/ai-tasks/$offlineTaskId/confirm" -Method 'POST' -Headers $adminHeaders
Assert-ResultSuccess -Result $confirmOffline -ActionName 'confirm offline task'
$studentResourcesAfterOffline = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/resources?keyword=$resourceKeyword" -Headers $studentHeaders
Assert-ResultSuccess -Result $studentResourcesAfterOffline -ActionName 'student list resources after offline'
Assert-True -Condition (@($studentResourcesAfterOffline.data | Where-Object { $_.resourceId -eq $resourceId }).Count -eq 0) -Message 'Offline resource is still visible to student'

$parsePublish = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/ai-tasks/parse' -Method 'POST' -Headers $adminHeaders -Body @{
    instruction = "publish resource `"$resourceTitle`""
}
Assert-ResultSuccess -Result $parsePublish -ActionName 'parse publish task'
$publishTaskId = [int64]$parsePublish.data.task.taskId
$confirmPublish = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/admin/ai-tasks/$publishTaskId/confirm" -Method 'POST' -Headers $adminHeaders
Assert-ResultSuccess -Result $confirmPublish -ActionName 'confirm publish task'
$studentResourcesAfterPublish = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/resources?keyword=$resourceKeyword" -Headers $studentHeaders
Assert-ResultSuccess -Result $studentResourcesAfterPublish -ActionName 'student list resources after publish'
Assert-True -Condition (@($studentResourcesAfterPublish.data | Where-Object { $_.resourceId -eq $resourceId }).Count -ge 1) -Message 'Republished resource is not visible to student'

Write-Step 'Admin list tasks and audit logs'
$tasks = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/ai-tasks' -Headers $adminHeaders
$auditLogs = Invoke-JqProApi -BaseUrl $BaseUrl -Path '/api/admin/audit-logs?actionCode=ADMIN_AI_PARSE' -Headers $adminHeaders
Assert-ResultSuccess -Result $tasks -ActionName 'list ai tasks'
Assert-ResultSuccess -Result $auditLogs -ActionName 'list audit logs'

Write-Success 'Phase3 resource governance script passed'
[pscustomobject]@{
    categoryId = $category.categoryId
    tagId = $tag.tagId
    resourceId = $resourceId
    resourceTitle = $resourceTitle
    resourceVisibleBeforeOffline = $studentResources.data.Count
    resourceVisibleAfterOffline = $studentResourcesAfterOffline.data.Count
    resourceVisibleAfterPublish = $studentResourcesAfterPublish.data.Count
    overviewResourceCount = $overview.data.resourceCount
    overviewPublishedResourceCount = $overview.data.publishedResourceCount
    exportRowCount = $exportRows.data.Count
    cancelTaskId = $cancelTaskId
    offlineTaskId = $offlineTaskId
    publishTaskId = $publishTaskId
    auditLogCount = $auditLogs.data.Count
} | ConvertTo-Json -Depth 10
