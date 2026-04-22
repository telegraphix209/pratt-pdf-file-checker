package com.preflight.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.preflight.model.PageMismatch;
import com.preflight.model.PdfPageAnalysis;
import com.preflight.model.PdfPageInfo;
import com.preflight.model.PdfPreflightResult;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Writes preflight results as structured JSON.
 */
public class JsonReportWriter implements PdfReportWriter {
    
    private final ObjectMapper objectMapper;
    
    public JsonReportWriter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    @Override
    public void write(PdfPreflightResult result, OutputStream out) throws IOException {
        Map<String, Object> report = new HashMap<>();
        
        report.put("passed", result.isPassed());
        report.put("totalPages", result.getTotalPages());
        report.put("processingTimeMs", result.getProcessingTimeMs());
        report.put("exitCode", result.getExitCode());
        report.put("inputPdf", result.getInputPdfPath());
        
        if (result.getErrorMessage() != null) {
            report.put("errorMessage", result.getErrorMessage());
        } else {
            // Add reference page info
            if (result.getReferencePage() != null) {
                report.put("referencePage", buildPageInfoMap(result.getReferencePage()));
            }
            
            // Add mismatches
            List<Map<String, Object>> mismatches = new ArrayList<>();
            for (PageMismatch mismatch : result.getMismatches()) {
                mismatches.add(buildMismatchMap(mismatch));
            }
            report.put("mismatches", mismatches);
            report.put("mismatchCount", mismatches.size());
            
            // Add page analyses
            List<Map<String, Object>> pageAnalyses = new ArrayList<>();
            for (PdfPageAnalysis analysis : result.getPageAnalyses()) {
                pageAnalyses.add(buildPageAnalysisMap(analysis));
            }
            report.put("pageAnalyses", pageAnalyses);
            report.put("warningCount", result.getWarningCount());
            report.put("criticalFontIssues", result.getCriticalFontIssues());
        }
        
        objectMapper.writeValue(out, report);
    }
    
    private Map<String, Object> buildPageInfoMap(PdfPageInfo pageInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("pageNumber", pageInfo.getPageNumber());
        map.put("width", roundTo2Decimals(pageInfo.getWidth()));
        map.put("height", roundTo2Decimals(pageInfo.getHeight()));
        map.put("orientation", pageInfo.getOrientation());
        map.put("boxUsed", pageInfo.getBoxUsed());
        return map;
    }
    
    private Map<String, Object> buildMismatchMap(PageMismatch mismatch) {
        Map<String, Object> map = new HashMap<>();
        map.put("pageNumber", mismatch.getPageNumber());
        map.put("actualWidth", roundTo2Decimals(mismatch.getActualWidth()));
        map.put("actualHeight", roundTo2Decimals(mismatch.getActualHeight()));
        map.put("actualOrientation", mismatch.getActualOrientation());
        map.put("expectedWidth", roundTo2Decimals(mismatch.getExpectedWidth()));
        map.put("expectedHeight", roundTo2Decimals(mismatch.getExpectedHeight()));
        map.put("expectedOrientation", mismatch.getExpectedOrientation());
        map.put("mismatchReason", mismatch.getMismatchReason());
        return map;
    }
    
    private double roundTo2Decimals(float value) {
        return Math.round(value * 100.0) / 100.0;
    }
    
    private Map<String, Object> buildPageAnalysisMap(PdfPageAnalysis analysis) {
        Map<String, Object> map = new HashMap<>();
        map.put("pageNumber", analysis.getPageNumber());
        map.put("widthPts", roundTo2Decimals(analysis.getWidthPts()));
        map.put("heightPts", roundTo2Decimals(analysis.getHeightPts()));
        map.put("widthInches", roundTo2Decimals(analysis.getWidthInches()));
        map.put("heightInches", roundTo2Decimals(analysis.getHeightInches()));
        map.put("orientation", analysis.getOrientation());
        map.put("colorspaces", analysis.getColorspaces());
        map.put("status", analysis.getStatus().toString());
        map.put("warnings", analysis.getWarnings());
        
        // Font issues
        List<Map<String, Object>> fontIssues = new ArrayList<>();
        for (PdfPageAnalysis.FontIssue issue : analysis.getFontIssues()) {
            Map<String, Object> issueMap = new HashMap<>();
            issueMap.put("type", issue.getType().toString());
            issueMap.put("fontName", issue.getFontName());
            issueMap.put("severity", issue.getSeverity().toString());
            issueMap.put("description", issue.getDescription());
            fontIssues.add(issueMap);
        }
        map.put("fontIssues", fontIssues);
        
        // Images
        List<Map<String, Object>> images = new ArrayList<>();
        for (PdfPageAnalysis.ImageInfo image : analysis.getImages()) {
            Map<String, Object> imageMap = new HashMap<>();
            imageMap.put("name", image.getName());
            imageMap.put("pixelWidth", image.getPixelWidth());
            imageMap.put("pixelHeight", image.getPixelHeight());
            imageMap.put("dpi", roundTo2Decimals(image.getDPI()));
            imageMap.put("colorspace", image.getColorspace());
            imageMap.put("isLowRes", image.isLowRes());
            images.add(imageMap);
        }
        map.put("images", images);
        
        return map;
    }
}
