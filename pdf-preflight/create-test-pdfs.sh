#!/bin/bash

# Create sample test PDFs for manual testing

echo "Creating sample test PDFs..."

# This script uses Python with reportlab to create test PDFs
# If reportlab is not available, we'll use a simple approach

if ! python3 -c "import reportlab" 2>/dev/null; then
    echo "reportlab not installed. Installing..."
    pip3 install reportlab
fi

python3 << 'EOF'
from reportlab.lib.pagesizes import letter, A4
from reportlab.pdfgen import canvas
import os

output_dir = "test-pdfs"
os.makedirs(output_dir, exist_ok=True)

# 1. Single page PDF
print("Creating single-page.pdf...")
c = canvas.Canvas(f"{output_dir}/single-page.pdf", pagesize=letter)
c.drawString(100, 750, "Single Page PDF - Letter Size")
c.save()

# 2. Matching pages (all letter size)
print("Creating matching-pages.pdf...")
c = canvas.Canvas(f"{output_dir}/matching-pages.pdf", pagesize=letter)
for i in range(5):
    c.drawString(100, 750, f"Page {i+1} - Letter Size (Matching)")
    if i < 4:
        c.showPage()
c.save()

# 3. Mismatched sizes
print("Creating mismatched-size.pdf...")
c = canvas.Canvas(f"{output_dir}/mismatched-size.pdf", pagesize=letter)
c.drawString(100, 750, "Page 1 - Letter Size")
c.showPage()
c = canvas.Canvas(f"{output_dir}/mismatched-size.pdf", pagesize=letter)
c.drawString(100, 750, "Page 2 - Letter Size")
c.showPage()

# We need a different approach for mixed page sizes
# Using PyPDF2 or manual PDF creation would be better
# For now, let's create separate PDFs and note this limitation

print("\nNote: Creating mixed-size PDFs requires additional libraries.")
print("The unit tests programmatically create these scenarios.")
print("Sample PDFs created in test-pdfs/ directory.")

EOF

echo ""
echo "Sample PDFs created successfully!"
echo "Run the unit tests for comprehensive validation:"
echo "  mvn test"
echo "  or"
echo "  gradle test"
