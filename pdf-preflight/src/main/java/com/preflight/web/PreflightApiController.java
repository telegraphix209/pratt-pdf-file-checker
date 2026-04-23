package com.preflight.web;

import com.preflight.config.PdfPreflightConfig;
import com.preflight.model.PdfPreflightResult;
import com.preflight.rasterizer.PdfRasterizer;
import com.preflight.service.PdfPreflightService;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static spark.Spark.*;

/**
 * REST API controller for PDF Preflight operations.
 * Handles file upload, validation, and report generation.
 */
public class PreflightApiController {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final long MAX_FILE_SIZE = 1024L * 1024 * 1024; // 1GB
    private static PdfRasterizer rasterizer = null;
    
    public static void setupRoutes() {
        
        // Enable multipart config before processing upload routes
        before("/api/*", (request, response) -> {
            if (request.contentType() != null && request.contentType().contains("multipart/")) {
                MultipartConfigElement config = new MultipartConfigElement(
                    System.getProperty("java.io.tmpdir"),
                    1073741824L,   // 1GB max file size
                    1073741824L,   // 1GB max request size
                    1048576        // 1MB threshold
                );
                request.raw().setAttribute("org.eclipse.jetty.multipartConfig", config);
            }
        });
        
        
        // POST /api/preflight - Upload PDF and run validation
        post("/api/preflight", (request, response) -> {
            response.type("application/json");
            return handlePreflight(request, response);
        });
        
        // POST /api/export/json - Export JSON report
        post("/api/export/json", (request, response) -> {
            response.type("application/json");
            return exportJsonReport(request, response);
        });
        
        // POST /api/export/text - Export text report
        post("/api/export/text", (request, response) -> {
            response.type("text/plain");
            return exportTextReport(request, response);
        });
        
        // POST /api/prepare-for-print - Prepare PDF for print
        post("/api/prepare-for-print", (request, response) -> {
            response.type("application/json");
            return handlePrepareForPrint(request, response);
        });
        
        // GET /api/download/:filename - Download prepared PDF
        get("/api/download/:filename", (request, response) -> {
            return handleDownload(request, response);
        });
        
        // POST /api/download-file - Download file from path
        post("/api/download-file", (request, response) -> {
            return handleDownloadFile(request, response);
        });
        
        // GET /api/rasterizer/status - Check if MuPDF is available
        get("/api/rasterizer/status", (request, response) -> {
            response.type("application/json");
            return checkRasterizerStatus(request, response);
        });
        
        // POST /api/rasterize - Rasterize PDF pages
        post("/api/rasterize", (request, response) -> {
            response.type("application/json");
            return handleRasterize(request, response);
        });
        
        // Error handlers
        exception(Exception.class, PreflightApiController::handleException);
    }
    
