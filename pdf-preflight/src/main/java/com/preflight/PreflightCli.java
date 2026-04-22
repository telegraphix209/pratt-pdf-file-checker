package com.preflight;

import com.preflight.config.PdfPreflightConfig;
import com.preflight.model.PdfPreflightResult;
import com.preflight.service.PdfPreflightService;

/**
 * Command-line entry point for PDF Preflight validation.
 * 
 * Usage:
 * java -jar pdf-preflight.jar --input <path> [options]
 * 
 * Exit codes:
 * 0 - Pass (all pages match)
 * 1 - Fail (mismatches found)
 * 2 - Error (invalid PDF, file not found, runtime exception)
 */
public class PreflightCli {
    
    public static void main(String[] args) {
        try {
            // Parse command-line arguments
            CommandLineArgs cliArgs = parseArgs(args);
            
            if (cliArgs == null || cliArgs.inputPath == null) {
                printUsage();
                System.exit(2);
            }
            
            // Build configuration
            PdfPreflightConfig.Builder configBuilder = PdfPreflightConfig.builder(cliArgs.inputPath)
                .outputJsonPath(cliArgs.outputJsonPath)
                .outputTextPath(cliArgs.outputTextPath)
                .useCropBox(cliArgs.useCropBox)
                .tolerance(cliArgs.tolerance)
                .enableRasterization(cliArgs.enableRasterization)
                .muPdfToolPath(cliArgs.muPdfToolPath)
                .rasterDpi(cliArgs.rasterDpi)
                .rasterOutputDir(cliArgs.rasterOutputDir);
            
            if (cliArgs.pagesToRasterize != null) {
                configBuilder.pagesToRasterize(cliArgs.pagesToRasterize);
            }
            
            PdfPreflightConfig config = configBuilder.build();
            
            // Execute preflight
            PdfPreflightService service = new PdfPreflightService();
            PdfPreflightResult result = service.execute(config);
            
            // Print summary to stdout
            printSummary(result);
            
            // Exit with appropriate code
            System.exit(result.getExitCode());
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
    
    /**
     * Parses command-line arguments.
     */
    private static CommandLineArgs parseArgs(String[] args) {
        CommandLineArgs cliArgs = new CommandLineArgs();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            switch (arg) {
                case "--input":
                    cliArgs.inputPath = getRequiredValue(args, i, "--input");
                    if (cliArgs.inputPath == null) return null;
                    i++;
                    break;
                    
                case "--output-json":
                    cliArgs.outputJsonPath = getRequiredValue(args, i, "--output-json");
                    if (cliArgs.outputJsonPath == null) return null;
                    i++;
                    break;
                    
                case "--output-text":
                    cliArgs.outputTextPath = getRequiredValue(args, i, "--output-text");
                    if (cliArgs.outputTextPath == null) return null;
                    i++;
                    break;
                    
                case "--use-mediabox":
                    cliArgs.useCropBox = false;
                    break;
                    
                case "--tolerance":
                    String tolValue = getRequiredValue(args, i, "--tolerance");
                    if (tolValue == null) return null;
                    try {
                        cliArgs.tolerance = Float.parseFloat(tolValue);
                    } catch (NumberFormatException e) {
                        System.err.println("ERROR: Invalid tolerance value: " + tolValue);
                        return null;
                    }
                    i++;
                    break;
                    
                case "--rasterize":
                    cliArgs.enableRasterization = true;
                    break;
                    
                case "--raster-dpi":
                    String dpiValue = getRequiredValue(args, i, "--raster-dpi");
                    if (dpiValue == null) return null;
                    try {
                        cliArgs.rasterDpi = Integer.parseInt(dpiValue);
                    } catch (NumberFormatException e) {
                        System.err.println("ERROR: Invalid DPI value: " + dpiValue);
                        return null;
                    }
                    i++;
                    break;
                    
                case "--mutool-path":
                    cliArgs.muPdfToolPath = getRequiredValue(args, i, "--mutool-path");
                    if (cliArgs.muPdfToolPath == null) return null;
                    i++;
                    break;
                    
                case "--raster-pages":
                    String pagesValue = getRequiredValue(args, i, "--raster-pages");
                    if (pagesValue == null) return null;
                    try {
                        cliArgs.pagesToRasterize = parsePageList(pagesValue);
                    } catch (NumberFormatException e) {
                        System.err.println("ERROR: Invalid page list: " + pagesValue);
                        return null;
                    }
                    i++;
                    break;
                    
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    return null;
                    
                default:
                    System.err.println("ERROR: Unknown argument: " + arg);
                    printUsage();
                    return null;
            }
        }
        
        return cliArgs;
    }
    
    /**
     * Gets the next argument value for a parameter.
     */
    private static String getRequiredValue(String[] args, int index, String paramName) {
        if (index + 1 >= args.length) {
            System.err.println("ERROR: " + paramName + " requires a value");
            return null;
        }
        return args[index + 1];
    }
    
    /**
     * Parses a comma-separated list of page numbers.
     */
    private static int[] parsePageList(String pagesValue) {
        String[] parts = pagesValue.split(",");
        int[] pages = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            pages[i] = Integer.parseInt(parts[i].trim());
        }
        return pages;
    }
    
