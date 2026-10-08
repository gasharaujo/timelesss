@echo off
setlocal
chcp 65001 >nul
:menu
cls
echo ========================================
echo        Just Gash Timelesss
echo ========================================
echo 1. Jogar - Minecraft 26.1.2 NeoForge
echo 2. Jogar - Minecraft 1.21.1 NeoForge
echo 3. Jogar - Minecraft 1.20.1 Forge
echo 4. Criar os tres JARs finais
echo 5. Sair
echo.
set /p "OPTION=Escolha uma opcao: "

if "%OPTION%"=="1" call "%~dp0jogar-neoforge-26.1.2.bat"
if "%OPTION%"=="2" call "%~dp0jogar-neoforge-1.21.1.bat"
if "%OPTION%"=="3" call "%~dp0jogar-forge-1.20.1.bat"
if "%OPTION%"=="4" call "%~dp0criar-jars.bat"
if "%OPTION%"=="5" exit /b 0
goto :menu

