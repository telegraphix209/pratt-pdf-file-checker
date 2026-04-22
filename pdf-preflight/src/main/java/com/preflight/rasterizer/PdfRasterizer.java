package com.preflight.rasterizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional rasterizer that uses MuPDF CLI utilities to render PDF pages as images.
 * Completely isolated from core preflight logic - failures here don't affect preflight results.
 */
public class PdfRasterizer {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfRasterizer.class);
    
    private final String muPdfToolPath;
    
    public PdfRasterizer(String muPdfToolPath) {
        this.muPdfToolPath = muPdfToolPath;
    }
    
    /**
     * Rasterizes specified pages from a PDF file.
     * 
     * @param inputPdfPath Path to the input PDF
     * @param pageNumbers Array of page numbers to rasterize (1-based)
     * @param outputDir Directory to save rendered images
     * @param dpi Resolution for rendering
     * @return true if rasterization succeeded, false otherwise
     */
    public boolean rasterizePages(String inputPdfPath, int[] pageNumbers, 
                                  String outputDir, int dpi) {
        try {
            // Create output directory if it doesn't exist
            Path outputDirPath = Paths.get(outputDir);
            if (!Files.exists(outputDirPath)) {
                Files.createDirectories(outputDirPath);
            }
            
            // Build page range string for mutool
            String pageRange = buildPageRange(pageNumbers);
            
            // Build command: mutool draw -o output/page_%d.png -r dpi input.pdf pages
            List<String> command = new ArrayList<>();
            command.add(muPdfToolPath);
            command.add("draw");
            command.add("-o");
            command.add(Paths.get(outputDir, "page_%d.png").toString());
            command.add("-r");
            command.add(String.valueOf(dpi));
            command.add(inputPdfPath);
            command.add(pageRange);
            
            logger.info("Executing MuPDF command: {}", String.join(" ", command));
            
            // Execute command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Wait for completion
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("Successfully rasterized {} pages", pageNumbers.length);
                return true;
            } else {
                logger.error("MuPDF exited with code {}: {}", exitCode, output.toString());
                return false;
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to rasterize pages: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
    
    /**
     * Builds a page range string from an array of page numbers.
     * MuPDF accepts comma-separated page numbers or ranges.
     */
    private String buildPageRange(int[] pageNumbers) {
        if (pageNumbers == null || pageNumbers.length == 0) {
            return "1";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pageNumbers.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(pageNumbers[i]);
        }
        return sb.toString();
    }
    
    /**
     * Checks if MuPDF tool is available and accessible.
     */
    public boolean isMuPdfAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(muPdfToolPath, "-v");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            logger.debug("MuPDF not available: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
