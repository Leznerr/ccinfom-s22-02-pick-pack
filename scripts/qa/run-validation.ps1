Param(
    [string]$User,
    [string]$Password,
    [string]$DbHost,
    [int]$DbPort,
    [string]$Schema,
    [string]$MysqlPath
)

if (-not $User)     { $User     = $env:CCINFOM_DB_USER }
if (-not $Password) { $Password = $env:CCINFOM_DB_PASS }
if (-not $DbHost)   { $DbHost   = $env:CCINFOM_DB_HOST }
if (-not $DbPort)   { $DbPort   = if ($env:CCINFOM_DB_PORT) { [int]$env:CCINFOM_DB_PORT } else { 3306 } }
if (-not $Schema)   { $Schema   = if ($env:CCINFOM_DB_SCHEMA) { $env:CCINFOM_DB_SCHEMA } else { "ccinfom_dev" } }
if (-not $MysqlPath){ $MysqlPath= if ($env:CCINFOM_MYSQL) { $env:CCINFOM_MYSQL } else { "mysql" } }

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot   = Split-Path -Parent (Split-Path -Parent $scriptRoot)
$queryFile  = Join-Path $repoRoot "qa\validation_queries.sql"
$reportDir  = Join-Path $repoRoot "qa\tests"

if (-not (Test-Path $queryFile)) {
    throw "Unable to locate validation_queries.sql at $queryFile"
}

if (-not $User) {
    throw "Database user is required. Provide -User or set CCINFOM_DB_USER."
}

if (-not $Password) {
    $secure = Read-Host -Prompt "Enter password for user '$User'" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $Password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

if (-not (Test-Path $reportDir)) {
    New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportFile = Join-Path $reportDir "phaseE-validation-$timestamp.log"

$args = @()
if ($DbHost) { $args += @("-h", $DbHost) }
$args += @("-P", $DbPort)
$args += @("-u", $User)
$args += "-p$Password"
$args += $Schema

Write-Host "Running QA validation queries against schema '$Schema'..."

$sqlText = Get-Content $queryFile -Raw
$sqlText | & $MysqlPath @args 2>&1 | Tee-Object -FilePath $reportFile

Write-Host "QA validation report written to $reportFile"
