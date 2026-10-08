@echo off
setlocal
chcp 65001 >nul
set "PROJECT=%~dp0neoforge-1.21.1"
call "%PROJECT%\gradlew.bat" -p "%PROJECT%" runClient
if errorlevel 1 (
    echo.
    echo Falha ao iniciar Minecraft 1.21.1 com NeoForge.
    pause
)
endlocal

