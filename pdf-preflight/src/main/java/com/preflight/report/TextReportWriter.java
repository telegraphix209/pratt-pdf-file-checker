package com.preflight.report;

import com.preflight.model.PageMismatch;
import com.preflight.model.PdfPageInfo;
import com.preflight.model.PdfPreflightResult;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes preflight results as human-readable text.
 */
public class TextReportWriter implements PdfReportWriter {
    
    @Override
    public void write(PdfPreflightResult result, OutputStream out) throws IOException {
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println("=== PDF Preflight Report ===");
            writer.println("Generated: " + ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT));
            writer.println("Input: " + result.getInputPdfPath());
            writer.println();
            
            if (result.getErrorMessage() != null) {
                writer.println("Status: ERROR");
                writer.println("Error: " + result.getErrorMessage());
                writer.println();
                writer.println("Processing Time: " + result.getProcessingTimeMs() + " ms");
            } else {
                writer.println("Status: " + (result.isPassed() ? "PASS" : "FAIL"));
                writer.println("Total Pages: " + result.getTotalPages());
                writer.println();
                
                // Reference page details
                if (result.getReferencePage() != null) {
                    PdfPageInfo ref = result.getReferencePage();
                    writer.println("Reference Page: Page " + ref.getPageNumber());
                    writer.println(String.format("  Width: %.2f pts (%.2f inches)", 
                        ref.getWidth(), ref.getWidth() / 72.0));
                    writer.println(String.format("  Height: %.2f pts (%.2f inches)", 
                        ref.getHeight(), ref.getHeight() / 72.0));
                    writer.println("  Orientation: " + ref.getOrientation());
                    writer.println("  Box Used: " + ref.getBoxUsed());
                    writer.println();
                }
                
                writer.println("Processing Time: " + result.getProcessingTimeMs() + " ms");
                writer.println();
                
                // Mismatches
                writer.println("Mismatches Found: " + result.getMismatchCount());
                writer.println();
                
                if (!result.getMismatches().isEmpty()) {
                    int dimensionMismatches = 0;
                    int orientationMismatches = 0;
                    
                    for (PageMismatch mismatch : result.getMismatches()) {
                        writer.println("Page " + mismatch.getPageNumber() + ":");
                        writer.println(String.format("  Actual: %.2f x %.2f pts (%s)",
                            mismatch.getActualWidth(),
                            mismatch.getActualHeight(),
                            mismatch.getActualOrientation()));
                        writer.println(String.format("  Expected: %.2f x %.2f pts (%s)",
                            mismatch.getExpectedWidth(),
                            mismatch.getExpectedHeight(),
                            mismatch.getExpectedOrientation()));
                        writer.println("  Reason: " + mismatch.getMismatchReason());
                        writer.println();
                        
                        // Count mismatch types
                        if (mismatch.getMismatchReason().contains("Width") || 
                            mismatch.getMismatchReason().contains("Height")) {
                            dimensionMismatches++;
                        }
                        if (mismatch.getMismatchReason().contains("Orientation")) {
                            orientationMismatches++;
                        }
                    }
                    
                    // Summary
                    writer.println("Summary:");
                    writer.println("  Dimension mismatches: " + dimensionMismatches);
                    writer.println("  Orientation mismatches: " + orientationMismatches);
                    writer.println("  Pages checked: " + result.getTotalPages());
                    writer.println("  Pages failed: " + result.getMismatchCount());
                }
            }
            
            writer.flush();
        }
    }
}
