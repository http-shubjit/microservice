@echo off
echo ===================================================
echo Starting Microservices Architecture!
echo ===================================================

echo [1/5] Starting Eureka Server...
start "Eureka Server" cmd /k "cd eureka-server && mvn spring-boot:run"

echo Waiting 30 seconds for Eureka to fully boot...
timeout /t 30 /nobreak >nul

echo [2/5] Starting Config Server...
start "Config Server" cmd /k "cd config-server && mvn spring-boot:run"
echo Waiting 30 seconds for Config Server to fully boot...
timeout /t 30 /nobreak >nul

echo [3/5] Starting Api Gateway...
start "Api Gateway" cmd /k "cd api-gateway && mvn spring-boot:run"
echo Waiting 30 seconds for Api Gateway to fully boot...
timeout /t 30 /nobreak >nul

echo [4/5] Starting Auth Service...
start "Auth Service" cmd /k "cd auth-service && mvn spring-boot:run"
echo Waiting 30 seconds for Auth Service to fully boot...
timeout /t 30 /nobreak >nul

echo [5/5] Starting Data Service...
start "Data Service" cmd /k "cd data-service && mvn spring-boot:run"
echo Waiting 30 seconds for Auth Service to fully boot...
timeout /t 30 /nobreak >nul

echo ===================================================
echo All services are launching! You can safely close this window.
echo ===================================================
pause