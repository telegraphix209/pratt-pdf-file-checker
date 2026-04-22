@echo off
REM Build Windows installer using jpackage
REM Requires Java 14+ on Windows

echo =========================================
echo   Building Pratt PDF Checker Installer
echo =========================================

REM Clean and build
echo Building JAR...
call mvn clean package -DskipTests -q

REM Create output directory
if not exist "dist" mkdir dist

REM Build Windows installer
echo Creating Windows installer...

jpackage --input target/ ^
  --name "Pratt-PDF-Checker" ^
  --main-jar pdf-preflight-1.0.0.jar ^
  --main-class com.preflight.web.PreflightWebServer ^
  --type exe ^
  --app-version 1.0.0 ^
  --description "PDF Preflight Validation ^& Print Preparation Tool" ^
  --vendor "Pratt" ^
  --copyright "Copyright (c) 2026" ^
  --dest dist ^
  --win-dir-chooser ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "Pratt" ^
  --win-per-user-install

if %errorlevel% equ 0 (
    echo =========================================
    echo   Build Complete!
    echo =========================================
    echo.
    echo Installer created: dist\Pratt-PDF-Checker-1.0.0.exe
    echo.
    echo To install:
    echo   1. Double-click the EXE file
    echo   2. Follow the installation wizard
    echo   3. Launch from Start Menu or Desktop
    echo.
) else (
    echo.
    echo Build failed! Please check the error messages above.
    echo.
    pause
    exit /b %errorlevel%
)
