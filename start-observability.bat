@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   FinFlow Observability Stack - local optimized
echo ===================================================

:: Set the base observability path based on the current script location
set "BASE_DIR=%~dp0"
set "PROMETHEUS_DIR=%BASE_DIR%prometheus-3.1.0.windows-amd64"
set "LOKI_DIR=%BASE_DIR%loki"
set "PROMTAIL_DIR=%BASE_DIR%promtail"
set "GRAFANA_DIR=%BASE_DIR%GrafanaLabs\grafana\bin"
:: SonarQube Path Extracted From User Context
set "SONARQUBE_DIR=E:\backup\toppings\sonarqube-9.9.5.90363\bin\windows-x86-64"

:: 1. Start Loki FIRST
echo [*] Starting Grafana Loki from !LOKI_DIR!...
start "Loki" cmd /k "cd /d !LOKI_DIR! && loki-windows-amd64.exe --config.file=loki-config.yaml"
timeout /t 5 >nul

:: 2. Start Zipkin (Located in E:\ as per previous logs)
echo [*] Starting Zipkin Distributed Tracing...
if exist E:\zipkin.jar (
    start "Zipkin" cmd /k "java -jar E:\zipkin.jar"
) else (
    echo [!] WARNING: Zipkin binary NOT FOUND at E:\zipkin.jar
)
timeout /t 2 >nul

:: 3. Start Prometheus
echo [*] Starting Prometheus from !PROMETHEUS_DIR!...
start "Prometheus" cmd /k "cd /d !PROMETHEUS_DIR! && prometheus.exe --config.file=e:\backup\finflow_sonarqube_docker\prometheus-local.yml"
timeout /t 2 >nul

:: 4. Start Promtail
echo [*] Starting Promtail Log Scraper from !PROMTAIL_DIR!...
start "Promtail" cmd /k "cd /d !PROMTAIL_DIR! && promtail-windows-amd64.exe --config.file=promtail-config.yaml"
timeout /t 2 >nul

:: 5. Start Grafana
echo [*] Starting Grafana Visualization from !GRAFANA_DIR!...
start "Grafana" cmd /k "cd /d !GRAFANA_DIR! && grafana-server.exe"
timeout /t 2 >nul

:: 6. Start SonarQube
echo [*] Starting SonarQube Code Quality from !SONARQUBE_DIR!...
start "SonarQube" cmd /k "cd /d !SONARQUBE_DIR! && StartSonar.bat"

echo.
echo ===================================================
echo [SUCCESS] Observability Stack is coming up!
echo ---------------------------------------------------
echo Loki Push: http://localhost:3100/loki/api/v1/push
echo Zipkin:    http://localhost:9411
echo Grafana:   http://localhost:3000
echo SonarQube: http://localhost:9000
echo ===================================================
echo.
pause