    /**
     * Prints usage information.
     */
    private static void printUsage() {
        System.out.println("PDF Preflight Module v1.0.0");
        System.out.println();
        System.out.println("Usage: java -jar pdf-preflight.jar --input <path> [options]");
        System.out.println();
        System.out.println("Required:");
        System.out.println("  --input <path>              Path to the PDF file to validate");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --output-json <path>        JSON report output path (default: preflight-report.json)");
        System.out.println("  --output-text <path>        Text report output path (default: preflight-report.txt)");
        System.out.println("  --use-mediabox              Use MediaBox instead of CropBox for measurements");
        System.out.println("  --tolerance <float>         Tolerance for dimension comparison in points (default: 0.01)");
        System.out.println("  --rasterize                 Enable rasterization of failed pages (requires MuPDF)");
        System.out.println("  --raster-dpi <int>          DPI for rasterization (default: 150)");
        System.out.println("  --mutool-path <path>        Path to mutool executable (default: mutool)");
        System.out.println("  --raster-pages <list>       Comma-separated page numbers to rasterize (default: all mismatched)");
        System.out.println("  --help, -h                  Show this help message");
        System.out.println();
        System.out.println("Exit Codes:");
        System.out.println("  0  Pass - all pages have matching dimensions and orientation");
        System.out.println("  1  Fail - mismatches found");
        System.out.println("  2  Error - invalid PDF, file not found, or runtime error");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar pdf-preflight.jar --input document.pdf");
        System.out.println("  java -jar pdf-preflight.jar --input document.pdf --output-json report.json --output-text report.txt");
        System.out.println("  java -jar pdf-preflight.jar --input document.pdf --use-mediabox --tolerance 0.1");
        System.out.println("  java -jar pdf-preflight.jar --input document.pdf --rasterize --raster-dpi 300");
    }
    
    /**
     * Prints a summary of the preflight result.
     */
    private static void printSummary(PdfPreflightResult result) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  PDF Preflight Result");
        System.out.println("========================================");
        System.out.println();
        
        if (result.getErrorMessage() != null) {
            System.out.println("Status: ERROR");
            System.out.println("Error: " + result.getErrorMessage());
        } else {
            System.out.println("Status: " + (result.isPassed() ? "PASS ✓" : "FAIL ✗"));
            System.out.println("Total Pages: " + result.getTotalPages());
            System.out.println("Mismatches: " + result.getMismatchCount());
            
            if (!result.isPassed() && result.getReferencePage() != null) {
                System.out.println();
                System.out.println("Reference (Page 1):");
                System.out.println(String.format("  %.2f x %.2f pts (%s)",
                    result.getReferencePage().getWidth(),
                    result.getReferencePage().getHeight(),
                    result.getReferencePage().getOrientation()));
            }
        }
        
        System.out.println();
        System.out.println("Processing Time: " + result.getProcessingTimeMs() + " ms");
        System.out.println("JSON Report: " + "preflight-report.json");
        System.out.println("Text Report: " + "preflight-report.txt");
        System.out.println();
    }
    
    /**
     * Container for parsed command-line arguments.
     */
    private static class CommandLineArgs {
        String inputPath;
        String outputJsonPath = "preflight-report.json";
        String outputTextPath = "preflight-report.txt";
        boolean useCropBox = true;
        float tolerance = 0.01f;
        boolean enableRasterization = false;
        String muPdfToolPath = "mutool";
        int rasterDpi = 150;
        int[] pagesToRasterize;
        String rasterOutputDir = "rasterized-pages";
    }
}
