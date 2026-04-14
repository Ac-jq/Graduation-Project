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
        @{ optionCode = 'A'; content = 'Never';      score = 0; sortNo = 1 },
        @{ optionCode = 'B'; content = 'Sometimes';  score = 1; sortNo = 2 },
        @{ optionCode = 'C'; content = 'Often';      score = 2; sortNo = 3 },
        @{ optionCode = 'D'; content = 'Almost daily'; score = 3; sortNo = 4 }
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

$visualCategory = Ensure-Category -Headers $headers -Name 'Visual Grounding' -Description 'Image-based calming resources.' -SortNo 11
$audioCategory = Ensure-Category -Headers $headers -Name 'Audio Relief' -Description 'Audio-guided breathing and low-stimulus listening resources.' -SortNo 12
$rhythmCategory = Ensure-Category -Headers $headers -Name 'Rhythm Clips' -Description 'Short campus and pacing videos for rhythm recovery.' -SortNo 13

$tagVisual = Ensure-Tag -Headers $headers -Name 'visual-grounding' -Description 'Short visual pause and sensory grounding.'
$tagBreath = Ensure-Tag -Headers $headers -Name 'light-breathing' -Description 'Short breathing reset for tension and overload.'
$tagSleep = Ensure-Tag -Headers $headers -Name 'sleep-buffer' -Description 'Low-stimulus support for wind-down and sleep transition.'
$tagRhythm = Ensure-Tag -Headers $headers -Name 'rhythm-reset' -Description 'Helps restore pacing and focus rhythm.'

$sleepScale = Ensure-Scale -Headers $headers -Code 'SLEEP6' -Name 'Sleep Recovery Scale' `
    -Description 'A short self-check for sleep onset, night waking and morning recovery.' `
    -Introduction 'Answer based on your recent week. This scale is for supportive self-observation only and is not a medical diagnosis.' `
    -PageSize 3 -LowThreshold 0 -MediumThreshold 5 -HighThreshold 10 -Questions @(
        (New-ScaleQuestion -No 1 -Content 'In the past week, did you often take a long time to fall asleep?'),
        (New-ScaleQuestion -No 2 -Content 'In the past week, did you wake up repeatedly at night and struggle to fall asleep again?'),
        (New-ScaleQuestion -No 3 -Content 'In the past week, did you wake up feeling unrefreshed or drained?'),
        (New-ScaleQuestion -No 4 -Content 'In the past week, did racing thoughts make it hard to relax before sleep?'),
        (New-ScaleQuestion -No 5 -Content 'In the past week, did worry about the next day make your nights more tense?'),
        (New-ScaleQuestion -No 6 -Content 'In the past week, did sleep trouble affect your daytime focus or mood?')
    )

$stressScale = Ensure-Scale -Headers $headers -Code 'STRESS8' -Name 'Campus Stress Pulse Scale' `
    -Description 'A short self-check for recent pressure load from study, pacing and interpersonal demands.' `
    -Introduction 'Answer based on your recent two weeks. This scale is for supportive self-observation only and is not a medical diagnosis.' `
    -PageSize 4 -LowThreshold 0 -MediumThreshold 6 -HighThreshold 12 -Questions @(
        (New-ScaleQuestion -No 1 -Content 'In the past two weeks, did tasks feel piled up and hard to prioritize?'),
        (New-ScaleQuestion -No 2 -Content 'In the past two weeks, did study demands keep you in a tense state?'),
        (New-ScaleQuestion -No 3 -Content 'In the past two weeks, did interactions with classmates, teachers or family feel draining?'),
        (New-ScaleQuestion -No 4 -Content 'In the past two weeks, did you feel constantly rushed and unable to rest?'),
        (New-ScaleQuestion -No 5 -Content 'In the past two weeks, did pressure lead to avoidance, delay or loss of motivation?'),
        (New-ScaleQuestion -No 6 -Content 'In the past two weeks, did you worry that you could not handle the next stretch of study or life?'),
        (New-ScaleQuestion -No 7 -Content 'In the past two weeks, did small events easily amplify your mood or interrupt your focus?'),
        (New-ScaleQuestion -No 8 -Content 'In the past two weeks, did you feel you needed support but found it hard to ask?')
    )

$resources = @(
    @{
        Title        = 'Morning Grounding Card'
        SummaryText  = 'A quiet visual card for short grounding pauses when the mind feels noisy.'
        ResourceType = 'IMAGE'
        ContentUrl   = "$staticBaseUrl/images/breathing-cover.jpg"
        CoverUrl     = "$staticBaseUrl/images/breathing-cover.jpg"
        CategoryId   = [long]$visualCategory.categoryId
        TagIds       = @([long]$tagVisual.tagId, [long]$tagBreath.tagId)
    },
    @{
        Title        = 'Night Reset Card'
        SummaryText  = 'A low-stimulus image for closing the day and shifting out of task mode.'
        ResourceType = 'IMAGE'
        ContentUrl   = "$staticBaseUrl/images/sleep-cover.jpg"
        CoverUrl     = "$staticBaseUrl/images/sleep-cover.jpg"
        CategoryId   = [long]$visualCategory.categoryId
        TagIds       = @([long]$tagVisual.tagId, [long]$tagSleep.tagId)
    },
    @{
        Title        = 'Focus Breathing Prompt'
        SummaryText  = 'A very short breathing audio for pre-class, pre-study, or tension reset moments.'
        ResourceType = 'AUDIO'
        ContentUrl   = "$staticBaseUrl/audio/pause-breathing-loop.mp3"
        CoverUrl     = "$staticBaseUrl/images/breathing-cover.jpg"
        CategoryId   = [long]$audioCategory.categoryId
        TagIds       = @([long]$tagBreath.tagId, [long]$tagRhythm.tagId)
    },
    @{
        Title        = 'Sleep Buffer Audio'
        SummaryText  = 'A low-stimulus listening clip for winding down before sleep.'
        ResourceType = 'AUDIO'
        ContentUrl   = "$staticBaseUrl/audio/pause-breathing-loop.mp3"
        CoverUrl     = "$staticBaseUrl/images/sleep-cover.jpg"
        CategoryId   = [long]$audioCategory.categoryId
        TagIds       = @([long]$tagSleep.tagId)
    },
    @{
        Title        = 'Campus Rhythm Clip'
        SummaryText  = 'A short campus pacing video for exam weeks and overloaded days.'
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
