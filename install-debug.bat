@echo off
setlocal
cd /d "%~dp0"

if exist "%~dp0.jdk\jdk-21.0.11+10\bin\java.exe" set "JAVA_HOME=%~dp0.jdk\jdk-21.0.11+10"
if not defined JAVA_HOME if exist "%~dp0..\.jdk\jdk-21.0.11+10\bin\java.exe" set "JAVA_HOME=%~dp0..\.jdk\jdk-21.0.11+10"
if not defined JAVA_HOME (
  echo JAVA_HOME is not set. Install JDK 17+ or put it in .jdk\jdk-21.0.11+10.
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

call "%~dp0gradlew.bat" :app:installDebug
endlocal
