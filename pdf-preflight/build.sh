#!/bin/bash

# PDF Preflight Build Script
# This script builds the project using Maven or Gradle if available,
# or provides instructions for manual compilation.

set -e

echo "========================================="
echo "  PDF Preflight Module Build Script"
echo "========================================="
echo ""

# Check for Maven
if command -v mvn &> /dev/null; then
    echo "Maven detected. Building with Maven..."
    mvn clean package -DskipTests
    echo ""
    echo "Build successful! JAR file created:"
    echo "  target/pdf-preflight-1.0.0.jar"
    echo ""
    echo "Run tests with:"
    echo "  mvn test"
    exit 0
fi

# Check for Gradle
if command -v gradle &> /dev/null; then
    echo "Gradle detected. Building with Gradle..."
    gradle clean build -x test
    echo ""
    echo "Build successful! JAR file created:"
    echo "  build/libs/pdf-preflight-1.0.0.jar"
    echo ""
    echo "Run tests with:"
    echo "  gradle test"
    exit 0
fi

echo "Neither Maven nor Gradle is installed."
echo ""
echo "Please install one of the following:"
echo ""
echo "Option 1: Maven (Recommended)"
echo "  macOS: brew install maven"
echo "  Ubuntu: sudo apt-get install maven"
echo "  Download: https://maven.apache.org/download.cgi"
echo ""
echo "Option 2: Gradle"
echo "  macOS: brew install gradle"
echo "  Ubuntu: sudo apt-get install gradle"
echo "  Download: https://gradle.org/install/"
echo ""
echo "After installation, run this script again."
exit 1
