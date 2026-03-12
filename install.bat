@echo off
setlocal EnableDelayedExpansion

:: Save the current directory
set "OriginalDir=%CD%"

:: Define service name
set "ServiceName=SessionTrackerService"

:: Get base directory (where install.bat is located)
set "BaseDir=%~dp0"

:: -------------------------------
:: 1. Create and Start Service
:: -------------------------------

cd /d "%BaseDir%service" || (
    echo Failed to change directory to service folder: "%BaseDir%service"
    exit /b 1
)

sc query "%ServiceName%" >nul 2>&1
if %errorlevel%==0 (
    echo Service already exists.
) else (
    sc create "%ServiceName%" binPath= "\"%BaseDir%service\ShutdownTimeUpdaterService.exe\"" start= auto
    if errorlevel 1 (
        echo Failed to create service.
        exit /b 1
    )
    echo Service created successfully.
)

sc start "%ServiceName%" >nul 2>&1
echo Service started (or already running).

:: -------------------------------
:: 2. Register GUI at Startup
::    FIXED working directory
:: -------------------------------

set "JavawPath=%BaseDir%jre\bin\javaw.exe"
set "GuiDir=%BaseDir%gui"
set "JarName=app-gui.jar"

reg add "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" ^
 /v SessionTracker ^
 /t REG_SZ ^
 /d "cmd /c start \"\" /D \"%GuiDir%\" \"%JavawPath%\" -jar \"%GuiDir%\%JarName%\"" ^
 /f

if errorlevel 1 (
    echo Failed to register GUI app to run at login.
    exit /b 1
) else (
    echo GUI app successfully registered to run at login.
)

:: -------------------------------
:: 3. Restore original directory
:: -------------------------------

cd /d "%OriginalDir%"
echo Returned to original directory: %OriginalDir%

echo.
echo =====================================
echo Installation complete.
echo =====================================

endlocal
pause
