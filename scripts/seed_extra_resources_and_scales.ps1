$ErrorActionPreference = 'Stop'

$baseUrl = 'http://127.0.0.1:8080/api'
$adminAccount = 'admin'
$adminPassword = 'Jqpro@123'
$staticBaseUrl = 'http://127.0.0.1:8080/assets/resources'

function Invoke-JsonApi {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [hashtable]$Headers,
        $Body
    )

    $params = @{
        Uri         = $Uri
        Method      = $Method
        ContentType = 'application/json; charset=utf-8'
    }

    if ($Headers) {
        $params.Headers = $Headers
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 12 -Compress)
    }

    Invoke-RestMethod @params
}

function Get-AdminHeaders {
    $response = Invoke-JsonApi -Method Post -Uri "$baseUrl/auth/login" -Body @{
        account  = $adminAccount
        password = $adminPassword
    }
    @{ Authorization = $response.data.token }
}

function Ensure-Category {
    param(
        [hashtable]$Headers,
        [string]$Name,
        [string]$Description,
        [int]$SortNo
    )

    $existing = (Invoke-RestMethod -Uri "$baseUrl/admin/resource-categories" -Headers $Headers).data |
        Where-Object { $_.name -eq $Name } |
        Select-Object -First 1

    if ($existing) {
        return $existing
    }

    (Invoke-JsonApi -Method Post -Uri "$baseUrl/admin/resource-categories" -Headers $Headers -Body @{
        name        = $Name
        description = $Description
        sortNo      = $SortNo
        status      = 'ACTIVE'
    }).data
}

function Ensure-Tag {
    param(
        [hashtable]$Headers,
        [string]$Name,
        [string]$Description
    )

    $existing = (Invoke-RestMethod -Uri "$baseUrl/admin/resource-tags" -Headers $Headers).data |
        Where-Object { $_.name -eq $Name } |
        Select-Object -First 1

    if ($existing) {
        return $existing
    }

    (Invoke-JsonApi -Method Post -Uri "$baseUrl/admin/resource-tags" -Headers $Headers -Body @{
        name        = $Name
        description = $Description
    }).data
}

function New-StandardOptions {
    @(
        @{ optionCode = 'A'; content = '从不';      score = 0; sortNo = 1 },
        @{ optionCode = 'B'; content = '偶尔';      score = 1; sortNo = 2 },
        @{ optionCode = 'C'; content = '经常';      score = 2; sortNo = 3 },
        @{ optionCode = 'D'; content = '几乎每天'; score = 3; sortNo = 4 }
    )
}

function New-ScaleQuestion {
    param(
        [int]$No,
        [string]$Content
    )

    @{
        questionNo   = $No
        content      = $Content
        requiredFlag = 1
        options      = New-StandardOptions
    }
}

function Ensure-Scale {
    param(
        [hashtable]$Headers,
        [string]$Code,
        [string]$Name,
        [string]$Description,
        [string]$Introduction,
        [int]$PageSize,
        [int]$LowThreshold,
        [int]$MediumThreshold,
        [int]$HighThreshold,
        [array]$Questions
    )

    $existing = (Invoke-RestMethod -Uri "$baseUrl/admin/scales" -Headers $Headers).data |
        Where-Object { $_.code -eq $Code } |
        Select-Object -First 1

    $payload = @{
        code            = $Code
        name            = $Name
        description     = $Description
        introduction    = $Introduction
        pageSize        = $PageSize
        lowThreshold    = $LowThreshold
        mediumThreshold = $MediumThreshold
        highThreshold   = $HighThreshold
        questions       = $Questions
    }

    if ($existing) {
        $scale = (Invoke-JsonApi -Method Put -Uri "$baseUrl/admin/scales/$($existing.scaleId)" -Headers $Headers -Body $payload).data
    } else {
        $scale = (Invoke-JsonApi -Method Post -Uri "$baseUrl/admin/scales" -Headers $Headers -Body $payload).data
    }

    if ($scale.status -ne 'ACTIVE') {
        $scale = (Invoke-JsonApi -Method Post -Uri "$baseUrl/admin/scales/$($scale.scaleId)/activate" -Headers $Headers).data
    }

    $scale
}

