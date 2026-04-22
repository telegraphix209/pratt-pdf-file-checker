#!/bin/bash
# Build native installer using jpackage
# Requires Java 14+

set -e

echo "========================================="
echo "  Building Pratt PDF Checker Installer"
echo "========================================="

# Clean and build
echo "Building JAR..."
/tmp/apache-maven-3.9.6/bin/mvn clean package -DskipTests -q

# Create output directory
OUTPUT_DIR="dist"
mkdir -p $OUTPUT_DIR

# Build native installer
echo "Creating native installer..."

jpackage --input target/ \
  --name "Pratt-PDF-Checker" \
  --main-jar pdf-preflight-1.0.0.jar \
  --main-class com.preflight.web.PreflightWebServer \
  --type dmg \
  --app-version 1.0.0 \
  --description "PDF Preflight Validation & Print Preparation Tool" \
  --vendor "Pratt" \
  --copyright "Copyright © 2026" \
  --dest $OUTPUT_DIR \
  --mac-package-name "Pratt PDF Checker" \
  --mac-package-identifier "com.preflight.pdfchecker" \
  --icon src/main/resources/public/favicon.ico 2>/dev/null || \
jpackage --input target/ \
  --name "Pratt-PDF-Checker" \
  --main-jar pdf-preflight-1.0.0.jar \
  --main-class com.preflight.web.PreflightWebServer \
  --type dmg \
  --app-version 1.0.0 \
  --description "PDF Preflight Validation & Print Preparation Tool" \
  --vendor "Pratt" \
  --dest $OUTPUT_DIR

echo "========================================="
echo "  Build Complete!"
echo "========================================="
echo ""
echo "Installer created: $OUTPUT_DIR/Pratt-PDF-Checker-1.0.0.dmg"
echo ""
echo "To install:"
echo "  1. Open the DMG file"
echo "  2. Drag Pratt PDF Checker to Applications"
echo "  3. Launch from Applications folder"
echo ""
