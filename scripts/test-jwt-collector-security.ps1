
# QuickCash Module 24 - JWT Security Tests
# Windows PowerShell 5.1
# Read-only HTTP tests: no payments are created.

Add-Type -AssemblyName System.Net.Http
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"

function Read-Jwt([string]$label) {
    $secure = Read-Host $label -AsSecureString

    $token = [System.Net.NetworkCredential]::new(
        "",
        $secure
    ).Password.Trim()

    if ($token.StartsWith("Bearer ")) {
        $token = $token.Substring(7).Trim()
    }

    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "A JWT is required."
    }

    return $token
}

$loanA = [int](Read-Host "Enter Collector A's assigned loan ID")
$loanB = [int](Read-Host "Enter Collector B's assigned loan ID")

if ($loanA -eq $loanB) {
    throw "Use two different loans."
}

$tokenA = Read-Jwt "Enter Collector A JWT"
$tokenB = Read-Jwt "Enter Collector B JWT"
$managerToken = Read-Jwt "Enter Manager JWT"

$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(30)

function Get-Status(
    [string]$path,
    [string]$token
) {
    $url = "$baseUrl$path"

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::Get,
        $url
    )

    try {
        if (-not [string]::IsNullOrWhiteSpace($token)) {
            $request.Headers.Authorization =
                [System.Net.Http.Headers.AuthenticationHeaderValue]::new(
                    "Bearer", $token
                )
        }

        $response = $client.SendAsync($request).
            GetAwaiter().GetResult()

        try {
            return [int]$response.StatusCode
        }
        finally {
            $response.Dispose()
        }
    }
    finally {
        $request.Dispose()
    }
}

function Assert-Status(
    [string]$name,
    [int]$actual,
    [int]$expected
) {
    if ($actual -eq $expected) {
        Write-Host "PASS - $name - HTTP $actual"
    }
    else {
        Write-Host "FAIL - $name - HTTP $actual (expected $expected)"
        throw "$name failed"
    }
}

function Assert-Denied(
    [string]$name,
    [int]$actual
) {
    if ($actual -eq 401 -or $actual -eq 403) {
        Write-Host "PASS - $name - HTTP $actual"
    }
    else {
        Write-Host "FAIL - $name - HTTP $actual"
        throw "$name failed: expected 401 or 403"
    }
}

try {
    Write-Host ""
    Write-Host "QuickCash Module 24 Security Tests"
    Write-Host ""

    # Check that test accounts are configured correctly.
    Assert-Status "Collector A own loan" (
        Get-Status "/api/collector/loans/$loanA" $tokenA
    ) 200

    Assert-Status "Collector B own loan" (
        Get-Status "/api/collector/loans/$loanB" $tokenB
    ) 200

    # Cross-collector isolation.
    Assert-Status "A cannot view B loan" (
        Get-Status "/api/collector/loans/$loanB" $tokenA
    ) 404

    Assert-Status "B cannot view A loan" (
        Get-Status "/api/collector/loans/$loanA" $tokenB
    ) 404

    Assert-Status "A cannot view B payments" (
        Get-Status "/api/collector/payments/loan/$loanB" $tokenA
    ) 404

    Assert-Status "B cannot view A follow-ups" (
        Get-Status "/api/collector/loans/$loanA/follow-ups" $tokenB
    ) 404

    # Role-based security.
    Assert-Status "Collector cannot access manager API" (
        Get-Status "/api/manager/loans/$loanA/follow-ups" $tokenA
    ) 403

    Assert-Status "Manager cannot access collector API" (
        Get-Status "/api/collector/loans/$loanA" $managerToken
    ) 403

    Assert-Status "Manager can access manager API" (
        Get-Status "/api/manager/loans/$loanA/follow-ups" $managerToken
    ) 200

    # Authentication tests.
    Assert-Denied "Missing JWT" (
        Get-Status "/api/collector/loans/$loanA" ""
    )

    Assert-Denied "Invalid JWT" (
        Get-Status "/api/collector/loans/$loanA" "invalid.jwt.token"
    )

    Write-Host ""
    Write-Host "================================"
    Write-Host "MODULE 24: ALL 11 HTTP CHECKS PASSED"
    Write-Host "================================"
}
finally {
    $client.Dispose()
    $tokenA = $null
    $tokenB = $null
    $managerToken = $null
}
