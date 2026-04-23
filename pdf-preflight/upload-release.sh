#!/bin/bash
# Manual release script - Upload installers to GitHub Release
# Requires: GitHub personal access token with repo scope

set -e

GITHUB_USER="telegraphix209"
REPO="pratt-pdf-file-checker"
TAG="v1.0.0"
RELEASE_NOTES="Release v1.0.0 - Pratt PDF File Checker

🎉 First Production Release

## Features
- Automated PDF preflight validation with dimension & orientation checks
- Comprehensive page analysis: colorspaces, fonts, images, resolution
- Auto-run preflight on file upload
- Automatic print preparation when file passes validation
- Custom filename prompt before downloading print-ready PDF

## Installation
- macOS: Open Pratt-PDF-Checker-1.0.0.dmg, drag to Applications
- Windows: Run Pratt-PDF-Checker-1.0.0.exe installer
- Linux (Debian/Ubuntu): sudo dpkg -i pratt-pdf-checker_1.0.0-1_amd64.deb
- Linux (RedHat/Fedora): sudo rpm -i pratt-pdf-checker-1.0.0-1.x86_64.rpm"

echo "========================================="
echo "  Manual Release Upload"
echo "========================================="
echo ""

# Check if GITHUB_TOKEN is set
if [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ Error: GITHUB_TOKEN environment variable not set"
    echo ""
    echo "Please set your GitHub token:"
    echo "  export GITHUB_TOKEN=your_token_here"
    echo ""
    echo "Create a token at: https://github.com/settings/tokens"
    echo "  - Select 'repo' scope"
    exit 1
fi

# Create release (if it doesn't exist)
echo "📦 Creating release..."
RELEASE_RESPONSE=$(curl -s -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/repos/$GITHUB_USER/$REPO/releases \
  -d "{
    \"tag_name\": \"$TAG\",
    \"name\": \"Release $TAG - Pratt PDF File Checker\",
    \"body\": \"$RELEASE_NOTES\",
    \"draft\": false,
    \"prerelease\": false
  }")

RELEASE_ID=$(echo $RELEASE_RESPONSE | grep -o '"id": [0-9]*' | head -1 | grep -o '[0-9]*')

if [ -z "$RELEASE_ID" ]; then
    echo "⚠️  Release may already exist, trying to get it..."
    RELEASE_ID=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
      https://api.github.com/repos/$GITHUB_USER/$REPO/releases/tags/$TAG | grep -o '"id": [0-9]*' | head -1 | grep -o '[0-9]*')
fi

echo "✅ Release ID: $RELEASE_ID"
echo ""

# Upload macOS DMG
if [ -f "dist/Pratt-PDF-Checker-1.0.0.dmg" ]; then
    echo "📤 Uploading macOS DMG..."
    curl -s -X POST \
      -H "Authorization: token $GITHUB_TOKEN" \
      -H "Content-Type: application/octet-stream" \
      "https://uploads.github.com/repos/$GITHUB_USER/$REPO/releases/$RELEASE_ID/assets?name=Pratt-PDF-Checker-1.0.0.dmg" \
      --data-binary "@dist/Pratt-PDF-Checker-1.0.0.dmg"
    echo " ✅ macOS DMG uploaded"
else
    echo "⚠️  macOS DMG not found at dist/Pratt-PDF-Checker-1.0.0.dmg"
fi

echo ""
echo "========================================="
echo "  Upload Complete!"
echo "========================================="
echo ""
echo "View release at:"
echo "https://github.com/$GITHUB_USER/$REPO/releases/tag/$TAG"
echo ""
