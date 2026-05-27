@echo off
setlocal
cd /d "%~dp0"

set "DEV_HOME=D:\AndroidDev"
if exist "%DEV_HOME%\.jdk\jdk-21.0.11+10\bin\java.exe" set "JAVA_HOME=%DEV_HOME%\.jdk\jdk-21.0.11+10"
if exist "%DEV_HOME%\.gradle" set "GRADLE_USER_HOME=%DEV_HOME%\.gradle"

if not defined JAVA_HOME if exist "%~dp0.jdk\jdk-21.0.11+10\bin\java.exe" set "JAVA_HOME=%~dp0.jdk\jdk-21.0.11+10"
if not defined JAVA_HOME if exist "%~dp0..\.jdk\jdk-21.0.11+10\bin\java.exe" set "JAVA_HOME=%~dp0..\.jdk\jdk-21.0.11+10"
if not defined JAVA_HOME (
  echo JAVA_HOME is not set. Install JDK 17+ or put it in D:\AndroidDev\.jdk\jdk-21.0.11+10.
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

call "%~dp0gradlew.bat" :app:assembleDebug
endlocal
