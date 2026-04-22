package com.preflight.service;

import com.preflight.model.PdfPageAnalysis;
import com.preflight.model.PdfPreflightResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to prepare PDFs for print by fixing common issues found during preflight.
 * Uses PDFBox for fixes with optional MuPDF fallback.
 */
public class PdfPrintPreparationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfPrintPreparationService.class);
    
    /**
     * Result of print preparation operation.
     */
    public static class PrintPreparationResult {
        private final boolean success;
        private final String outputFilePath;
        private final List<String> fixesApplied;
        private final List<String> warnings;
        
        public PrintPreparationResult(boolean success, String outputFilePath, 
                                     List<String> fixesApplied, List<String> warnings) {
            this.success = success;
            this.outputFilePath = outputFilePath;
            this.fixesApplied = new ArrayList<>(fixesApplied);
            this.warnings = new ArrayList<>(warnings);
        }
        
        public boolean isSuccess() { return success; }
        public String getOutputFilePath() { return outputFilePath; }
        public List<String> getFixesApplied() { return fixesApplied; }
        public List<String> getWarnings() { return warnings; }
    }
    
    /**
     * Prepares a PDF for print by fixing issues identified in the preflight result.
     * 
     * @param result The preflight result containing page analyses
     * @return Result of the preparation operation
     */
    public PrintPreparationResult prepareForPrint(PdfPreflightResult result) {
        return prepareForPrint(result, null);
    }
    
    /**
     * Prepares a PDF for print by fixing issues identified in the preflight result.
     * 
     * @param result The preflight result containing page analyses
     * @param customOutputFileName Custom output filename (null to use default _PFv.2 suffix)
     * @return Result of the preparation operation
     */
    public PrintPreparationResult prepareForPrint(PdfPreflightResult result, String customOutputFileName) {
        String inputPath = result.getInputPdfPath();
        logger.info("Starting print preparation for: {}", inputPath);
        
        List<String> fixesApplied = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Load the PDF
            File inputFile = new File(inputPath);
            PDDocument document = PDDocument.load(inputFile);
            
            try {
                // Get reference dimensions from first page
                PDPage referencePage = document.getPage(0);
                PDRectangle refBox = referencePage.getMediaBox();
                
                // Fix page orientations
                int orientationFixes = fixPageOrientations(document, result.getPageAnalyses(), refBox);
                if (orientationFixes > 0) {
                    fixesApplied.add(String.format("Fixed orientation on %d page(s)", orientationFixes));
                }
                
                // Attempt to embed fonts
                int fontFixes = attemptFontEmbedding(document);
                if (fontFixes > 0) {
                    fixesApplied.add(String.format("Embedded %d font(s)", fontFixes));
                }
                
                // Note: RGB to CMYK conversion requires ICC profiles and is complex
                // For now, just warn about RGB content
                long rgbPages = result.getPageAnalyses().stream()
                    .filter(a -> a.getColorspaces().contains("RGB"))
                    .count();
                if (rgbPages > 0) {
                    warnings.add(String.format("%d page(s) contain RGB colorspaces - manual conversion recommended", rgbPages));
                }
                
                // Note: Low-res images cannot be upscaled
                long lowResImages = result.getPageAnalyses().stream()
                    .mapToLong(a -> a.getImages().stream().filter(PdfPageAnalysis.ImageInfo::isLowRes).count())
                    .sum();
                if (lowResImages > 0) {
                    warnings.add(String.format("%d low-resolution image(s) found - cannot be automatically fixed", lowResImages));
                }
                
                // Generate output filename
                String outputPath = generateOutputPath(inputPath, customOutputFileName);
                
                // Save the prepared PDF
                document.save(outputPath);
                logger.info("Print-ready PDF saved to: {}", outputPath);
                
                // Try MuPDF optimization if available
                boolean mupdfOptimized = attemptMuPdfOptimization(outputPath);
                if (mupdfOptimized) {
                    fixesApplied.add("Optimized with MuPDF");
                }
                
                return new PrintPreparationResult(true, outputPath, fixesApplied, warnings);
                
            } finally {
                document.close();
            }
            
        } catch (IOException e) {
            logger.error("Failed to prepare PDF for print: {}", e.getMessage(), e);
            return new PrintPreparationResult(false, null, fixesApplied, 
                List.of("Error: " + e.getMessage()));
        }
    }
    
    /**
     * Fixes page orientations to match the reference page.
     */
    private int fixPageOrientations(PDDocument document, List<PdfPageAnalysis> pageAnalyses, 
                                   PDRectangle refBox) {
        int fixes = 0;
        String refOrientation = refBox.getWidth() >= refBox.getHeight() ? "landscape" : "portrait";
        
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            PDPage page = document.getPage(i);
            PdfPageAnalysis analysis = pageAnalyses.get(i);
            
            // Check if orientation mismatches reference
            if (!analysis.getOrientation().equals(refOrientation)) {
                // Calculate rotation needed
                int currentRotation = page.getRotation();
                int newRotation = (currentRotation + 90) % 360;
                page.setRotation(newRotation);
                fixes++;
                
                logger.debug("Rotated page {} from {} to {}", i + 1, analysis.getOrientation(), refOrientation);
            }
        }
        
        return fixes;
    }
    
    /**
     * Attempts to embed unembedded fonts.
     * Note: PDFBox has limited font embedding capabilities. Some fonts cannot be embedded.
     */
    private int attemptFontEmbedding(PDDocument document) {
        int fixes = 0;
        
        // PDFBox doesn't easily allow embedding fonts after creation
        // This is a placeholder for future enhancement
        // In practice, font embedding should be done during PDF creation
        
        logger.debug("Font embedding skipped - requires PDF creation-time support");
        return fixes;
    }
    
    /**
     * Generates output file path with '_PFv.2' suffix or custom filename.
     */
    private String generateOutputPath(String inputPath, String customOutputFileName) {
        File inputFile = new File(inputPath);
        String parentDir = inputFile.getParent();
            
        String outputFileName;
        if (customOutputFileName != null && !customOutputFileName.trim().isEmpty()) {
            // Use custom filename
            outputFileName = customOutputFileName.trim();
            // Ensure it ends with .pdf
            if (!outputFileName.toLowerCase().endsWith(".pdf")) {
                outputFileName += ".pdf";
            }
        } else {
            // Use default _PFv.2 suffix
            String fileName = inputFile.getName();
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            String extension = fileName.substring(fileName.lastIndexOf('.'));
            outputFileName = nameWithoutExt + "_PFv.2" + extension;
        }
            
        if (parentDir != null) {
            return parentDir + File.separator + outputFileName;
        }
        return outputFileName;
    }
    
    /**
     * Attempts to optimize the PDF using MuPDF if available.
     */
    private boolean attemptMuPdfOptimization(String pdfPath) {
        try {
            // Check if mutool is available
            ProcessBuilder pb = new ProcessBuilder("mutool", "-v");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                logger.debug("MuPDF not available, skipping optimization");
                return false;
            }
            
            // Run mutool clean
            String tempPath = pdfPath + ".tmp";
            ProcessBuilder cleanPb = new ProcessBuilder("mutool", "clean", "-d", pdfPath, tempPath);
            Process cleanProcess = cleanPb.start();
            int cleanExitCode = cleanProcess.waitFor();
            
            if (cleanExitCode == 0) {
                // Replace original with optimized version
                File tempFile = new File(tempPath);
                File originalFile = new File(pdfPath);
                if (tempFile.exists()) {
                    originalFile.delete();
                    tempFile.renameTo(originalFile);
                    logger.debug("MuPDF optimization successful");
                    return true;
                }
            }
            
            return false;
            
        } catch (IOException | InterruptedException e) {
            logger.debug("MuPDF optimization failed: {}", e.getMessage());
            return false;
        }
    }
}
