# Portfolio Database Restore Script
# Restores MySQL and MongoDB data from a backup folder

param(
    [Parameter(Mandatory = $true)]
    [string]$BackupPath
)

if (-not (Test-Path $BackupPath)) {
    Write-Host "Backup path not found: $BackupPath" -ForegroundColor Red
    exit 1
}

$mysqlFile = Join-Path $BackupPath "mysql\portfoliodb.sql"
$mongoArchive = Join-Path $BackupPath "mongo\portfolio_audit.archive"

# MySQL restore (credentials can be overridden via environment variables)
$mysqlContainer = "portfolio_mysql"
$mysqlUser = if ($env:PORTFOLIO_MYSQL_USER) { $env:PORTFOLIO_MYSQL_USER } else { "root" }
$mysqlPass = if ($env:PORTFOLIO_MYSQL_PASSWORD) { $env:PORTFOLIO_MYSQL_PASSWORD } else { "rootpass" }
$mysqlDb = if ($env:PORTFOLIO_MYSQL_DATABASE) { $env:PORTFOLIO_MYSQL_DATABASE } else { "portfoliodb" }

if (Test-Path $mysqlFile) {
    try {
        Get-Content $mysqlFile | docker exec -i $mysqlContainer mysql -u $mysqlUser --password=$mysqlPass $mysqlDb
        Write-Host "MySQL restored from: $mysqlFile" -ForegroundColor Green
    } catch {
        Write-Host "MySQL restore error: $_" -ForegroundColor Red
    }
} else {
    Write-Host "MySQL backup file not found: $mysqlFile" -ForegroundColor Yellow
}

# MongoDB restore
$mongoContainer = "portfolio_mongo"

if (Test-Path $mongoArchive) {
    try {
        docker cp $mongoArchive "${mongoContainer}:/data/backup/portfolio_audit.archive"
        docker exec $mongoContainer mongorestore --archive=/data/backup/portfolio_audit.archive --drop
        Write-Host "MongoDB restored from: $mongoArchive" -ForegroundColor Green
    } catch {
        Write-Host "MongoDB restore error: $_" -ForegroundColor Red
    }
} else {
    Write-Host "MongoDB backup archive not found: $mongoArchive" -ForegroundColor Yellow
}

Write-Host "Restore completed at: $(Get-Date)" -ForegroundColor Cyan
