package com.preflight;

import com.preflight.checker.PdfDimensionChecker;
import com.preflight.config.PdfPreflightConfig;
import com.preflight.model.PageMismatch;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PdfDimensionChecker.
 * Tests dimension and orientation validation across various PDF scenarios.
 */
public class PdfDimensionCheckerTest {
    
    private final PdfDimensionChecker checker = new PdfDimensionChecker();
    
    @Test
    public void testEmptyPdf() throws IOException {
        // Empty PDF with no pages
        try (PDDocument document = new PDDocument()) {
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/empty.pdf").build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertTrue(mismatches.isEmpty(), "Empty PDF should have no mismatches");
        }
    }
    
    @Test
    public void testSinglePagePdf() throws IOException {
        // Single page PDF - no mismatches possible
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER));
            
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/single.pdf").build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertTrue(mismatches.isEmpty(), "Single page PDF should have no mismatches");
        }
    }
    
    @Test
    public void testMatchingPageSizes() throws IOException {
        // Multiple pages with same dimensions
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < 5; i++) {
                document.addPage(new PDPage(PDRectangle.LETTER));
            }
            
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/matching.pdf").build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertTrue(mismatches.isEmpty(), "All pages matching should result in no mismatches");
        }
    }
    
    @Test
    public void testMismatchedPageSizes() throws IOException {
        // Pages with different dimensions
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER)); // 612x792 (reference)
            document.addPage(new PDPage(PDRectangle.LETTER)); // 612x792 (match)
            document.addPage(new PDPage(PDRectangle.A4));     // 595x842 (mismatch)
            document.addPage(new PDPage(PDRectangle.LETTER)); // 612x792 (match)
            
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/mismatch.pdf").build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertEquals(1, mismatches.size(), "Should have exactly one mismatch");
            assertEquals(3, mismatches.get(0).getPageNumber(), "Mismatch should be on page 3");
            assertTrue(mismatches.get(0).getMismatchReason().contains("Width"), 
                "Should report width mismatch");
            assertTrue(mismatches.get(0).getMismatchReason().contains("Height"), 
                "Should report height mismatch");
        }
    }
    
    @Test
    public void testMismatchedOrientation() throws IOException {
        // Pages with different orientations
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER)); // portrait (reference)
            document.addPage(new PDPage(PDRectangle.LETTER)); // portrait (match)
            
            // Add landscape page (swapped dimensions)
            PDPage landscapePage = new PDPage();
            landscapePage.setMediaBox(new PDRectangle(792, 612));
            document.addPage(landscapePage);
            
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/orientation.pdf").build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertEquals(1, mismatches.size(), "Should have exactly one mismatch");
            assertEquals(3, mismatches.get(0).getPageNumber(), "Mismatch should be on page 3");
            assertTrue(mismatches.get(0).getMismatchReason().contains("Orientation"), 
                "Should report orientation mismatch");
            assertEquals("landscape", mismatches.get(0).getActualOrientation(), 
                "Actual orientation should be landscape");
            assertEquals("portrait", mismatches.get(0).getExpectedOrientation(), 
                "Expected orientation should be portrait");
        }
    }
    
    @Test
    public void testTolerance() throws IOException {
        // Pages with tiny differences within tolerance
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER)); // 612x792 (reference)
            
            // Page with very small difference (within default tolerance of 0.01)
            PDPage slightlyDifferentPage = new PDPage();
            slightlyDifferentPage.setMediaBox(new PDRectangle(612.005f, 792.005f));
            document.addPage(slightlyDifferentPage);
            
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/tolerance.pdf").build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertTrue(mismatches.isEmpty(), 
                "Differences within tolerance should not trigger mismatches");
        }
    }
    
    @Test
    public void testToleranceExceeded() throws IOException {
        // Pages with differences exceeding tolerance
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER)); // 612x792 (reference)
            
            // Page with difference exceeding tolerance
            PDPage differentPage = new PDPage();
            differentPage.setMediaBox(new PDRectangle(612.1f, 792.0f));
            document.addPage(differentPage);
            
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/tolerance.pdf")
                .tolerance(0.01f)
                .build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertEquals(1, mismatches.size(), 
                "Differences exceeding tolerance should trigger mismatches");
        }
    }
    
    @Test
    public void testCropBoxFallback() throws IOException {
        // Pages without CropBox should fall back to MediaBox
        try (PDDocument document = new PDDocument()) {
            // Page with only MediaBox (no CropBox set)
            document.addPage(new PDPage(PDRectangle.LETTER));
            document.addPage(new PDPage(PDRectangle.LETTER));
            
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/fallback.pdf")
                .useCropBox(true)
                .build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertTrue(mismatches.isEmpty(), 
                "Should use MediaBox when CropBox unavailable and match");
        }
    }
    
    @Test
    public void testUseMediaBox() throws IOException {
        // Explicitly use MediaBox instead of CropBox
        try (PDDocument document = new PDDocument()) {
            PDPage page1 = new PDPage(PDRectangle.LETTER);
            page1.setCropBox(new PDRectangle(0, 0, 500, 700)); // Different from MediaBox
            document.addPage(page1);
            
            PDPage page2 = new PDPage(PDRectangle.LETTER);
            page2.setCropBox(new PDRectangle(0, 0, 500, 700));
            document.addPage(page2);
            
            // Use MediaBox - should match
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/mediabox.pdf")
                .useCropBox(false)
                .build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertTrue(mismatches.isEmpty(), 
                "Using MediaBox should result in matching pages");
        }
    }
    
    @Test
    public void testMultipleMismatches() throws IOException {
        // Multiple pages with various mismatches
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER)); // reference
            document.addPage(new PDPage(PDRectangle.A4));     // size mismatch
            document.addPage(new PDPage(PDRectangle.LETTER)); // match
            
            PDPage landscapePage = new PDPage();
            landscapePage.setMediaBox(new PDRectangle(792, 612));
            document.addPage(landscapePage); // orientation mismatch
            
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/multi.pdf").build();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            
            assertEquals(2, mismatches.size(), "Should have two mismatches");
            assertEquals(2, mismatches.get(0).getPageNumber());
            assertEquals(4, mismatches.get(1).getPageNumber());
        }
    }
    
    @Test
    public void testLargeNumberOfPages() throws IOException {
        // Test performance with many pages
        try (PDDocument document = new PDDocument()) {
            int pageCount = 100;
            for (int i = 0; i < pageCount; i++) {
                document.addPage(new PDPage(PDRectangle.LETTER));
            }
            
            PdfPreflightConfig config = PdfPreflightConfig.builder("/tmp/large.pdf").build();
            long startTime = System.currentTimeMillis();
            List<PageMismatch> mismatches = checker.checkDimensions(document, config);
            long endTime = System.currentTimeMillis();
            
            assertTrue(mismatches.isEmpty(), "All matching pages should pass");
            assertTrue((endTime - startTime) < 1000, 
                "Should process 100 pages in under 1 second");
        }
    }
}
