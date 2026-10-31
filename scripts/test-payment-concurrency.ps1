
# ============================================
# QuickCash - Module 23
# Payment Concurrency and Idempotency Tests
# Windows PowerShell 5.1 compatible
#
# WARNING: Creates REAL development payments.
# ============================================

Add-Type -AssemblyName System.Net.Http

$ErrorActionPreference = "Stop"

# CHANGE THESE IF YOUR TEST LOAN IDs ARE DIFFERENT
$loanA = 2
$loanB = 1

$baseUrl = "http://localhost:8080"
$paymentUrl = "$baseUrl/api/collector/payments"

# Amount of each test payment (LKR)
$amount = [decimal]1.00

Write-Host ""
Write-Host "===================================="
Write-Host " QuickCash Module 23 - Concurrency"
Write-Host "===================================="
Write-Host ""

# Enter JWT securely
$secureToken = Read-Host "Enter ACTIVE COLLECTOR JWT" -AsSecureString

$jwt = [System.Net.NetworkCredential]::new(
    "",
    $secureToken
).Password.Trim()

if ($jwt.StartsWith("Bearer ")) {
    $jwt = $jwt.Substring(7).Trim()
}

if ([string]::IsNullOrWhiteSpace($jwt)) {
    throw "JWT token is empty."
}

# Create HTTP client
$client = [System.Net.Http.HttpClient]::new()

$client.Timeout = [TimeSpan]::FromSeconds(60)

$client.DefaultRequestHeaders.Authorization =
    [System.Net.Http.Headers.AuthenticationHeaderValue]::new(
        "Bearer",
        $jwt
    )

# --------------------------------------------
# CHECK: Loan must be assigned to collector
# --------------------------------------------
function Get-TestLoan {

    param(
        [int]$LoanId
    )

    $url = "$baseUrl/api/collector/loans/$LoanId"

    $response = $client.GetAsync(
        $url
    ).GetAwaiter().GetResult()

    try {
        $status = [int]$response.StatusCode

        if ($status -ne 200) {
            throw "Loan $LoanId pre-check failed. HTTP $status. Check JWT, loan status and collector assignment."
        }

        $body = $response.Content.ReadAsStringAsync().
            GetAwaiter().GetResult()

        return ($body | ConvertFrom-Json)
    }
    finally {
        $response.Dispose()
    }
}

# --------------------------------------------
# FUNCTION: Send 2 requests concurrently
# --------------------------------------------
function Invoke-PaymentPair {

    param(
        [string]$Json1,
        [string]$Json2
    )

    $content1 = [System.Net.Http.StringContent]::new(
        $Json1,
        [System.Text.Encoding]::UTF8,
        "application/json"
    )

    $content2 = [System.Net.Http.StringContent]::new(
        $Json2,
        [System.Text.Encoding]::UTF8,
        "application/json"
    )

    try {
        # Start BOTH requests without waiting
        $task1 = $client.PostAsync(
            $paymentUrl,
            $content1
        )

        $task2 = $client.PostAsync(
            $paymentUrl,
            $content2
        )

        # Wait until both complete
        [System.Threading.Tasks.Task]::WaitAll(
            [System.Threading.Tasks.Task[]]@(
                $task1,
                $task2
            )
        )

        $response1 = $task1.Result
        $response2 = $task2.Result

        try {
            $body1 = $response1.Content.
                ReadAsStringAsync().GetAwaiter().GetResult()

            $body2 = $response2.Content.
                ReadAsStringAsync().GetAwaiter().GetResult()

            @(
                [pscustomobject]@{
                    Status = [int]$response1.StatusCode
                    Body = $body1
                }

                [pscustomobject]@{
                    Status = [int]$response2.StatusCode
                    Body = $body2
                }
            )
        }
        finally {
            $response1.Dispose()
            $response2.Dispose()
        }
    }
    finally {
        $content1.Dispose()
        $content2.Dispose()
    }
}

