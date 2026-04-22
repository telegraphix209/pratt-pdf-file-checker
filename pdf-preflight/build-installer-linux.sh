#!/bin/bash
# Build Linux installer using jpackage
# Requires Java 14+ on Linux

set -e

echo "========================================="
echo "  Building Pratt PDF Checker Installer"
echo "========================================="

# Check if we're on Linux
if [[ "$OSTYPE" != "linux-gnu"* ]]; then
    echo "WARNING: This script is designed for Linux systems."
    echo "Current OS: $OSTYPE"
    echo ""
    echo "For cross-compilation, you would need to run this on a Linux machine."
    echo ""
fi

# Clean and build
echo "Building JAR..."
mvn clean package -DskipTests -q

# Create output directory
OUTPUT_DIR="dist"
mkdir -p $OUTPUT_DIR

# Detect package type
if command -v dpkg-deb &> /dev/null; then
    PACKAGE_TYPE="deb"
    echo "Creating DEB package (Debian/Ubuntu)..."
elif command -v rpm &> /dev/null; then
    PACKAGE_TYPE="rpm"
    echo "Creating RPM package (RedHat/Fedora)..."
else
    PACKAGE_TYPE="app-image"
    echo "Creating Linux application image..."
fi

# Build Linux installer
echo "Creating Linux installer..."

jpackage --input target/ \
  --name "pratt-pdf-checker" \
  --main-jar pdf-preflight-1.0.0.jar \
  --main-class com.preflight.web.PreflightWebServer \
  --type $PACKAGE_TYPE \
  --app-version 1.0.0 \
  --description "PDF Preflight Validation & Print Preparation Tool" \
  --vendor "Pratt" \
  --copyright "Copyright © 2026" \
  --dest $OUTPUT_DIR \
  --linux-shortcut \
  --linux-menu-group "Office;Graphics;PDF" \
  --linux-app-category "Office" \
  --linux-deb-maintainer "support@pratt.edu" 2>/dev/null || \
jpackage --input target/ \
  --name "pratt-pdf-checker" \
  --main-jar pdf-preflight-1.0.0.jar \
  --main-class com.preflight.web.PreflightWebServer \
  --type $PACKAGE_TYPE \
  --app-version 1.0.0 \
  --description "PDF Preflight Validation & Print Preparation Tool" \
  --vendor "Pratt" \
  --dest $OUTPUT_DIR

echo "========================================="
echo "  Build Complete!"
echo "========================================="
echo ""

if [ "$PACKAGE_TYPE" = "deb" ]; then
    echo "Package created: $OUTPUT_DIR/pratt-pdf-checker_1.0.0-1_amd64.deb"
    echo ""
    echo "To install:"
    echo "  sudo dpkg -i $OUTPUT_DIR/pratt-pdf-checker_1.0.0-1_amd64.deb"
    echo "  sudo apt-get install -f  # if dependencies are missing"
    echo ""
elif [ "$PACKAGE_TYPE" = "rpm" ]; then
    echo "Package created: $OUTPUT_DIR/pratt-pdf-checker-1.0.0-1.x86_64.rpm"
    echo ""
    echo "To install:"
    echo "  sudo rpm -i $OUTPUT_DIR/pratt-pdf-checker-1.0.0-1.x86_64.rpm"
    echo ""
else
    echo "Application created: $OUTPUT_DIR/pratt-pdf-checker/"
    echo ""
    echo "To run:"
    echo "  $OUTPUT_DIR/pratt-pdf-checker/bin/pratt-pdf-checker"
    echo ""
fi
