package com.preflight;

import com.preflight.config.PdfPreflightConfig;
import com.preflight.model.PdfPreflightResult;
import com.preflight.service.PdfPreflightService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PdfPreflightService.
 * Tests the complete preflight workflow including file I/O and report generation.
 */
public class PdfPreflightServiceTest {
    
    private final PdfPreflightService service = new PdfPreflightService();
    
    @TempDir
    Path tempDir;
    
    @Test
    public void testMissingInputFile() {
        PdfPreflightConfig config = PdfPreflightConfig.builder("/nonexistent/file.pdf")
            .outputJsonPath(tempDir.resolve("report.json").toString())
            .outputTextPath(tempDir.resolve("report.txt").toString())
            .build();
        
        PdfPreflightResult result = service.execute(config);
        
        assertEquals(2, result.getExitCode(), "Missing file should return error exit code");
        assertFalse(result.isPassed(), "Should not pass");
        assertNotNull(result.getErrorMessage(), "Should have error message");
        assertTrue(result.getErrorMessage().contains("not found"), 
            "Error message should indicate file not found");
    }
    
    @Test
    public void testEmptyPdf() throws IOException {
        // Create empty PDF
        File pdfFile = tempDir.resolve("empty.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            document.save(pdfFile);
        }
        
        PdfPreflightConfig config = PdfPreflightConfig.builder(pdfFile.getAbsolutePath())
            .outputJsonPath(tempDir.resolve("report.json").toString())
            .outputTextPath(tempDir.resolve("report.txt").toString())
            .build();
        
        PdfPreflightResult result = service.execute(config);
        
        assertEquals(2, result.getExitCode(), "Empty PDF should return error exit code");
        assertNotNull(result.getErrorMessage(), "Should have error message");
        assertTrue(result.getErrorMessage().contains("no pages"), 
            "Error message should indicate no pages");
    }
    
    @Test
    public void testSinglePagePdf() throws IOException {
        // Create single-page PDF
        File pdfFile = tempDir.resolve("single.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER));
            document.save(pdfFile);
        }
        
        PdfPreflightConfig config = PdfPreflightConfig.builder(pdfFile.getAbsolutePath())
            .outputJsonPath(tempDir.resolve("report.json").toString())
            .outputTextPath(tempDir.resolve("report.txt").toString())
            .build();
        
        PdfPreflightResult result = service.execute(config);
        
        assertEquals(0, result.getExitCode(), "Single page PDF should pass");
        assertTrue(result.isPassed(), "Should pass");
        assertEquals(1, result.getTotalPages(), "Should have 1 page");
        assertEquals(0, result.getMismatchCount(), "Should have no mismatches");
    }
    
    @Test
    public void testMatchingPagesPdf() throws IOException {
        // Create PDF with matching pages
        File pdfFile = tempDir.resolve("matching.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < 10; i++) {
                document.addPage(new PDPage(PDRectangle.LETTER));
            }
            document.save(pdfFile);
        }
        
        PdfPreflightConfig config = PdfPreflightConfig.builder(pdfFile.getAbsolutePath())
            .outputJsonPath(tempDir.resolve("report.json").toString())
            .outputTextPath(tempDir.resolve("report.txt").toString())
            .build();
        
        PdfPreflightResult result = service.execute(config);
        
        assertEquals(0, result.getExitCode(), "Matching pages should pass");
        assertTrue(result.isPassed(), "Should pass");
        assertEquals(10, result.getTotalPages(), "Should have 10 pages");
        assertEquals(0, result.getMismatchCount(), "Should have no mismatches");
        