function Ensure-Resource {
    param(
        [hashtable]$Headers,
        [string]$Title,
        [string]$SummaryText,
        [string]$ResourceType,
        [string]$ContentUrl,
        [string]$CoverUrl,
        [long]$CategoryId,
        [long[]]$TagIds
    )

    $existing = (Invoke-RestMethod -Uri "$baseUrl/admin/resources" -Headers $Headers).data |
        Where-Object { $_.title -eq $Title } |
        Select-Object -First 1

    $payload = @{
        title        = $Title
        summaryText  = $SummaryText
        resourceType = $ResourceType
        contentUrl   = $ContentUrl
        coverUrl     = $CoverUrl
        categoryId   = $CategoryId
        tagIds       = $TagIds
    }

    if ($existing) {
        $resource = (Invoke-JsonApi -Method Put -Uri "$baseUrl/admin/resources/$($existing.resourceId)" -Headers $Headers -Body $payload).data
    } else {
        $resource = (Invoke-JsonApi -Method Post -Uri "$baseUrl/admin/resources" -Headers $Headers -Body $payload).data
    }

    if ($resource.status -ne 'PUBLISHED') {
        $resource = (Invoke-JsonApi -Method Post -Uri "$baseUrl/admin/resources/$($resource.resourceId)/publish" -Headers $Headers).data
    }

    $resource
}

$headers = Get-AdminHeaders

$visualCategory = Ensure-Category -Headers $headers -Name '图像安抚资源' -Description '以图片为主的轻量安抚资源。' -SortNo 11
$audioCategory = Ensure-Category -Headers $headers -Name '呼吸音频集' -Description '用于呼吸放松和低刺激陪伴聆听的音频资源。' -SortNo 12
$rhythmCategory = Ensure-Category -Headers $headers -Name '节律观察短片' -Description '帮助恢复学习与生活节奏感的校园短片。' -SortNo 13

$tagVisual = Ensure-Tag -Headers $headers -Name '图卡安抚' -Description '适合短时停留和视觉落地练习的图像标签。'
$tagBreath = Ensure-Tag -Headers $headers -Name '轻缓呼吸' -Description '适合紧张、烦躁或切换状态时的短呼吸引导。'
$tagSleep = Ensure-Tag -Headers $headers -Name '睡眠缓冲' -Description '适合睡前放松与过渡的低刺激陪伴内容。'
$tagRhythm = Ensure-Tag -Headers $headers -Name '节律重整' -Description '帮助重新找回学习、休息和专注节奏。'

$sleepScale = Ensure-Scale -Headers $headers -Code 'SLEEP6' -Name '睡眠恢复感知量表' `
    -Description '用于快速了解近一周入睡、夜间醒来与晨起恢复感受的辅助量表。' `
    -Introduction '请根据最近一周的真实状态作答。本量表仅用于心理状态辅助评估，不作为医学诊断依据。' `
    -PageSize 3 -LowThreshold 0 -MediumThreshold 5 -HighThreshold 10 -Questions @(
        (New-ScaleQuestion -No 1 -Content '最近一周，你是否经常需要很久才能入睡？'),
        (New-ScaleQuestion -No 2 -Content '最近一周，你是否在夜间反复醒来且难以再次入睡？'),
        (New-ScaleQuestion -No 3 -Content '最近一周，你早晨醒来时是否仍觉得疲惫或没有恢复感？'),
        (New-ScaleQuestion -No 4 -Content '最近一周，你是否因为脑中想法停不下来而难以放松入睡？'),
        (New-ScaleQuestion -No 5 -Content '最近一周，你是否因为担心第二天的事情而让夜晚更紧绷？'),
        (New-ScaleQuestion -No 6 -Content '最近一周，睡眠困扰是否已经影响到你的白天专注或情绪状态？')
    )

$stressScale = Ensure-Scale -Headers $headers -Code 'STRESS8' -Name '校园压力脉搏量表' `
    -Description '用于识别近期学习、人际与节奏安排带来的压力负荷变化。' `
    -Introduction '请根据最近两周的真实状态作答。本量表仅用于心理状态辅助评估，不作为医学诊断依据。' `
    -PageSize 4 -LowThreshold 0 -MediumThreshold 6 -HighThreshold 12 -Questions @(
        (New-ScaleQuestion -No 1 -Content '最近两周，你是否觉得任务不断堆积，很难理清轻重缓急？'),
        (New-ScaleQuestion -No 2 -Content '最近两周，学习要求是否让你持续处在紧绷状态？'),
        (New-ScaleQuestion -No 3 -Content '最近两周，与同学、老师或家人的互动是否让你感到明显消耗？'),
        (New-ScaleQuestion -No 4 -Content '最近两周，你是否总觉得自己在赶时间，几乎没有真正休息？'),
        (New-ScaleQuestion -No 5 -Content '最近两周，压力是否让你出现拖延、逃避或动力下降？'),
        (New-ScaleQuestion -No 6 -Content '最近两周，你是否担心自己难以应对接下来的学习或生活安排？'),
        (New-ScaleQuestion -No 7 -Content '最近两周，小事是否也容易放大你的情绪波动，打断你的专注？'),
        (New-ScaleQuestion -No 8 -Content '最近两周，你是否感觉自己需要支持，却又不容易开口求助？')
    )

