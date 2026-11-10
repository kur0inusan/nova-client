@echo off
rem Nova Client - 26.3 development client (loads straight into "New World" if it exists)
cd /d "%~dp0mod\v26"
call gradlew.bat runClient "-PquickPlay=New World" --console=plain
if errorlevel 1 pause
