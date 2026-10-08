@echo off
setlocal
chcp 65001 >nul
set "PROJECT=%~dp0forge-1.20.1"
call "%PROJECT%\gradlew.bat" -p "%PROJECT%" runClient
if errorlevel 1 (
    echo.
    echo Falha ao iniciar Minecraft 1.20.1 com Forge.
    pause
)
endlocal

