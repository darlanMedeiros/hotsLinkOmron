@echo off
setlocal

set APP_DIR=%~dp0
set JAR=%APP_DIR%api-1.0-SNAPSHOT.jar

if not exist "%JAR%" (
  echo Arquivo nao encontrado: %JAR%
  exit /b 1
)

if "%JAVA_HOME%"=="" (
  set JAVA_CMD=java
) else (
  set JAVA_CMD=%JAVA_HOME%\bin\java
)

%JAVA_CMD% -jar "%JAR%"
