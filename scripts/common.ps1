Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Success {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Host "[OK] $Message" -ForegroundColor Green
}

function Convert-ToPrettyJson {
    param(
        [Parameter(Mandatory = $true)]
        [object]$InputObject
    )

    return ($InputObject | ConvertTo-Json -Depth 10)
}

function Assert-ResultSuccess {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Result,

        [Parameter(Mandatory = $true)]
        [string]$ActionName
    )

    if ($null -eq $Result) {
        throw "$ActionName returned null."
    }
    if ($null -eq $Result.code -or $Result.code -ne 200) {
        throw "$ActionName failed: $(Convert-ToPrettyJson $Result)"
    }
}

function Invoke-JqProApi {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,

        [Parameter(Mandatory = $true)]
        [string]$Path,

        [ValidateSet("GET", "POST", "PUT", "DELETE", "PATCH")]
        [string]$Method = "GET",

        [hashtable]$Headers = @{},

        [object]$Body
    )

    $uri = $BaseUrl.TrimEnd("/") + $Path
    $invokeParams = @{
        Method     = $Method
        Uri        = $uri
        Headers    = $Headers
        TimeoutSec = 30
    }

    if ($PSBoundParameters.ContainsKey("Body") -and $null -ne $Body) {
        $invokeParams["ContentType"] = "application/json"
        $invokeParams["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }

    return Invoke-RestMethod @invokeParams
}

function Invoke-JqProWebRequest {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,

        [Parameter(Mandatory = $true)]
        [string]$Path,

        [ValidateSet("GET", "POST", "PUT", "DELETE", "PATCH")]
        [string]$Method = "GET",

        [hashtable]$Headers = @{},

        [object]$Body
    )

    $uri = $BaseUrl.TrimEnd("/") + $Path
    $invokeParams = @{
        Method     = $Method
        Uri        = $uri
        Headers    = $Headers
        TimeoutSec = 30
    }

    if ($PSBoundParameters.ContainsKey("Body") -and $null -ne $Body) {
        $invokeParams["ContentType"] = "application/json"
        $invokeParams["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $response = Invoke-WebRequest @invokeParams
        return [pscustomobject]@{
            StatusCode = [int]$response.StatusCode
            Content = $response.Content
        }
    } catch {
        if ($_.Exception.Response) {
            $httpResponse = $_.Exception.Response
            $reader = New-Object System.IO.StreamReader($httpResponse.GetResponseStream())
            $content = $reader.ReadToEnd()
            return [pscustomobject]@{
                StatusCode = [int]$httpResponse.StatusCode
                Content = $content
            }
        }
        throw
    }
}

function Assert-HttpStatus {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Response,

        [Parameter(Mandatory = $true)]
        [int[]]$ExpectedStatusCodes,

        [Parameter(Mandatory = $true)]
        [string]$ActionName
    )

    if ($Response.StatusCode -notin $ExpectedStatusCodes) {
        throw "$ActionName failed, expected [$($ExpectedStatusCodes -join ',')], actual [$($Response.StatusCode)] body: $($Response.Content)"
    }
}

function Assert-True {
    param(
        [Parameter(Mandatory = $true)]
        [bool]$Condition,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function New-AuthHeaders {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Token
    )

    return @{
        Authorization = $Token
    }
}

function Login-JqPro {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,

        [Parameter(Mandatory = $true)]
        [string]$Account,

        [Parameter(Mandatory = $true)]
        [string]$Password
    )

    $result = Invoke-JqProApi -BaseUrl $BaseUrl -Path "/api/auth/login" -Method "POST" -Body @{
        account  = $Account
        password = $Password
    }
    Assert-ResultSuccess -Result $result -ActionName "login[$Account]"
    return $result
}

function Try-LoginJqPro {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,

        [Parameter(Mandatory = $true)]
        [string]$Account,

        [Parameter(Mandatory = $true)]
        [string]$Password
    )

    try {
        return (Login-JqPro -BaseUrl $BaseUrl -Account $Account -Password $Password)
    } catch {
        return $null
    }
}
