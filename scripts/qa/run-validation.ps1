<#
    .SYNOPSIS
        Executes the consolidated QA validation queries against a target MySQL schema.

    .DESCRIPTION
        This script is the single entry point for Gate A-D checks. It loads
        qa/validation_queries.sql and pipes the contents into the MySQL CLI,
        capturing the console output to qa/tests/phaseE-validation-<timestamp>.log.

        Connection settings are resolved in the following order:
            1. Explicit script parameters
            2. Environment variables (CCINFOM_DB_*)
            3. Interactive prompts (user/password only)
#>
Param(
    [Alias('User')]
    [string]$DbUser,
    [Alias('Password')]
    [string]$DbPassword,
    [Alias('Host')]
    [string]$DbHost,
    [Alias('Port')]
    [int]$DbPort,
    [Alias('Database')]
    [string]$DbName,
    [string]$MysqlPath
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------
# Resolve repository paths
# ---------------------------------------------------------------------
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot  = Split-Path -Parent (Split-Path -Parent $scriptDir)
$qaDir     = Join-Path $repoRoot 'qa'
$sqlFile   = Join-Path $qaDir 'validation_queries.sql'
$logDir    = Join-Path $qaDir 'tests'

if (-not (Test-Path $sqlFile)) {
    throw "Unable to locate validation SQL at '$sqlFile'."
}

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
}

# ---------------------------------------------------------------------
# Resolve connection settings (param > env > default/prompt)
# ---------------------------------------------------------------------
function Resolve-OrDefault {
    param(
        [string]$ParamValue,
        [string]$EnvValue,
        [string]$DefaultValue
    )
    if ($ParamValue) { return $ParamValue }
    if ($EnvValue)   { return $EnvValue }
    return $DefaultValue
}

$dbHost   = Resolve-OrDefault $DbHost     $env:CCINFOM_DB_HOST   'localhost'
$dbPort   = [int](Resolve-OrDefault $DbPort $env:CCINFOM_DB_PORT '3306')
$dbName   = Resolve-OrDefault $DbName     $env:CCINFOM_DB_SCHEMA 'ccinfom_dev'
$dbUser   = Resolve-OrDefault $DbUser     $env:CCINFOM_DB_USER   $null
$dbPass   = Resolve-OrDefault $DbPassword $env:CCINFOM_DB_PASS   $null
$mysqlExe = Resolve-OrDefault $MysqlPath  $env:CCINFOM_MYSQL    'mysql'

if (-not $dbUser) {
    $dbUser = Read-Host 'Enter MySQL user'
}

if (-not $dbPass) {
    $secure = Read-Host -Prompt "Enter password for '$dbUser'" -AsSecureString
    $pwdPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $dbPass = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pwdPtr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pwdPtr)
    }
}

# ---------------------------------------------------------------------
# Verify mysql client
# ---------------------------------------------------------------------
try {
    $mysqlCmd = Get-Command $mysqlExe -ErrorAction Stop
}
catch {
    throw "MySQL client '$mysqlExe' is not available on PATH. Set CCINFOM_MYSQL or pass -MysqlPath."
}

# ---------------------------------------------------------------------
# Execute validation queries
# ---------------------------------------------------------------------
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$logFile   = Join-Path $logDir "phaseE-validation-$timestamp.log"

Write-Host "Running QA validation:"
Write-Host "  Host     : $dbHost`:$dbPort"
Write-Host "  Schema   : $dbName"
Write-Host "  User     : $dbUser"
Write-Host "  Log file : $logFile"

$args = @(
    '-h', $dbHost,
    '-P', $dbPort,
    '-u', $dbUser,
    $dbName,
    '--table'
)

$previousMysqlPwd = $env:MYSQL_PWD
$env:MYSQL_PWD = $dbPass

try {
    Get-Content -Raw $sqlFile |
        & $mysqlCmd.Source @args 2>&1 |
        Tee-Object -FilePath $logFile | Out-Host
    Write-Host "Validation complete. Results captured in $logFile"
}
catch {
    throw "QA validation failed: $($_.Exception.Message)"
}
finally {
    if ($previousMysqlPwd -ne $null) {
        $env:MYSQL_PWD = $previousMysqlPwd
    }
    else {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
}
