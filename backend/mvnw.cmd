@echo off
setlocal
set "MVNW_DIR=%~dp0"
if "%MVNW_DIR:~-1%"=="\" set "MVNW_DIR=%MVNW_DIR:~0,-1%"
set "WRAPPER_JAR=%MVNW_DIR%\.mvn\wrapper\maven-wrapper.jar"
if exist "%WRAPPER_JAR%" (
  java -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MVNW_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
  exit /b %ERRORLEVEL%
)
mvn %*
