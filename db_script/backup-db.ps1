param(
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$UserName = "postgres",
    [string]$Database = "omron",
    [string]$OutputDir = ".\\db_backups",
    [string]$Password = ""
)

$ErrorActionPreference = "Stop"

if ($Password -and $Password.Trim().Length -gt 0) {
    $env:PGPASSWORD = $Password
}

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    throw "pg_dump nao encontrado no PATH. Instale PostgreSQL client tools ou ajuste PATH."
}

if (-not (Test-Path -LiteralPath $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$fileName = "{0}-{1}.backup" -f $Database, $timestamp
$backupPath = Join-Path $OutputDir $fileName

Write-Host "Iniciando backup do banco '$Database'..."
$cmdArgs = @(
    "-h", $HostName,
    "-p", "$Port",
    "-U", $UserName,
    "-d", $Database,
    "-F", "c",
    "-b",
    "-v",
    "-f", $backupPath
)

& pg_dump @cmdArgs

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao gerar backup. Exit code: $LASTEXITCODE"
}

Write-Host "Backup concluido com sucesso: $backupPath"

if ($env:PGPASSWORD) {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
