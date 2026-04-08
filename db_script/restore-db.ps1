param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$UserName = "postgres",
    [string]$Database = "omron",
    [switch]$CreateDatabase,
    [string]$Password = ""
)

$ErrorActionPreference = "Stop"

if ($Password -and $Password.Trim().Length -gt 0) {
    $env:PGPASSWORD = $Password
}

if (-not (Get-Command pg_restore -ErrorAction SilentlyContinue)) {
    throw "pg_restore nao encontrado no PATH. Instale PostgreSQL client tools ou ajuste PATH."
}

$resolvedBackup = Resolve-Path -LiteralPath $BackupFile -ErrorAction Stop

if ($CreateDatabase) {
    if (-not (Get-Command createdb -ErrorAction SilentlyContinue)) {
        throw "createdb nao encontrado no PATH. Instale PostgreSQL client tools ou ajuste PATH."
    }

    Write-Host "Criando banco '$Database' (se nao existir)..."
    & createdb -h $HostName -p "$Port" -U $UserName $Database 2>$null
}

Write-Host "Restaurando backup '$resolvedBackup' no banco '$Database'..."
$cmdArgs = @(
    "-h", $HostName,
    "-p", "$Port",
    "-U", $UserName,
    "-d", $Database,
    "-v",
    "--clean",
    "--if-exists",
    "--no-owner",
    "--no-privileges",
    "$resolvedBackup"
)

& pg_restore @cmdArgs

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao restaurar backup. Exit code: $LASTEXITCODE"
}

Write-Host "Restore concluido com sucesso."

if ($env:PGPASSWORD) {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
