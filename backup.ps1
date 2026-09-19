# Portfolio Database Backup Script
# Backs up MySQL and MongoDB data to D:\Portfolio-Backups\<timestamp>

param(
    [string]$BackupRoot = "D:\Portfolio-Backups"
)

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = Join-Path $BackupRoot $timestamp
$mysqlDir = Join-Path $backupDir "mysql"
$mongoDir = Join-Path $backupDir "mongo"

New-Item -ItemType Directory -Path $mysqlDir -Force | Out-Null
New-Item -ItemType Directory -Path $mongoDir -Force | Out-Null

Write-Host "Starting backup to: $backupDir" -ForegroundColor Cyan

# MySQL backup (credentials can be overridden via environment variables)
$mysqlContainer = "portfolio_mysql"
$mysqlUser = if ($env:PORTFOLIO_MYSQL_USER) { $env:PORTFOLIO_MYSQL_USER } else { "root" }
$mysqlPass = if ($env:PORTFOLIO_MYSQL_PASSWORD) { $env:PORTFOLIO_MYSQL_PASSWORD } else { "rootpass" }
$mysqlDb = if ($env:PORTFOLIO_MYSQL_DATABASE) { $env:PORTFOLIO_MYSQL_DATABASE } else { "portfoliodb" }
$mysqlFile = Join-Path $mysqlDir "portfoliodb.sql"

try {
    docker exec $mysqlContainer mysqldump -u $mysqlUser --password=$mysqlPass $mysqlDb > $mysqlFile
    if ($LASTEXITCODE -ne 0) { throw "MySQL backup failed" }
    Write-Host "MySQL backup saved to: $mysqlFile" -ForegroundColor Green
} catch {
    Write-Host "MySQL backup error: $_" -ForegroundColor Red
}

# MongoDB backup
$mongoContainer = "portfolio_mongo"
$mongoDb = "portfolio_audit"
$mongoArchive = Join-Path $mongoDir "portfolio_audit.archive"

try {
    docker exec $mongoContainer mkdir -p /data/backup
    docker exec $mongoContainer mongodump --db=$mongoDb --archive=/data/backup/portfolio_audit.archive
    if ($LASTEXITCODE -ne 0) { throw "MongoDB backup failed" }
    docker cp "${mongoContainer}:/data/backup/portfolio_audit.archive" $mongoArchive
    Write-Host "MongoDB backup saved to: $mongoArchive" -ForegroundColor Green
} catch {
    Write-Host "MongoDB backup error: $_" -ForegroundColor Red
}

Write-Host "Backup completed at: $(Get-Date)" -ForegroundColor Cyan
Write-Host "Backup location: $backupDir"
