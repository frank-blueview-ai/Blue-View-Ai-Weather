@echo off
REM ─────────────────────────────────────────────────────────────────────────────
REM Blue View AI Weather — Windows MSI builder
REM Copyright (c) 2026 BlueView / Frank Perez. All rights reserved.
REM
REM Requirements (run once, in an admin PowerShell):
REM   pip install cx_Freeze PyQt6 PyQt6-WebEngine
REM
REM Usage:  Double-click this file, or run from a command prompt
REM Output: dist\Blue-View-AI-Weather-1.0.0-amd64.msi
REM ─────────────────────────────────────────────────────────────────────────────

echo.
echo  Blue View AI Weather — Windows MSI Builder
echo  -------------------------------------------

REM Make sure we're in the script's directory
cd /d "%~dp0"

REM Install / update dependencies
echo  Installing dependencies...
pip install --quiet cx_Freeze PyQt6 PyQt6-WebEngine

REM Build the MSI
echo  Building MSI...
python setup_win.py bdist_msi

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo  ERROR: Build failed. Check the output above.
    pause
    exit /b 1
)

echo.
echo  Done! Installer is in the dist\ folder.
echo  Run it to install Blue View AI Weather on this machine.
echo.
pause