    /**
     * Handles PDF preflight validation request.
     * Accepts multipart file upload and configuration.
     */
    private static String handlePreflight(Request request, Response response) {
        long startTime = System.currentTimeMillis();
        File tempFile = null;
        
        try {
            // Parse configuration from form data
            boolean useCropBox = Boolean.parseBoolean(request.queryParams("useCropBox") != null ? 
                request.queryParams("useCropBox") : "true");
            float tolerance = Float.parseFloat(request.queryParams("tolerance") != null ? 
                request.queryParams("tolerance") : "0.01");
            
            // Get uploaded file
            Part filePart = request.raw().getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                throw new IllegalArgumentException("No file uploaded");
            }
            
            // Validate file size
            long fileSize = filePart.getSize();
            if (fileSize > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("File size exceeds 1GB limit");
            }
            
            // Get filename
            String filename = getFilename(filePart);
            if (!filename.toLowerCase().endsWith(".pdf")) {
                throw new IllegalArgumentException("File must be a PDF");
            }
            
            // Save uploaded file to temp location
            tempFile = File.createTempFile("preflight-" + UUID.randomUUID(), ".pdf");
            try (InputStream input = filePart.getInputStream();
                 FileOutputStream output = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            
            // Build configuration
            PdfPreflightConfig config = PdfPreflightConfig.builder(tempFile.getAbsolutePath())
                .useCropBox(useCropBox)
                .tolerance(tolerance)
                .build();
            
            // Run preflight validation
            PdfPreflightService service = new PdfPreflightService();
            PdfPreflightResult result = service.execute(config);
            
            // Build response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("result", result);
            responseData.put("fileName", filename);
            responseData.put("fileSize", formatFileSize(fileSize));
            responseData.put("processingTime", System.currentTimeMillis() - startTime);
            
            return objectMapper.writeValueAsString(responseData);
            
        } catch (IllegalArgumentException e) {
            response.status(400);
            System.err.println("Preflight validation error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(false, e.getMessage());
        } catch (Exception e) {
            response.status(500);
            System.err.println("Server error during preflight: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(false, "Server error: " + e.getMessage());
        } finally {
            // Cleanup temp file
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
    
    /**
     * Exports preflight result as JSON report.
     */
    private static String exportJsonReport(Request request, Response response) {
        try {
            String resultJson = request.body();
            response.type("application/json");
            response.header("Content-Disposition", "attachment; filename=preflight-report.json");
            return resultJson;
        } catch (Exception e) {
            response.status(500);
            return createErrorResponse(false, "Export failed: " + e.getMessage());
        }
    }
    
    /**
     * Exports preflight result as text report.
     */
    private static String exportTextReport(Request request, Response response) {
        try {
            String resultJson = request.body();
            Map<String, Object> result = objectMapper.readValue(resultJson, Map.class);
            
            // Generate text report
            StringBuilder textReport = new StringBuilder();
            textReport.append("=== PDF Preflight Report ===\n");
            textReport.append("Generated: ").append(java.time.Instant.now()).append("\n\n");
            
            Map<String, Object> resultMap = (Map<String, Object>) result.get("result");
            if (resultMap != null) {
                textReport.append("Status: ").append(
                    Boolean.TRUE.equals(resultMap.get("passed")) ? "PASS" : "FAIL").append("\n");
                textReport.append("Total Pages: ").append(resultMap.get("totalPages")).append("\n");
                textReport.append("Mismatches: ").append(resultMap.get("mismatchCount")).append("\n\n");
                
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> mismatches = 
                    (java.util.List<Map<String, Object>>) resultMap.get("mismatches");
                
                if (mismatches != null && !mismatches.isEmpty()) {
                    textReport.append("Mismatches Found:\n\n");
                    for (Map<String, Object> mismatch : mismatches) {
                        textReport.append("Page ").append(mismatch.get("pageNumber")).append(":\n");
                        textReport.append("  Reason: ").append(mismatch.get("mismatchReason")).append("\n\n");
                    }
                }
            }
            
            response.type("text/plain");
            response.header("Content-Disposition", "attachment; filename=preflight-report.txt");
            return textReport.toString();
        } catch (Exception e) {
            response.status(500);
            return "Export failed: " + e.getMessage();
        }
    }
    
    /**
     * Handles exceptions and returns error response.
     */
    private static void handleException(Exception e, Request request, Response response) {
        response.status(500);
        response.type("application/json");
        response.body(createErrorResponse(false, "Internal server error: " + e.getMessage()));
    }
    
    /**
     * Handles prepare-for-print request.
     * Fixes issues and creates a print-ready PDF.
     */
    private static String handlePrepareForPrint(Request request, Response response) {
        try {
            // Parse request body
            String requestBody = request.body();
            Map<String, Object> requestData = objectMapper.readValue(requestBody, Map.class);
            
            String inputPdfPath = (String) requestData.get("inputPdfPath");
            if (inputPdfPath == null || inputPdfPath.isEmpty()) {
                throw new IllegalArgumentException("Missing inputPdfPath");
            }
            
            // Load preflight result from request (for reference)
            @SuppressWarnings("unchecked")
            Map<String, Object> preflightResult = (Map<String, Object>) requestData.get("preflightResult");
            
            // For now, we'll reload the PDF and run a quick preflight
            // In production, you'd cache the preflight result
            PdfPreflightConfig config = PdfPreflightConfig.builder(inputPdfPath).build();
            PdfPreflightService service = new PdfPreflightService();
            PdfPreflightResult result = service.execute(config);
            
            // Get custom output filename if provided
            String outputFileName = (String) requestData.get("outputFileName");
            
            // Prepare for print
            com.preflight.service.PdfPrintPreparationService printService = 
                new com.preflight.service.PdfPrintPreparationService();
            com.preflight.service.PdfPrintPreparationService.PrintPreparationResult printResult = 
                printService.prepareForPrint(result, outputFileName);
            
            // Build response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", printResult.isSuccess());
            responseData.put("outputFilePath", printResult.getOutputFilePath());
            responseData.put("fixesApplied", printResult.getFixesApplied());
            responseData.put("warnings", printResult.getWarnings());
            
            // Extract just the filename for display
            if (printResult.getOutputFilePath() != null) {
                File outputFile = new File(printResult.getOutputFilePath());
                responseData.put("outputFileName", outputFile.getName());
            }
            
            return objectMapper.writeValueAsString(responseData);
            
        } catch (IllegalArgumentException e) {
            response.status(400);
            System.err.println("Prepare for print error: " + e.getMessage());
            return createErrorResponse(false, e.getMessage());
        } catch (Exception e) {
            response.status(500);
            System.err.println("Prepare for print server error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(false, "Server error: " + e.getMessage());
        }
    }
    
    /**
     * Creates a JSON error response.
     */
    private static String createErrorResponse(boolean success, String message) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("success", success);
            error.put("message", message);
            return objectMapper.writeValueAsString(error);
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Extracts filename from multipart part.
     */
    private static String getFilename(Part part) {
        String contentDisposition = part.getHeader("Content-Disposition");
        if (contentDisposition != null) {
            for (String element : contentDisposition.split(";")) {
                if (element.trim().startsWith("filename")) {
                    return element.substring(element.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
        }
        return "unknown.pdf";
    }
    
    /**
     * Formats file size in human-readable format.
     */
    private static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Sets the rasterizer instance.
     */
    public static void setRasterizer(PdfRasterizer rasterizer) {
        PreflightApiController.rasterizer = rasterizer;
    }
    
    /**
     * Checks if MuPDF rasterizer is available.
     */
    private static String checkRasterizerStatus(Request request, Response response) {
        Map<String, Object> result = new HashMap<>();
        boolean available = rasterizer != null && rasterizer.isMuPdfAvailable();
        result.put("available", available);
        result.put("message", available ? "MuPDF is available" : "MuPDF not found");
        
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"available\": false, \"message\": \"Error checking status\"}";
        }
    }
    
    /**
     * Handles PDF rasterization request.
     */
    private static String handleRasterize(Request request, Response response) {
        try {
            // Parse request body
            String requestBody = request.body();
            Map<String, Object> requestData = objectMapper.readValue(requestBody, Map.class);
            
            String inputPdfPath = (String) requestData.get("inputPdfPath");
            @SuppressWarnings("unchecked")
            java.util.List<Integer> pageNumbers = (java.util.List<Integer>) requestData.get("pageNumbers");
            Integer dpi = (Integer) requestData.getOrDefault("dpi", 150);
            
            if (inputPdfPath == null || inputPdfPath.isEmpty()) {
                throw new IllegalArgumentException("Missing inputPdfPath");
            }
            
            if (pageNumbers == null || pageNumbers.isEmpty()) {
                throw new IllegalArgumentException("Missing pageNumbers");
            }
            
            // Check if rasterizer is available
            if (rasterizer == null || !rasterizer.isMuPdfAvailable()) {
                response.status(503);
                return createErrorResponse(false, "MuPDF is not installed or not available");
            }
            
            // Create temp directory for output
            String outputDir = System.getProperty("java.io.tmpdir") + "/preflight-rasterized-" + UUID.randomUUID();
            
            // Convert List<Integer> to int[]
            int[] pages = pageNumbers.stream().mapToInt(i -> i).toArray();
            
            // Rasterize pages
            boolean success = rasterizer.rasterizePages(inputPdfPath, pages, outputDir, dpi);
            
            if (!success) {
                response.status(500);
                return createErrorResponse(false, "Rasterization failed");
            }
            
            // List generated images
            File outputDirFile = new File(outputDir);
            File[] imageFiles = outputDirFile.listFiles((dir, name) -> name.endsWith(".png"));
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("outputDir", outputDir);
            responseData.put("imageCount", imageFiles != null ? imageFiles.length : 0);
            
            if (imageFiles != null) {
                java.util.List<Map<String, String>> images = new java.util.ArrayList<>();
                for (File img : imageFiles) {
                    Map<String, String> imgInfo = new HashMap<>();
                    imgInfo.put("filename", img.getName());
                    imgInfo.put("path", img.getAbsolutePath());
                    imgInfo.put("size", String.valueOf(img.length()));
                    images.add(imgInfo);
                }
                responseData.put("images", images);
            }
            
            return objectMapper.writeValueAsString(responseData);
            
        } catch (IllegalArgumentException e) {
            response.status(400);
            return createErrorResponse(false, e.getMessage());
        } catch (Exception e) {
            response.status(500);
            System.err.println("Rasterization error: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(false, "Server error: " + e.getMessage());
        }
    }
    
    /**
     * Handles file download requests.
     */
    private static Object handleDownload(Request request, Response response) {
        try {
            String filename = request.params("filename");
            if (filename == null || filename.isEmpty()) {
                response.status(400);
                return "Missing filename";
            }
            
            // Check if full path is provided as query parameter (for rasterized images)
            String fullPath = request.queryParams("path");
            File file;
            
            if (fullPath != null && !fullPath.isEmpty()) {
                // Use the full path directly
                file = new File(fullPath);
            } else {
                // Determine file path (look in current directory and temp directories)
                file = new File(filename);
                if (!file.exists()) {
                    // Try in system temp directory
                    String tempDir = System.getProperty("java.io.tmpdir");
                    file = new File(tempDir, filename);
                }
            }
            
            if (!file.exists()) {
                response.status(404);
                return "File not found: " + filename;
            }
            
            // Determine content type based on file extension
            String contentType = "application/octet-stream";
            if (filename.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }
            
            // Set response headers for file download
            response.type(contentType);
            response.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.header("Content-Length", String.valueOf(file.length()));
            
            // Stream file to response
            java.nio.file.Files.copy(file.toPath(), response.raw().getOutputStream());
            
            return "";
            
        } catch (Exception e) {
            response.status(500);
            System.err.println("Download error: " + e.getMessage());
            e.printStackTrace();
            return "Download failed: " + e.getMessage();
        }
    }
    
    /**
     * Handles file download from full path.
     */
    private static Object handleDownloadFile(Request request, Response response) {
        try {
            // Parse request body
            String requestBody = request.body();
            Map<String, Object> requestData = objectMapper.readValue(requestBody, Map.class);
            
            String filePath = (String) requestData.get("filePath");
            String fileName = (String) requestData.get("fileName");
            
            if (filePath == null || filePath.isEmpty()) {
                response.status(400);
                return "Missing filePath";
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                response.status(404);
                return "File not found: " + filePath;
            }
            
            // Set response headers for file download
            response.type("application/pdf");
            response.header("Content-Disposition", "attachment; filename=\"" + 
                (fileName != null ? fileName : file.getName()) + "\"");
            response.header("Content-Length", String.valueOf(file.length()));
            
            // Stream file to response
            java.nio.file.Files.copy(file.toPath(), response.raw().getOutputStream());
            
            return "";
            
        } catch (Exception e) {
            response.status(500);
            System.err.println("Download file error: " + e.getMessage());
            e.printStackTrace();
            return "Download failed: " + e.getMessage();
        }
    }
}
