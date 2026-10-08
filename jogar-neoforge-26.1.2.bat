@echo off
setlocal
chcp 65001 >nul
set "PROJECT=%~dp0neoforge-26.1.2"
call "%PROJECT%\gradlew.bat" -p "%PROJECT%" runClient
if errorlevel 1 (
    echo.
    echo Falha ao iniciar Minecraft 26.1.2 com NeoForge.
    pause
)
endlocal

