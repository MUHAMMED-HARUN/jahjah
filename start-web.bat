@echo off
setlocal
rem Serves this folder over HTTP and opens index.html in the default browser.
rem (fetch does not work from file://, so a local server is required.)
rem Usage: start-web.bat [port]   default port 8000. Close this window to stop the server.

set "PORT=%~1"
if "%PORT%"=="" set "PORT=8000"
cd /d "%~dp0"

set "PY="
where py >nul 2>nul && set "PY=py -3"
if not defined PY where python >nul 2>nul && set "PY=python"
if not defined PY (
    echo Python was not found. Install Python 3 and add it to PATH.
    pause
    exit /b 1
)

echo Serving %CD% on http://localhost:%PORT%/
echo Close this window (or press Ctrl+C) to stop the server.
start "" /b cmd /c "timeout /t 2 /nobreak >nul & start "" http://localhost:%PORT%/index.html"
%PY% -m http.server %PORT% --bind 127.0.0.1
if errorlevel 1 (
    echo.
    echo The server could not start. Port %PORT% may be in use; try: start-web.bat 8080
    pause
)
endlocal
