@echo off
pwsh -NoProfile -File "%~dp0deploy.ps1" %*
exit /b %ERRORLEVEL%
