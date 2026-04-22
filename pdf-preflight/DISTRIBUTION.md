# Distribution Guide - Pratt PDF File Checker

## 📦 Multi-Platform Support

Your app can be distributed as native installers for:
- ✅ **macOS** - DMG installer
- ✅ **Windows** - EXE installer
- ✅ **Linux** - DEB/RPM packages

## 🚀 Quick Build

### Build for Current Platform
```bash
./build-installer.sh        # macOS
build-installer.bat         # Windows
./build-installer-linux.sh  # Linux
```

### Build All Platforms (from macOS)
```bash
./build-all-platforms.sh
```
**Note**: This builds the JAR and macOS installer. Windows and Linux installers must be built on their respective platforms.

---

## Platform-Specific Instructions

### macOS ⭐

**Build**:
```bash
./build-installer.sh
```

**Output**: `dist/Pratt-PDF-Checker-1.0.0.dmg`

**User installs**:
1. Open DMG
2. Drag to Applications
3. Launch from Launchpad

**Size**: ~60-150 MB (includes JRE)

---

### Windows

**Build** (run on Windows):
```cmd
build-installer.bat
```

**Output**: `dist\Pratt-PDF-Checker-1.0.0.exe`

**User installs**:
1. Double-click EXE
2. Follow installation wizard
3. Launch from Start Menu or Desktop

**Features**:
- Desktop shortcut
- Start Menu entry
- Uninstaller in Control Panel

**Size**: ~100-150 MB (includes JRE)

---

### Linux

**Build** (run on Linux):
```bash
./build-installer-linux.sh
```

**Output**:
- **Debian/Ubuntu**: `dist/pratt-pdf-checker_1.0.0-1_amd64.deb`
- **RedHat/Fedora**: `dist/pratt-pdf-checker-1.0.0-1.x86_64.rpm`

**User installs**:
```bash
# Debian/Ubuntu
sudo dpkg -i pratt-pdf-checker_1.0.0-1_amd64.deb
sudo apt-get install -f

# RedHat/Fedora
sudo rpm -i pratt-pdf-checker-1.0.0-1.x86_64.rpm
```

**Features**:
- Application menu entry
- Desktop shortcut
- Command-line launcher: `pratt-pdf-checker`

**Size**: ~100-150 MB (includes JRE)

---

### Option 2: GraalVM Native Image

**Best for**: Minimal size, fastest startup

**Install GraalVM**:
```bash
# macOS
brew install --cask graalvm/tap/graalvm-ce-java17

# Set JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java17/Contents/Home
```

**Build**:
```bash
# Install native-image tool
gu install native-image

# Build native executable
native-image \
  --no-fallback \
  -jar target/pdf-preflight-1.0.0.jar \
  -H:Name=pratt-pdf-checker \
  -H:Class=com.preflight.web.PreflightWebServer \
  --allow-incomplete-classpath \
  --report-unsupported-elements-at-runtime \
  --initialize-at-build-time \
  -o pratt-pdf-checker
```

**Output**: `pratt-pdf-checker` (single executable)

**Size**: ~50-80 MB

**Note**: May require configuration files for PDFBox reflection.

---

### Option 3: Portable Bundle (Easiest)

**Best for**: Quick distribution, no build tools needed

**Create bundle**:
```bash
./create-portable-bundle.sh
```

**Output**: `Pratt-PDF-Checker-Portable.zip`

**Contains**:
- JAR file
- Minimal JRE (created with jlink)
- Launch scripts

**Size**: ~60 MB

**User runs**:
```bash
# Mac/Linux
./run.sh

# Windows
run.bat
```

---

## 🚀 Comparison

| Method | Size | User Setup | Build Complexity | Startup Speed |
|--------|------|------------|------------------|---------------|
| **jpackage** | ~150 MB | Install DMG | Easy | Fast |
| **GraalVM** | ~60 MB | Just run | Hard | Fastest |
| **Portable** | ~60 MB | Unzip & run | Easy | Medium |
| **JAR only** | ~17 MB | Install Java | None | Fast |

---

## 📋 Current State

Your app is already:
- ✅ Built as single JAR (17 MB)
- ✅ All dependencies included (uber jar)
- ✅ Ready to run: `java -jar pdf-preflight-1.0.0.jar`
- ✅ Only dependency: Java 11+

---

## 🎯 Recommendation

**For your use case** (PDF preflight tool):

1. **Start with jpackage** - Easy, professional, native installer
2. **Consider GraalVM later** - If you want smaller size and faster startup

---

## 🔧 Quick Test jpackage

```bash
cd pdf-preflight
./build-installer.sh
```

This will create a DMG installer in the `dist/` folder!