try {

    Write-Host "Checking backend and loan access..."

    # Check both loans before creating payments
    $checkA = Get-TestLoan -LoanId $loanA
    $checkB = Get-TestLoan -LoanId $loanB

    if ($loanA -eq $loanB) {
        throw "Test A and Test B must use different loan IDs."
    }

    if ($checkA.status -ne "ACTIVE" -or
        $checkB.status -ne "ACTIVE") {
        throw "Both loans must be ACTIVE."
    }

    if ([decimal]$checkA.outstandingBalance -lt 2 -or
        [decimal]$checkB.outstandingBalance -lt 1) {
        throw "Insufficient test balance. Loan A needs at least LKR 2 and Loan B at least LKR 1."
    }

    Write-Host "Loan access checks PASSED"
    Write-Host ""

    # ========================================
    # TEST A - Same Loan, Same Transaction ID
    # ========================================

    Write-Host "Starting TEST A..."

    $sameId = "QC-M23-SAME-" +
        [guid]::NewGuid().ToString("N").Substring(0,12)

    $sameBody = @{
        loanId = $loanA
        amount = $amount
        clientTransactionId = $sameId
    } | ConvertTo-Json -Compress

    $same = @(
        Invoke-PaymentPair `
            -Json1 $sameBody `
            -Json2 $sameBody
    )

    Write-Host ""
    Write-Host "TEST A Transaction ID: $sameId"
    Write-Host "Request 1 HTTP:" $same[0].Status
    Write-Host "Request 2 HTTP:" $same[1].Status

    $samePassed = $false

    if ($same[0].Status -eq 201 -and
        $same[1].Status -eq 201) {

        $p1 = $same[0].Body | ConvertFrom-Json
        $p2 = $same[1].Body | ConvertFrom-Json

        Write-Host "Payment ID 1:" $p1.paymentId
        Write-Host "Payment ID 2:" $p2.paymentId

        $samePassed = (
            ($null -ne $p1.paymentId) -and
            ($p1.paymentId -eq $p2.paymentId) -and
            ($p1.clientTransactionId -eq $sameId) -and
            ($p2.clientTransactionId -eq $sameId)
        )
    }

    Write-Host "TEST A PASS:" $samePassed

    if (-not $samePassed) {
        Write-Host ""
        Write-Host "TEST A response 1:"
        Write-Host $same[0].Body
        Write-Host "TEST A response 2:"
        Write-Host $same[1].Body

        throw "TEST A failed. Check the database before retrying."
    }

    # ========================================
    # TEST B - Different Loans, Same ID
    # ========================================

    Write-Host ""
    Write-Host "Starting TEST B..."

    $crossId = "QC-M23-CROSS-" +
        [guid]::NewGuid().ToString("N").Substring(0,12)

    $bodyLoanA = @{
        loanId = $loanA
        amount = $amount
        clientTransactionId = $crossId
    } | ConvertTo-Json -Compress

    $bodyLoanB = @{
        loanId = $loanB
        amount = $amount
        clientTransactionId = $crossId
    } | ConvertTo-Json -Compress

    $cross = @(
        Invoke-PaymentPair `
            -Json1 $bodyLoanA `
            -Json2 $bodyLoanB
    )

    Write-Host ""
    Write-Host "TEST B Transaction ID: $crossId"
    Write-Host "Request 1 HTTP:" $cross[0].Status
    Write-Host "Request 2 HTTP:" $cross[1].Status

    $statuses = @(
        $cross[0].Status,
        $cross[1].Status
    )

    $crossPassed = (
        ($statuses -contains 201) -and
        ($statuses -contains 409) -and
        ($statuses -notcontains 500)
    )

    Write-Host "TEST B PASS:" $crossPassed

    if (-not $crossPassed) {
        Write-Host ""
        Write-Host "TEST B response 1:"
        Write-Host $cross[0].Body

        Write-Host "TEST B response 2:"
        Write-Host $cross[1].Body

        throw "TEST B failed. Check the database before retrying."
    }

    # ========================================
    # SUMMARY
    # ========================================

    Write-Host ""
    Write-Host "===================================="
    Write-Host "        TEST RESULTS"
    Write-Host "===================================="

    Write-Host "TEST A: PASS"
    Write-Host "TEST B: PASS"

    Write-Host ""
    Write-Host "HTTP CONCURRENCY CHECKS PASSED"
    Write-Host "Verify payment counts and balances in MySQL."

    Write-Host ""
    Write-Host "Test A ID: $sameId"
    Write-Host "Test B ID: $crossId"
}
finally {

    $client.Dispose()

    $jwt = $null
    $secureToken = $null
}
