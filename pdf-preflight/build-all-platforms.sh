#!/bin/bash
# Build installers for all platforms
# Note: Windows and Linux builds must be run on their respective platforms

set -e

echo "========================================="
echo "  Pratt PDF Checker - Multi-Platform Build"
echo "========================================="
echo ""

# Clean and build JAR first
echo "Building universal JAR..."
/tmp/apache-maven-3.9.6/bin/mvn clean package -DskipTests -q
echo "✅ JAR built: target/pdf-preflight-1.0.0.jar"
echo ""

# Create dist directory
mkdir -p dist

# Build for current platform
PLATFORM=$(uname -s)
echo "Detected platform: $PLATFORM"
echo ""

if [[ "$PLATFORM" == "Darwin" ]]; then
    echo "📦 Building macOS installer..."
    ./build-installer.sh
    echo ""
    echo "⚠️  Note: Windows and Linux installers must be built on their respective platforms"
    echo ""
    echo "To build for other platforms:"
    echo "  Windows: Run build-installer.bat on Windows"
    echo "  Linux:   Run build-installer-linux.sh on Linux"
    echo ""
elif [[ "$PLATFORM" == "Linux" ]]; then
    echo "📦 Building Linux installer..."
    ./build-installer-linux.sh
    echo ""
    echo "⚠️  Note: macOS and Windows installers must be built on their respective platforms"
    echo ""
    echo "To build for other platforms:"
    echo "  macOS:   Run build-installer.sh on macOS"
    echo "  Windows: Run build-installer.bat on Windows"
    echo ""
fi

# Show what was built
echo "========================================="
echo "  Build Summary"
echo "========================================="
echo ""
echo "Files in dist/:"
ls -lh dist/ 2>/dev/null || echo "  (no installers built yet)"
echo ""
echo "Universal JAR (works on all platforms):"
ls -lh target/pdf-preflight-1.0.0.jar
echo ""
echo "Distribution options:"
echo "  1. Share the JAR: java -jar pdf-preflight-1.0.0.jar"
echo "  2. Share platform-specific installers from dist/"
echo ""
