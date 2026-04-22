package com.preflight.web;

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
}
