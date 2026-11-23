@echo off
setlocal
chcp 65001 >nul
set "ROOT=%~dp0"
set "VER="
for /f "tokens=1,* delims==" %%A in ('findstr /b "mod_version=" "%ROOT%forge-1.20.1\gradle.properties"') do set "VER=%%B"
if not defined VER goto :failure
set "OUTPUT=%ROOT%builds\%VER%"
set "JAR=timelesss-%VER%.jar"

call :build forge-1.20.1
if errorlevel 1 goto :failure
call :build neoforge-1.21.1 --no-problems-report
if errorlevel 1 goto :failure

if exist "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
call :build neoforge-26.1.2 --no-problems-report
if errorlevel 1 goto :failure
call :build neoforge-26.3 --no-problems-report
if errorlevel 1 goto :failure

powershell -NoProfile -Command "$ErrorActionPreference='Stop'; Compress-Archive -Path (Join-Path $env:OUTPUT '*') -DestinationPath (Join-Path (Split-Path $env:OUTPUT) ('timelesss-' + $env:VER + '.zip')) -Force"
if errorlevel 1 goto :failure

echo JARs criados em %OUTPUT%
pause
exit /b 0

:build
echo Compilando %~1...
call "%ROOT%%~1\gradlew.bat" -p "%ROOT%%~1" build --console=plain %~2
if errorlevel 1 exit /b 1
if not exist "%OUTPUT%\%~1" mkdir "%OUTPUT%\%~1"
copy /Y "%ROOT%%~1\build\libs\%JAR%" "%OUTPUT%\%~1\%JAR%" >nul
exit /b %errorlevel%

:failure
echo A compilacao ou o empacotamento falhou. Veja os erros acima.
pause
exit /b 1