$resources = @(
    @{
        Title        = '晨光静观图卡'
        SummaryText  = '适合在思绪嘈杂时短暂停留，通过画面让注意力慢慢落回当下。'
        ResourceType = 'IMAGE'
        ContentUrl   = "$staticBaseUrl/images/breathing-cover.jpg"
        CoverUrl     = "$staticBaseUrl/images/breathing-cover.jpg"
        CategoryId   = [long]$visualCategory.categoryId
        TagIds       = @([long]$tagVisual.tagId, [long]$tagBreath.tagId)
    },
    @{
        Title        = '夜色整理图卡'
        SummaryText  = '适合睡前从任务状态切换出来，用更柔和的节奏结束一天。'
        ResourceType = 'IMAGE'
        ContentUrl   = "$staticBaseUrl/images/sleep-cover.jpg"
        CoverUrl     = "$staticBaseUrl/images/sleep-cover.jpg"
        CategoryId   = [long]$visualCategory.categoryId
        TagIds       = @([long]$tagVisual.tagId, [long]$tagSleep.tagId)
    },
    @{
        Title        = '专注前的呼吸引导'
        SummaryText  = '适合上课前、自习前或情绪绷紧时，先用一分钟把呼吸稳下来。'
        ResourceType = 'AUDIO'
        ContentUrl   = "$staticBaseUrl/audio/pause-breathing-loop.mp3"
        CoverUrl     = "$staticBaseUrl/images/breathing-cover.jpg"
        CategoryId   = [long]$audioCategory.categoryId
        TagIds       = @([long]$tagBreath.tagId, [long]$tagRhythm.tagId)
    },
    @{
        Title        = '睡前白噪音片段'
        SummaryText  = '低刺激的陪伴音频，帮助你在睡前慢慢放掉白天的紧张感。'
        ResourceType = 'AUDIO'
        ContentUrl   = "$staticBaseUrl/audio/pause-breathing-loop.mp3"
        CoverUrl     = "$staticBaseUrl/images/sleep-cover.jpg"
        CategoryId   = [long]$audioCategory.categoryId
        TagIds       = @([long]$tagSleep.tagId)
    },
    @{
        Title        = '校园步调观察短片'
        SummaryText  = '用缓慢镜头重新感受校园节奏，适合考试周或事务堆积时短暂抽离。'
        ResourceType = 'VIDEO'
        ContentUrl   = "$staticBaseUrl/videos/campus-rhythm.mp4"
        CoverUrl     = "$staticBaseUrl/images/study-cover.jpg"
        CategoryId   = [long]$rhythmCategory.categoryId
        TagIds       = @([long]$tagRhythm.tagId)
    }
)

$createdResources = foreach ($resource in $resources) {
    Ensure-Resource -Headers $headers @resource
}

[PSCustomObject]@{
    scales = @(
        [PSCustomObject]@{ code = $sleepScale.code; scaleId = $sleepScale.scaleId; status = $sleepScale.status; totalQuestions = $sleepScale.totalQuestions }
        [PSCustomObject]@{ code = $stressScale.code; scaleId = $stressScale.scaleId; status = $stressScale.status; totalQuestions = $stressScale.totalQuestions }
    )
    resources = $createdResources | Select-Object resourceId, title, resourceType, status, contentUrl
} | ConvertTo-Json -Depth 8
