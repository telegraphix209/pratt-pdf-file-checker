# Pratt PDF File Checker

[![Build Status](https://github.com/telegraphix209/pratt-pdf-file-checker/actions/workflows/build-installers.yml/badge.svg)](https://github.com/telegraphix209/pratt-pdf-file-checker/actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

A professional PDF preflight validation and print preparation tool. Similar to commercial tools like callas pdfToolbox, designed to validate PDF documents for print readiness.

A production-minded Java-based PDF preflight engine for validating page dimensions and orientation consistency in large PDF files (up to 1GB).

## Overview

This tool performs preflight validation on PDF documents similar to a subset of callas pdfToolbox. It validates that all pages in a PDF have consistent dimensions and orientation, using low-memory page-by-page processing to handle very large files efficiently.

## Features

- **Dimension Validation**: Ensures all pages have the same width and height
- **Orientation Validation**: Ensures all pages have the same orientation (portrait/landscape)
- **Large File Support**: Handles PDFs up to 1GB+ using temp-file-backed memory settings
- **Configurable Tolerance**: Adjustable floating-point comparison tolerance
- **Flexible Box Selection**: Use CropBox (default) or MediaBox for measurements
- **Dual Report Output**: Generates both JSON (machine-readable) and text (human-readable) reports
- **Optional Rasterization**: Render failed pages using MuPDF CLI utilities
- **Modular Architecture**: Easy to extend with additional preflight checks
- **Clear Exit Codes**: 0 (pass), 1 (fail), 2 (error) for CI/CD integration

## 📦 Installation

### Option 1: Native Installers (Recommended - No Java Required)

Download from [GitHub Releases](https://github.com/telegraphix209/pratt-pdf-file-checker/releases):

**macOS**:
```bash
# Download the DMG
open Pratt-PDF-Checker-1.0.0.dmg
# Drag to Applications folder
```

**Windows**:
```bash
# Download the EXE installer
# Double-click to install
```

**Linux (Debian/Ubuntu)**:
```bash
sudo dpkg -i pratt-pdf-checker_1.0.0-1_amd64.deb
```

**Linux (RedHat/Fedora)**:
```bash
sudo rpm -i pratt-pdf-checker-1.0.0-1.x86_64.rpm
```

### Option 2: Run from JAR (Requires Java)

**Requirements**: Java 11 or higher

```bash
java -jar pdf-preflight-1.0.0.jar
```

### Option 3: Build from Source

```bash
git clone https://github.com/telegraphix209/pratt-pdf-file-checker.git
cd pdf-preflight

# Build JAR
mvn clean package -DskipTests

# Run
java -jar target/pdf-preflight-1.0.0.jar

# Or build native installer
./build-installer.sh        # macOS
build-installer.bat         # Windows
./build-installer-linux.sh  # Linux
```

## 🚀 Usage

---

## Prerequisites

- **Java 11 or higher** (OpenJDK or Oracle JDK)
- **Maven** or **Gradle** (for building)
- **MuPDF** (optional, for page rasterization)

### Installing Dependencies

**macOS:**
```bash
# Install Maven
brew install maven

# Install Gradle (alternative)
brew install gradle

# Install MuPDF (optional, for rasterization)
brew install mupdf-tools
```

**Ubuntu/Debian:**
```bash
# Install Maven
sudo apt-get install maven

# Install Gradle (alternative)
sudo apt-get install gradle

# Install MuPDF (optional, for rasterization)
sudo apt-get install mupdf-tools
```

## Installation

### Clone or Download

The project is self-contained in the `pdf-preflight` directory.

### Build the Project

**Using the build script (recommended):**
```bash
cd pdf-preflight
./build.sh
```

**Using Maven directly:**
```bash
cd pdf-preflight
mvn clean package
```

**Using Gradle:**
```bash
cd pdf-preflight
gradle clean build
```

The build creates an executable fat JAR with all dependencies included:
- Maven: `target/pdf-preflight-1.0.0.jar`
- Gradle: `build/libs/pdf-preflight-1.0.0.jar`

## Usage

### Basic Usage

Validate a PDF file with default settings:
```bash
java -jar pdf-preflight-1.0.0.jar --input document.pdf
```

### Command-Line Options

```
Required:
  --input <path>              Path to the PDF file to validate

Options:
  --output-json <path>        JSON report output path (default: preflight-report.json)
  --output-text <path>        Text report output path (default: preflight-report.txt)
  --use-mediabox              Use MediaBox instead of CropBox for measurements
  --tolerance <float>         Tolerance for dimension comparison in points (default: 0.01)
  --rasterize                 Enable rasterization of failed pages (requires MuPDF)
  --raster-dpi <int>          DPI for rasterization (default: 150)
  --mutool-path <path>        Path to mutool executable (default: mutool)
  --raster-pages <list>       Comma-separated page numbers to rasterize (default: all mismatched)
  --help, -h                  Show help message
```

### Examples

**Example 1: Basic validation**
```bash
java -jar pdf-preflight-1.0.0.jar --input large-document.pdf
```

**Example 2: Custom report paths**
```bash
java -jar pdf-preflight-1.0.0.jar \
  --input document.pdf \
  --output-json reports/preflight.json \
  --output-text reports/preflight.txt
```

**Example 3: Use MediaBox with custom tolerance**
```bash
java -jar pdf-preflight-1.0.0.jar \
  --input document.pdf \
  --use-mediabox \
  --tolerance 0.1
```

**Example 4: Enable rasterization of failed pages**
```bash
java -jar pdf-preflight-1.0.0.jar \
  --input document.pdf \
  --rasterize \
  --raster-dpi 300 \
  --mutool-path /usr/local/bin/mutool
```

**Example 5: Rasterize specific pages**
```bash
java -jar pdf-preflight-1.0.0.jar \
  --input document.pdf \
  --rasterize \
  --raster-pages 25,87,102
```

## Exit Codes

- **0**: PASS - All pages have matching dimensions and orientation
- **1**: FAIL - Mismatches found (reports generated)
- **2**: ERROR - Invalid PDF, file not found, or runtime error

## Output Reports

### JSON Report (Machine-Readable)

Generated by default as `preflight-report.json`:

```json
{
  "passed" : false,
  "totalPages" : 150,
  "processingTimeMs" : 1247,
  "exitCode" : 1,
  "inputPdf" : "/path/to/document.pdf",
  "referencePage" : {
    "pageNumber" : 1,
    "width" : 612.0,
    "height" : 792.0,
    "orientation" : "portrait",
    "boxUsed" : "CropBox"
  },
  "mismatchCount" : 2,
  "mismatches" : [ {
    "pageNumber" : 25,
    "actualWidth" : 595.0,
    "actualHeight" : 842.0,
    "actualOrientation" : "portrait",
    "expectedWidth" : 612.0,
    "expectedHeight" : 792.0,
    "expectedOrientation" : "portrait",
    "mismatchReason" : "Width mismatch: 595.00 != 612.00, Height mismatch: 842.00 != 792.00"
  }, {
    "pageNumber" : 87,
    "actualWidth" : 792.0,
    "actualHeight" : 612.0,
    "actualOrientation" : "landscape",
    "expectedWidth" : 612.0,
    "expectedHeight" : 792.0,
    "expectedOrientation" : "portrait",
    "mismatchReason" : "Orientation mismatch: landscape != portrait"
  } ]
}
```

### Text Report (Human-Readable)

Generated by default as `preflight-report.txt`:

```
=== PDF Preflight Report ===
Generated: 2026-04-20T10:30:45Z
Input: /path/to/document.pdf

Status: FAIL
Total Pages: 150

Reference Page: Page 1
  Width: 612.00 pts (8.50 inches)
  Height: 792.00 pts (11.00 inches)
  Orientation: portrait
  Box Used: CropBox

Processing Time: 1247 ms

Mismatches Found: 2

Page 25:
  Actual: 595.00 x 842.00 pts (portrait)
  Expected: 612.00 x 792.00 pts (portrait)
  Reason: Width mismatch: 595.00 != 612.00, Height mismatch: 842.00 != 792.00

Page 87:
  Actual: 792.00 x 612.00 pts (landscape)
  Expected: 612.00 x 792.00 pts (portrait)
  Reason: Orientation mismatch: landscape != portrait

Summary:
  Dimension mismatches: 1
  Orientation mismatches: 1
  Pages checked: 150
  Pages failed: 2
```

## Architecture

### Core Components

```
com.preflight
├── config
│   └── PdfPreflightConfig          # Configuration with builder pattern
├── model
│   ├── PdfPageInfo                 # Immutable page information
│   ├── PageMismatch                # Mismatch details
│   └── PdfPreflightResult          # Result container
├── checker
│   └── PdfDimensionChecker         # Dimension & orientation validation
├── report
│   ├── PdfReportWriter             # Report interface
│   ├── JsonReportWriter            # JSON report generation
│   └── TextReportWriter            # Text report generation
├── rasterizer
│   └── PdfRasterizer               # Optional MuPDF rasterization
├── service
│   └── PdfPreflightService         # Main orchestration service
└── PreflightCli                    # CLI entry point
```

### Design Decisions

1. **Combined Checking**: Dimension and orientation checks run in a single pass for efficiency
2. **Default Tolerance**: 0.01 points (~0.00014 inches) for floating-point comparison
3. **Orientation Rule**: width >= height → landscape, height > width → portrait
4. **Box Priority**: CropBox preferred, MediaBox fallback (configurable)
5. **Memory Management**: Temp-file-only mode for PDFBox to handle 1GB+ files
6. **Rasterization**: Completely optional and isolated from core logic
7. **Error Handling**: Fail-safe with descriptive messages and appropriate exit codes

## Performance Considerations

- **Memory Usage**: Uses `MemoryUsageSetting.setupTempFileOnly()` to avoid loading entire PDF into memory
- **Streaming**: Pages are processed sequentially using iterators, not loaded all at once
- **No Rendering**: Core validation does not render pages (unless explicitly requested)
- **Efficient Comparison**: Single-pass algorithm checks dimensions and orientation together

For a 500MB PDF with 1000 pages:
- Memory usage: < 256MB heap
- Processing time: Typically 2-5 seconds (depending on disk I/O)

## Running Tests

**Using Maven:**
```bash
mvn test
```

**Using Gradle:**
```bash
gradle test
```

### Test Coverage

- ✅ Empty PDF (0 pages)
- ✅ Single-page PDF
- ✅ Multiple pages with matching dimensions
- ✅ Multiple pages with mismatched dimensions
- ✅ Mixed orientation (portrait/landscape)
- ✅ CropBox to MediaBox fallback
- ✅ Custom tolerance settings
- ✅ Large number of pages (100+)
- ✅ Corrupt PDF handling
- ✅ Missing file handling

## Extending the Preflight Engine

The modular architecture makes it easy to add new validation checks:

### Adding a New Checker

1. Create a new checker class in `com.preflight.checker`:

```java
public class PdfColorSpaceChecker {
    public List<Issue> checkColorSpaces(PDDocument document, PdfPreflightConfig config) {
        // Implement your validation logic
        return issues;
    }
}
```

2. Integrate it into `PdfPreflightService`:

```java
PdfColorSpaceChecker colorChecker = new PdfColorSpaceChecker();
List<Issue> colorIssues = colorChecker.checkColorSpaces(document, config);
```

3. Add configuration options to `PdfPreflightConfig` if needed.

### Future Enhancement Ideas

- Font embedding validation
- Image resolution checks
- Color space validation (CMYK vs RGB)
- Transparency detection
- Bleed and trim box validation
- Metadata validation
- PDF/A compliance checks
- JavaScript detection
- Form field validation

## Troubleshooting

### "OutOfMemoryError" with very large PDFs

The tool is designed to handle large files with minimal memory. If you encounter memory issues:
- Ensure you're using Java 11+ (newer versions have better memory management)
- Increase heap size if needed: `java -Xmx512m -jar pdf-preflight-1.0.0.jar ...`
- Check available disk space (temp files are created during processing)

### "MuPDF not available" warning

This is only relevant if you enabled rasterization:
- Install MuPDF: `brew install mupdf-tools` (macOS) or `sudo apt-get install mupdf-tools` (Ubuntu)
- Specify custom path: `--mutool-path /custom/path/to/mutool`
- Rasterization is optional - preflight works fine without it

### Corrupt or encrypted PDFs

The tool handles these gracefully:
- Corrupt PDFs: Returns exit code 2 with error message
- Encrypted PDFs: Returns exit code 2 with "PDF is encrypted" message
- Missing files: Returns exit code 2 with "file not found" message

## License

This project uses only open-source libraries:
- Apache PDFBox (Apache License 2.0)
- Jackson (Apache License 2.0)
- SLF4J (MIT License)
- JUnit 5 (Eclipse Public License 2.0)

## Contributing

This is a starting point for a real preflight pipeline. Feel free to:
- Add new validation checks
- Improve performance
- Enhance report formats
- Add integration tests with real-world PDFs
- Support additional PDF standards (PDF/A, PDF/X, etc.)

## Support

For issues, questions, or contributions, please refer to the project repository.
