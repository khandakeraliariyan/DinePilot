$ErrorActionPreference = "Stop"

$envFile = Join-Path $PSScriptRoot ".env"

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^\s*#|^\s*$") { return }
        $parts = $_ -split "=", 2
        if ($parts.Length -eq 2) {
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}
else {
    Write-Host ".env file not found at: $envFile" -ForegroundColor Yellow
}

Set-Location $PSScriptRoot

Write-Host "Starting DinePilot services..." -ForegroundColor Cyan

# Start infra first
& .\mvnw.cmd -f .\eureka-server\pom.xml spring-boot:run

Start-Sleep -Seconds 5

& .\mvnw.cmd -f .\user-service\pom.xml spring-boot:run
& .\mvnw.cmd -f .\restaurant-service\pom.xml spring-boot:run
& .\mvnw.cmd -f .\reservation-service\pom.xml spring-boot:run
& .\mvnw.cmd -f .\order-service\pom.xml spring-boot:run
& .\mvnw.cmd -f .\billing-service\pom.xml spring-boot:run
& .\mvnw.cmd -f .\api-gateway\pom.xml spring-boot:run
