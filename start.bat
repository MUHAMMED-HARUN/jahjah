@echo off
setlocal
rem Launches the Products desktop app on this folder's data ("data base" + "images").
rem Builds the jar first if it does not exist yet (or if called with "rebuild").

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"
set "JAR=%ROOT%\desktop\out\products-manager.jar"

where java >nul 2>nul
if errorlevel 1 (
    echo Java was not found. Install JDK 21 or newer and add it to PATH.
    pause
    exit /b 1
)

if /i "%~1"=="rebuild" goto build
if exist "%JAR%" goto run

:build
echo Building...
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%\desktop\build.ps1"
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)

:run
start "" javaw -Dfile.encoding=UTF-8 -jar "%JAR%" "%ROOT%"
endlocal
