package com.preflight.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive analysis of a single PDF page including dimensions, colorspaces,
 * font issues, and image resolution information.
 */
public class PdfPageAnalysis {
    
    public enum PageStatus { PASS, WARNING, FAIL }
    
    /**
     * Represents a font issue found on a page.
     */
    public static class FontIssue {
        public enum Severity { WARNING, CRITICAL }
        public enum IssueType { UNEMBEDDED, SUBSTITUTION_ERROR, MISSING_GLYPHS, CID_FONT_ISSUE }
        
        private final IssueType type;
        private final String fontName;
        private final Severity severity;
        private final String description;
        
        public FontIssue(IssueType type, String fontName, Severity severity, String description) {
            this.type = type;
            this.fontName = fontName;
            this.severity = severity;
            this.description = description;
        }
        
        public IssueType getType() { return type; }
        public String getFontName() { return fontName; }
        public Severity getSeverity() { return severity; }
        public String getDescription() { return description; }
    }
    
    /**
     * Represents image information extracted from a page.
     */
    public static class ImageInfo {
        private final String name;
        private final float x, y, width, height; // Display size in points
        private final int pixelWidth, pixelHeight; // Actual pixel dimensions
        private final float dpi;
        private final String colorspace;
        private final boolean isLowRes;
        
        public ImageInfo(String name, float x, float y, float width, float height,
                        int pixelWidth, int pixelHeight, String colorspace) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.pixelWidth = pixelWidth;
            this.pixelHeight = pixelHeight;
            this.colorspace = colorspace;
            this.dpi = calculateDPI(pixelWidth, width);
            this.isLowRes = this.dpi < 150.0f;
        }
        
        private float calculateDPI(int pixels, float points) {
            if (points <= 0) return 0;
            return (pixels / points) * 72.0f; // 72 points = 1 inch
        }
        
        public String getName() { return name; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }
        public int getPixelWidth() { return pixelWidth; }
        public int getPixelHeight() { return pixelHeight; }
        public float getDPI() { return dpi; }
        public String getColorspace() { return colorspace; }
        public boolean isLowRes() { return isLowRes; }
    }
    
    // Page identification and dimensions
    private final int pageNumber;
    private final float widthPts, heightPts;
    private final float widthInches, heightInches;
    private final String orientation;
    
    // Analysis results
    private final List<String> colorspaces;
    private final List<FontIssue> fontIssues;
    private final List<ImageInfo> images;
    private final PageStatus status;
    private final List<String> warnings;
    
    private PdfPageAnalysis(int pageNumber, float widthPts, float heightPts, 
                           String orientation, List<String> colorspaces,
                           List<FontIssue> fontIssues, List<ImageInfo> images,
                           PageStatus status, List<String> warnings) {
        this.pageNumber = pageNumber;
        this.widthPts = widthPts;
        this.heightPts = heightPts;
        this.widthInches = widthPts / 72.0f;
        this.heightInches = heightPts / 72.0f;
        this.orientation = orientation;
        this.colorspaces = Collections.unmodifiableList(new ArrayList<>(colorspaces));
        this.fontIssues = Collections.unmodifiableList(new ArrayList<>(fontIssues));
        this.images = Collections.unmodifiableList(new ArrayList<>(images));
        this.status = status;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }
    
    /**
     * Builder for constructing PdfPageAnalysis instances.
     */
    public static class Builder {
        private final int pageNumber;
        private final float widthPts, heightPts;
        private final String orientation;
        private List<String> colorspaces = new ArrayList<>();
        private List<FontIssue> fontIssues = new ArrayList<>();
        private List<ImageInfo> images = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public Builder(int pageNumber, float widthPts, float heightPts, String orientation) {
            this.pageNumber = pageNumber;
            this.widthPts = widthPts;
            this.heightPts = heightPts;
            this.orientation = orientation;
        }
        
        public Builder colorspaces(List<String> colorspaces) {
            this.colorspaces = colorspaces;
            return this;
        }
        
        public Builder fontIssues(List<FontIssue> fontIssues) {
            this.fontIssues = fontIssues;
            return this;
        }
        
        public Builder images(List<ImageInfo> images) {
            this.images = images;
            return this;
        }
        
        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }
        
        public PdfPageAnalysis build() {
            PageStatus status = determineStatus();
            return new PdfPageAnalysis(pageNumber, widthPts, heightPts, orientation,
                                     colorspaces, fontIssues, images, status, warnings);
        }
        
        private PageStatus determineStatus() {
            boolean hasCriticalFontIssues = fontIssues.stream()
                .anyMatch(issue -> issue.getSeverity() == FontIssue.Severity.CRITICAL);
            
            if (hasCriticalFontIssues) {
                return PageStatus.FAIL;
            }
            
            boolean hasWarnings = !fontIssues.isEmpty() || 
                                images.stream().anyMatch(ImageInfo::isLowRes) ||
                                colorspaces.contains("RGB") ||
                                !warnings.isEmpty();
            
            return hasWarnings ? PageStatus.WARNING : PageStatus.PASS;
        }
    }
    
    // Getters
    public int getPageNumber() { return pageNumber; }
    public float getWidthPts() { return widthPts; }
    public float getHeightPts() { return heightPts; }
    public float getWidthInches() { return widthInches; }
    public float getHeightInches() { return heightInches; }
    public String getOrientation() { return orientation; }
    public List<String> getColorspaces() { return colorspaces; }
    public List<FontIssue> getFontIssues() { return fontIssues; }
    public List<ImageInfo> getImages() { return images; }
    public PageStatus getStatus() { return status; }
    public List<String> getWarnings() { return warnings; }
    
    public boolean hasCriticalFontIssues() {
        return fontIssues.stream()
            .anyMatch(issue -> issue.getSeverity() == FontIssue.Severity.CRITICAL);
    }
    
    public int getLowResImageCount() {
        return (int) images.stream().filter(ImageInfo::isLowRes).count();
    }
    
    @Override
    public String toString() {
        return String.format("Page %d: %.2f\" x %.2f\" (%s) - %s [%d fonts, %d images, %d warnings]",
            pageNumber, widthInches, heightInches, orientation, status,
            fontIssues.size(), images.size(), warnings.size());
    }
}
