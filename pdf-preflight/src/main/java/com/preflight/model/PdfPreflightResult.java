package com.preflight.model;

import java.util.Collections;
import java.util.List;

/**
 * Result container for PDF preflight operations.
 */
public class PdfPreflightResult {
    
    private final boolean passed;
    private final int totalPages;
    private final PdfPageInfo referencePage;
    private final List<PageMismatch> mismatches;
    private final List<PdfPageAnalysis> pageAnalyses;
    private final int warningCount;
    private final int criticalFontIssues;
    private final String errorMessage;
    private final long processingTimeMs;
    private final int exitCode;
    private final String inputPdfPath;
    
    public PdfPreflightResult(boolean passed, int totalPages, PdfPageInfo referencePage,
                             List<PageMismatch> mismatches, long processingTimeMs,
                             String inputPdfPath) {
        this(passed, totalPages, referencePage, mismatches, Collections.emptyList(), 
             processingTimeMs, inputPdfPath);
    }
    
    public PdfPreflightResult(boolean passed, int totalPages, PdfPageInfo referencePage,
                             List<PageMismatch> mismatches, List<PdfPageAnalysis> pageAnalyses,
                             long processingTimeMs, String inputPdfPath) {
        this.passed = passed;
        this.totalPages = totalPages;
        this.referencePage = referencePage;
        this.mismatches = Collections.unmodifiableList(mismatches);
        this.pageAnalyses = pageAnalyses != null ? 
            Collections.unmodifiableList(pageAnalyses) : Collections.emptyList();
        this.errorMessage = null;
        this.processingTimeMs = processingTimeMs;
        this.inputPdfPath = inputPdfPath;
        this.exitCode = passed ? 0 : 1;
        
        // Calculate warning and critical issue counts
        this.warningCount = (int) this.pageAnalyses.stream()
            .filter(a -> a.getStatus() == PdfPageAnalysis.PageStatus.WARNING)
            .count();
        this.criticalFontIssues = (int) this.pageAnalyses.stream()
            .filter(PdfPageAnalysis::hasCriticalFontIssues)
            .count();
    }
    
    public PdfPreflightResult(String errorMessage, long processingTimeMs, String inputPdfPath) {
        this.passed = false;
        this.totalPages = 0;
        this.referencePage = null;
        this.mismatches = Collections.emptyList();
        this.pageAnalyses = Collections.emptyList();
        this.warningCount = 0;
        this.criticalFontIssues = 0;
        this.errorMessage = errorMessage;
        this.processingTimeMs = processingTimeMs;
        this.inputPdfPath = inputPdfPath;
        this.exitCode = 2;
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public PdfPageInfo getReferencePage() {
        return referencePage;
    }
    
    public List<PageMismatch> getMismatches() {
        return mismatches;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public int getExitCode() {
        return exitCode;
    }
    
    public String getInputPdfPath() {
        return inputPdfPath;
    }
    
    public int getMismatchCount() {
        return mismatches.size();
    }
    
    public List<PdfPageAnalysis> getPageAnalyses() {
        return pageAnalyses;
    }
    
    public int getWarningCount() {
        return warningCount;
    }
    
    public int getCriticalFontIssues() {
        return criticalFontIssues;
    }
    
    @Override
    public String toString() {
        if (errorMessage != null) {
            return String.format("ERROR: %s", errorMessage);
        }
        return String.format("Preflight %s - %d pages checked, %d mismatches found",
            passed ? "PASSED" : "FAILED", totalPages, mismatches.size());
    }
}
