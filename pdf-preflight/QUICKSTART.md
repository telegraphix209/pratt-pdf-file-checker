# Quick Start Guide

## Prerequisites

- Java 11 or higher
- Maven or Gradle

## Build

```bash
cd pdf-preflight

# Option 1: Use build script
./build.sh

# Option 2: Maven
mvn clean package

# Option 3: Gradle
gradle clean build
```

## Run

```bash
# Basic usage
java -jar target/pdf-preflight-1.0.0.jar --input your-document.pdf

# With custom options
java -jar target/pdf-preflight-1.0.0.jar \
  --input your-document.pdf \
  --output-json report.json \
  --output-text report.txt \
  --use-mediabox \
  --tolerance 0.1
```

## Test

```bash
# Maven
mvn test

# Gradle
gradle test
```

## Exit Codes

- `0` - PASS (all pages match)
- `1` - FAIL (mismatches found)
- `2` - ERROR (invalid file or runtime error)

## Example Output

```
========================================
  PDF Preflight Result
========================================

Status: FAIL ✗
Total Pages: 150
Mismatches: 2

Reference (Page 1):
  612.00 x 792.00 pts (portrait)

Processing Time: 1247 ms
JSON Report: preflight-report.json
Text Report: preflight-report.txt
```

## Next Steps

1. Read the full README.md for detailed documentation
2. Review the sample JSON and text reports
3. Explore the source code in src/main/java/com/preflight/
4. Run the unit tests to see all validation scenarios
5. Extend with custom preflight checks as needed
