@echo off
setlocal

if not exist gradlew.bat (
  echo ERROR: gradlew.bat not found in current directory.
  exit /b 1
)
set /p VERSION="Enter version (e.g. 0.0.9): "
if "%VERSION%"=="" (
  echo ERROR: Version cannot be empty.
  exit /b 1
)

call gradlew.bat publishAllToNewMavenCentralApi --info -Psign=true ^
  -PprojVersion=%VERSION% ^
  -PsigningKeyPassword="%SIGNING_KEY_PASSWORD%" ^
  -PcentralApiUsername="%MAVENCENTRAL_USERNAME%" ^
  -PcentralApiPassword="%MAVENCENTRAL_PASSWORD%" ^
  -PsigningKey="%SIGNING_KEY%" ^
  -PautomaticPublish=true ^
  -PwaitForPublished=false

exit /b %ERRORLEVEL%
