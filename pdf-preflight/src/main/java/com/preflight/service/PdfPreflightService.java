package com.preflight.service;

import com.preflight.checker.PdfDimensionChecker;
import com.preflight.checker.PdfPageAnalyzer;
import com.preflight.config.PdfPreflightConfig;
import com.preflight.model.PageMismatch;
import com.preflight.model.PdfPageAnalysis;
import com.preflight.model.PdfPageInfo;
import com.preflight.model.PdfPreflightResult;
import com.preflight.rasterizer.PdfRasterizer;
import com.preflight.report.JsonReportWriter;
import com.preflight.report.TextReportWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Main orchestration service for PDF preflight operations.
 * Handles PDF loading, validation, report generation, and optional rasterization.
 */
public class PdfPreflightService {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfPreflightService.class);
    
    private final PdfDimensionChecker dimensionChecker;
    private final PdfPageAnalyzer pageAnalyzer;
    private final JsonReportWriter jsonReportWriter;
    private final TextReportWriter textReportWriter;
    
    public PdfPreflightService() {
        this.dimensionChecker = new PdfDimensionChecker();
        this.pageAnalyzer = new PdfPageAnalyzer();
        this.jsonReportWriter = new JsonReportWriter();
        this.textReportWriter = new TextReportWriter();
    }
    
    /**
     * Executes the preflight validation on the configured PDF.
     * 
     * @param config Configuration settings
     * @return Preflight result with pass/fail status and details
     */
    public PdfPreflightResult execute(PdfPreflightConfig config) {
        long startTime = System.currentTimeMillis();
        String inputPath = config.getInputPdfPath();
        
        logger.info("Starting preflight validation for: {}", inputPath);
        
        try {
            // Validate input file exists
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                return errorResult("Input file not found: " + inputPath, startTime, inputPath);
            }
            
            if (!inputFile.canRead()) {
                return errorResult("Input file is not readable: " + inputPath, startTime, inputPath);
            }
            
            // Load PDF with low-memory settings for large files
            PDDocument document;
            try {
                // Use temp-file-only mode to handle large PDFs without loading into memory
                document = PDDocument.load(inputFile, null, 
                    MemoryUsageSetting.setupTempFileOnly());
            } catch (IOException e) {
                return errorResult("Failed to load PDF: " + e.getMessage(), startTime, inputPath);
            }
            
            try {
                // Check for empty PDF
                int pageCount = document.getNumberOfPages();
                if (pageCount == 0) {
                    return errorResult("PDF contains no pages", startTime, inputPath);
                }
                
                logger.info("PDF loaded successfully. Total pages: {}", pageCount);
                
                // Get reference page information
                PdfPageInfo referencePage = getReferencePageInfo(document, config);
                
                // Run dimension and orientation checks
                List<PageMismatch> mismatches = dimensionChecker.checkDimensions(document, config);
                
                // Run comprehensive page analysis
                List<PdfPageAnalysis> pageAnalyses = new ArrayList<>();
                for (int i = 0; i < pageCount; i++) {
                    PDPage page = document.getPage(i);
                    PdfPageAnalysis analysis = pageAnalyzer.analyzePage(page, i + 1);
                    pageAnalyses.add(analysis);
                }
                
                // Determine pass/fail based on dimension mismatches and critical font issues
                boolean hasDimensionMismatches = !mismatches.isEmpty();
                boolean hasCriticalFontIssues = pageAnalyses.stream()
                    .anyMatch(PdfPageAnalysis::hasCriticalFontIssues);
                
                boolean passed = !hasDimensionMismatches && !hasCriticalFontIssues;
                long processingTime = System.currentTimeMillis() - startTime;
                
                logger.info("Preflight {} in {} ms. Mismatches: {}",
                    passed ? "PASSED" : "FAILED", processingTime, mismatches.size());
                
                // Create result
                PdfPreflightResult result = new PdfPreflightResult(
                    passed,
                    pageCount,
                    referencePage,
                    mismatches,
                    pageAnalyses,
                    processingTime,
                    inputPath
                );
                
                // Generate reports
                writeReports(result, config);
                
                // Optional rasterization
                if (config.isEnableRasterization() && !passed) {
                    rasterizeFailedPages(result, config);
                }
                
                return result;
                
            } finally {
                // Ensure document is closed to free resources
                document.close();
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during preflight: {}", e.getMessage(), e);
            return errorResult("Unexpected error: " + e.getMessage(), startTime, inputPath);
        }
    }
    
    /**
     * Gets reference page information from the first page.
     */
    private PdfPageInfo getReferencePageInfo(PDDocument document, PdfPreflightConfig config) {
        PDPage firstPage = document.getPage(0);
        PDRectangle box = getPageBox(firstPage, config.isUseCropBox());
        String boxName = getBoxName(firstPage, config.isUseCropBox());
        
        return new PdfPageInfo(1, box.getWidth(), box.getHeight(), boxName);
    }
    
    /**
     * Gets the appropriate page box based on configuration.
     */
    private PDRectangle getPageBox(PDPage page, boolean preferCropBox) {
        if (preferCropBox) {
            PDRectangle cropBox = page.getCropBox();
            if (cropBox != null && cropBox.getWidth() > 0 && cropBox.getHeight() > 0) {
                return cropBox;
            }
        }
        return page.getMediaBox();
    }
    
    private String getBoxName(PDPage page, boolean preferCropBox) {
        if (preferCropBox) {
            PDRectangle cropBox = page.getCropBox();
            if (cropBox != null && cropBox.getWidth() > 0 && cropBox.getHeight() > 0) {
                return "CropBox";
            }
        }
        return "MediaBox";
    }
    
    /**
     * Writes JSON and text reports to configured output paths.
     */
    private void writeReports(PdfPreflightResult result, PdfPreflightConfig config) {
        try {
            // Write JSON report
            String jsonPath = config.getOutputJsonPath();
            try (FileOutputStream fos = new FileOutputStream(jsonPath)) {
                jsonReportWriter.write(result, fos);
                logger.info("JSON report written to: {}", jsonPath);
            }
            
            // Write text report
            String textPath = config.getOutputTextPath();
            try (FileOutputStream fos = new FileOutputStream(textPath)) {
                textReportWriter.write(result, fos);
                logger.info("Text report written to: {}", textPath);
            }
            
        } catch (IOException e) {
            logger.error("Failed to write reports: {}", e.getMessage());
        }
    }
    
    /**
     * Rasterizes failed pages using MuPDF if enabled.
     */
    private void rasterizeFailedPages(PdfPreflightResult result, PdfPreflightConfig config) {
        try {
            PdfRasterizer rasterizer = new PdfRasterizer(config.getMuPdfToolPath());
            
            if (!rasterizer.isMuPdfAvailable()) {
                logger.warn("MuPDF tool not available at: {}. Skipping rasterization.",
                    config.getMuPdfToolPath());
                return;
            }
            
            // Determine which pages to rasterize
            int[] pagesToRasterize;
            if (config.getPagesToRasterize() != null && config.getPagesToRasterize().length > 0) {
                pagesToRasterize = config.getPagesToRasterize();
            } else {
                // Default: rasterize all mismatched pages
                pagesToRasterize = result.getMismatches().stream()
                    .mapToInt(PageMismatch::getPageNumber)
                    .toArray();
            }
            
            if (pagesToRasterize.length > 0) {
                logger.info("Rasterizing {} pages to {}", 
                    pagesToRasterize.length, config.getRasterOutputDir());
                
                boolean success = rasterizer.rasterizePages(
                    config.getInputPdfPath(),
                    pagesToRasterize,
                    config.getRasterOutputDir(),
                    config.getRasterDpi()
                );
                
                if (success) {
                    logger.info("Rasterization completed successfully");
                } else {
                    logger.warn("Rasterization failed");
                }
            }
            
        } catch (Exception e) {
            logger.error("Rasterization failed: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Creates an error result.
     */
    private PdfPreflightResult errorResult(String message, long startTime, String inputPath) {
        long processingTime = System.currentTimeMillis() - startTime;
        logger.error("Preflight error: {}", message);
        return new PdfPreflightResult(message, processingTime, inputPath);
    }
}
