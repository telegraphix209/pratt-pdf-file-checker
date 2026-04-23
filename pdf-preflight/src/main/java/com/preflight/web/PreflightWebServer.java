package com.preflight.web;

import com.preflight.rasterizer.PdfRasterizer;
import static spark.Spark.*;
import javax.servlet.MultipartConfigElement;

/**
 * Main web server for PDF Preflight Web UI.
 * Starts an HTTP server on port 8080 serving the web interface and REST API.
 */
public class PreflightWebServer {
    
    public static void main(String[] args) {
        // Configure server
        port(8080);
        
        // Initialize MuPDF rasterizer if available
        initializeRasterizer();
        
        // Serve static files from /public directory
        staticFiles.location("/public");
        
        // Setup API routes
        PreflightApiController.setupRoutes();
        
        // Server startup message
        System.out.println("=================================================");
        System.out.println("  PDF Preflight Web UI");
        System.out.println("=================================================");
        System.out.println();
        System.out.println("  Server running at: http://localhost:8080");
        System.out.println();
        System.out.println("  Open your browser and navigate to the URL above");
        System.out.println("  to start validating PDF files.");
        System.out.println();
        System.out.println("=================================================");
    }
    
    /**
     * Initializes the MuPDF rasterizer if mutool is available.
     */
    private static void initializeRasterizer() {
        // Try common MuPDF paths
        String[] mutoolPaths = {
            "mutool",  // In PATH
            "/usr/local/bin/mutool",  // macOS Homebrew
            "/opt/homebrew/bin/mutool",  // macOS Apple Silicon
            "/usr/bin/mutool"  // Linux
        };
        
        PdfRasterizer rasterizer = null;
        String foundPath = null;
        
        for (String path : mutoolPaths) {
            PdfRasterizer testRasterizer = new PdfRasterizer(path);
            if (testRasterizer.isMuPdfAvailable()) {
                rasterizer = testRasterizer;
                foundPath = path;
                break;
            }
        }
        
        if (rasterizer != null) {
            PreflightApiController.setRasterizer(rasterizer);
            System.out.println("  ✓ MuPDF rasterizer enabled: " + foundPath);
        } else {
            System.out.println("  ✗ MuPDF not found (rasterization disabled)");
            System.out.println("    Install with: brew install mupdf-tools (macOS)");
            System.out.println("    Or: sudo apt-get install mupdf-tools (Linux)");
        }
        System.out.println();
    }
}
