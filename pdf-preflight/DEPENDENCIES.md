# Dependencies

## Production Dependencies

### Apache PDFBox 2.0.30
- **Group**: org.apache.pdfbox
- **Artifact**: pdfbox, pdfbox-tools
- **License**: Apache License 2.0
- **Purpose**: PDF parsing, page box extraction, low-memory document loading
- **Website**: https://pdfbox.apache.org/

### Jackson Databind 2.15.3
- **Group**: com.fasterxml.jackson.core
- **Artifact**: jackson-databind
- **License**: Apache License 2.0
- **Purpose**: JSON report generation
- **Website**: https://github.com/FasterXML/jackson

### SLF4J 2.0.9
- **Group**: org.slf4j
- **Artifact**: slf4j-api, slf4j-simple
- **License**: MIT License
- **Purpose**: Logging framework
- **Website**: https://www.slf4j.org/

## Test Dependencies

### JUnit Jupiter 5.10.1
- **Group**: org.junit.jupiter
- **Artifact**: junit-jupiter
- **License**: Eclipse Public License 2.0
- **Purpose**: Unit testing framework
- **Website**: https://junit.org/junit5/

## Optional Dependencies

### MuPDF Tools
- **Package**: mupdf-tools
- **License**: GNU AGPL v3
- **Purpose**: Optional PDF page rasterization
- **Website**: https://mupdf.com/
- **Note**: Not required for core preflight functionality

## Dependency Tree

```
com.preflight:pdf-preflight:1.0.0
├── org.apache.pdfbox:pdfbox:2.0.30
│   ├── org.apache.pdfbox:fontbox:2.0.30
│   ├── org.apache.pdfbox:xmpbox:2.0.30
│   └── commons-logging:commons-logging:1.2
├── org.apache.pdfbox:pdfbox-tools:2.0.30
│   ├── org.apache.pdfbox:pdfbox:2.0.30
│   ├── org.apache.pdfbox:pdfbox-debugger:2.0.30
│   └── org.bouncycastle:bcprov-jdk15on:1.70
├── com.fasterxml.jackson.core:jackson-databind:2.15.3
│   ├── com.fasterxml.jackson.core:jackson-core:2.15.3
│   └── com.fasterxml.jackson.core:jackson-annotations:2.15.3
├── org.slf4j:slf4j-api:2.0.9
├── org.slf4j:slf4j-simple:2.0.9
│   └── org.slf4j:slf4j-api:2.0.9
└── org.junit.jupiter:junit-jupiter:5.10.1 (test)
    ├── org.junit.jupiter:junit-jupiter-api:5.10.1
    ├── org.junit.jupiter:junit-jupiter-params:5.10.1
    └── org.junit.jupiter:junit-jupiter-engine:5.10.1
```

## License Compatibility

All production dependencies use permissive open-source licenses:
- Apache License 2.0: PDFBox, Jackson
- MIT License: SLF4J

This project can be used in commercial applications without license conflicts.

## Memory Requirements

- **Minimum heap**: 256MB
- **Recommended heap**: 512MB (for large PDFs)
- **Temp disk space**: 2-3x PDF file size (for temp-file mode)

## Java Version

- **Minimum**: Java 11 (LTS)
- **Tested with**: Java 11, 17, 21, 25
- **Recommended**: Java 17 or 21 (LTS versions)
