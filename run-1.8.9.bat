@echo off
rem Nova Client - 1.8.9 development client
cd /d "%~dp0mod\v1_8_9"
call gradlew.bat runClient --console=plain
if errorlevel 1 pause
