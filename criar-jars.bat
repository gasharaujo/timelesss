@echo off
setlocal
chcp 65001 >nul
set "ROOT=%~dp0"
set "ver=2.1.2"
set "OUTPUT=%ROOT%builds"

echo [1/3] Compilando NeoForge 26.1.2...
call "%ROOT%neoforge-26.1.2\gradlew.bat" -p "%ROOT%neoforge-26.1.2" build
if errorlevel 1 goto :failure

echo [2/3] Compilando NeoForge 1.21.1...
call "%ROOT%neoforge-1.21.1\gradlew.bat" -p "%ROOT%neoforge-1.21.1" build
if errorlevel 1 goto :failure

echo [3/3] Compilando Forge 1.20.1...
call "%ROOT%forge-1.20.1\gradlew.bat" -p "%ROOT%forge-1.20.1" build
if errorlevel 1 goto :failure

if not exist "%OUTPUT%" mkdir "%OUTPUT%"
copy /Y "%ROOT%neoforge-26.1.2\build\libs\timelesss-neoforge-26.1.2-%ver%.jar" "%OUTPUT%\" >nul
copy /Y "%ROOT%neoforge-1.21.1\build\libs\timelesss-neoforge-1.21.1-%ver%.jar" "%OUTPUT%\" >nul
copy /Y "%ROOT%forge-1.20.1\build\libs\timelesss-forge-1.20.1-%ver%.jar" "%OUTPUT%\" >nul

echo.
echo JARs criados com sucesso em:
echo %OUTPUT%
pause
exit /b 0

:failure
echo.
echo A compilacao falhou. Veja os erros acima.
pause
exit /b 1
