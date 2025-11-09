@echo off
REM Script to build Docker containers, run tests, and cleanup for Windows
REM Usage: docker-test.bat [--skip-build]
REM   --skip-build: Skip Docker image build phase (use existing images)
REM 
REM Note: This script should be run from the project root directory

setlocal enabledelayedexpansion

REM Get the directory where this script is located
set "SCRIPT_DIR=%~dp0"
REM Get the project root directory (parent of docker directory)
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"

REM Change to project root to ensure relative paths work correctly
cd /d "%PROJECT_ROOT%"

REM Parse command line arguments
set "SKIP_BUILD=false"
if "%1"=="--skip-build" set "SKIP_BUILD=true"
if "%1"=="-s" set "SKIP_BUILD=true"

REM Also check environment variable
if "%SKIP_DOCKER_BUILD%"=="true" set "SKIP_BUILD=true"
if "%SKIP_DOCKER_BUILD%"=="1" set "SKIP_BUILD=true"

echo ==========================================
echo Docker Integration Test Runner
echo ==========================================

REM Function to cleanup
:cleanup
echo.
echo Cleaning up Docker containers...
docker compose -f docker/docker-compose.yml down -v
echo Cleanup completed
goto :eof

REM Trap cleanup on exit
set "CLEANUP_CALLED=0"

REM Build Docker images (unless skipped)
if "%SKIP_BUILD%"=="true" (
    echo Step 1: Skipping Docker image build (using existing images)...
) else (
    echo Step 1: Building Docker images...
    docker compose -f docker/docker-compose.yml build
    if errorlevel 1 (
        echo Error building Docker images
        call :cleanup
        exit /b 1
    )
)

echo.
echo Step 2: Starting Docker containers...
docker compose -f docker/docker-compose.yml up -d
if errorlevel 1 (
    echo Error starting Docker containers
    call :cleanup
    exit /b 1
)

echo.
echo Step 3: Waiting for services to be healthy...
echo Waiting for PostgreSQL...
set TIMEOUT=60
set ELAPSED=0
:wait_postgres
docker compose -f docker/docker-compose.yml exec -T postgres pg_isready -U postgres >nul 2>&1
if errorlevel 1 (
    if !ELAPSED! geq !TIMEOUT! (
        echo PostgreSQL failed to start within !TIMEOUT! seconds
        call :cleanup
        exit /b 1
    )
    timeout /t 2 /nobreak >nul
    set /a ELAPSED+=2
    echo|set /p="."
    goto :wait_postgres
)
echo PostgreSQL is ready

echo Waiting for application...
set TIMEOUT=120
set ELAPSED=0
:wait_app
curl -f http://localhost:8081/api/guests >nul 2>&1
if errorlevel 1 (
    if !ELAPSED! geq !TIMEOUT! (
        echo Application failed to start within !TIMEOUT! seconds
        docker compose -f docker/docker-compose.yml logs app
        call :cleanup
        exit /b 1
    )
    timeout /t 3 /nobreak >nul
    set /a ELAPSED+=3
    echo|set /p="."
    goto :wait_app
)
echo Application is ready

echo.
echo Step 4: Running integration tests...
mvn clean test -Dtest=GuestControllerIntegrationTest

set TEST_RESULT=%ERRORLEVEL%

if %TEST_RESULT% equ 0 (
    echo.
    echo ==========================================
    echo All tests passed!
    echo ==========================================
) else (
    echo.
    echo ==========================================
    echo Tests failed!
    echo ==========================================
    echo.
    echo Application logs:
    docker compose -f docker/docker-compose.yml logs app --tail=50
)

call :cleanup
exit /b %TEST_RESULT%