        // Verify reports were created
        assertTrue(new File(config.getOutputJsonPath()).exists(), "JSON report should exist");
        assertTrue(new File(config.getOutputTextPath()).exists(), "Text report should exist");
    }
    
    @Test
    public void testMismatchedPagesPdf() throws IOException {
        // Create PDF with mismatched pages
        File pdfFile = tempDir.resolve("mismatched.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER));  // reference
            document.addPage(new PDPage(PDRectangle.LETTER));  // match
            document.addPage(new PDPage(PDRectangle.A4));      // mismatch
            document.addPage(new PDPage(PDRectangle.LETTER));  // match
            document.save(pdfFile);
        }
        
        PdfPreflightConfig config = PdfPreflightConfig.builder(pdfFile.getAbsolutePath())
            .outputJsonPath(tempDir.resolve("report.json").toString())
            .outputTextPath(tempDir.resolve("report.txt").toString())
            .build();
        
        PdfPreflightResult result = service.execute(config);
        
        assertEquals(1, result.getExitCode(), "Mismatched pages should fail");
        assertFalse(result.isPassed(), "Should not pass");
        assertEquals(4, result.getTotalPages(), "Should have 4 pages");
        assertEquals(1, result.getMismatchCount(), "Should have 1 mismatch");
        assertEquals(3, result.getMismatches().get(0).getPageNumber(), 
            "Mismatch should be on page 3");
    }
    
    @Test
    public void testMixedOrientationPdf() throws IOException {
        // Create PDF with mixed orientations
        File pdfFile = tempDir.resolve("mixed-orientation.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER)); // portrait
            
            PDPage landscapePage = new PDPage();
            landscapePage.setMediaBox(new PDRectangle(792, 612));
            document.addPage(landscapePage); // landscape
            
            document.addPage(new PDPage(PDRectangle.LETTER)); // portrait
            document.save(pdfFile);
        }
        
        PdfPreflightConfig config = PdfPreflightConfig.builder(pdfFile.getAbsolutePath())
            .outputJsonPath(tempDir.resolve("report.json").toString())
            .outputTextPath(tempDir.resolve("report.txt").toString())
            .build();
        
        PdfPreflightResult result = service.execute(config);
        
        assertEquals(1, result.getExitCode(), "Mixed orientation should fail");
        assertFalse(result.isPassed(), "Should not pass");
        assertEquals(1, result.getMismatchCount(), "Should have 1 mismatch");
        assertTrue(result.getMismatches().get(0).getMismatchReason().contains("Orientation"),
            "Should report orientation mismatch");
    }
    
    @Test
    public void testCorruptPdf() throws IOException {
        // Create a corrupt PDF file
        File pdfFile = tempDir.resolve("corrupt.pdf").toFile();
        java.nio.file.Files.write(pdfFile.toPath(), 
            "This is not a valid PDF file".getBytes());
        
        PdfPreflightConfig config = PdfPreflightConfig.builder(pdfFile.getAbsolutePath())
            .outputJsonPath(tempDir.resolve("report.json").toString())
            .outputTextPath(tempDir.resolve("report.txt").toString())
            .build();
        
        PdfPreflightResult result = service.execute(config);
        
        assertEquals(2, result.getExitCode(), "Corrupt PDF should return error");
        assertNotNull(result.getErrorMessage(), "Should have error message");
    }
    
    @Test
    public void testCustomTolerance() throws IOException {
        // Create PDF with small differences
        File pdfFile = tempDir.resolve("tolerance.pdf").toFile();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER));
            
            PDPage slightlyDifferent = new PDPage();
            slightlyDifferent.setMediaBox(new PDRectangle(612.05f, 792.0f));
            document.addPage(slightlyDifferent);
            
            document.save(pdfFile);
        }
        
        // With tight tolerance, should fail
        PdfPreflightConfig configTight = PdfPreflightConfig.builder(pdfFile.getAbsolutePath())
            .outputJsonPath(tempDir.resolve("report-tight.json").toString())
            .outputTextPath(tempDir.resolve("report-tight.txt").toString())
            .tolerance(0.01f)
            .build();
        
        PdfPreflightResult resultTight = service.execute(configTight);
        assertEquals(1, resultTight.getExitCode(), "Should fail with tight tolerance");
        
        // With loose tolerance, should pass
        PdfPreflightConfig configLoose = PdfPreflightConfig.builder(pdfFile.getAbsolutePath())
            .outputJsonPath(tempDir.resolve("report-loose.json").toString())
            .outputTextPath(tempDir.resolve("report-loose.txt").toString())
            .tolerance(0.1f)
            .build();
        
        PdfPreflightResult resultLoose = service.execute(configLoose);
        assertEquals(0, resultLoose.getExitCode(), "Should pass with loose tolerance");
    }
}
