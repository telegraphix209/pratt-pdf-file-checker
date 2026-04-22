package com.preflight.model;

/**
 * Immutable representation of a PDF page's dimensional information.
 */
public class PdfPageInfo {
    
    private final int pageNumber;
    private final float width;
    private final float height;
    private final String orientation;
    private final String boxUsed;
    private final boolean isValid;
    
    public PdfPageInfo(int pageNumber, float width, float height, String boxUsed) {
        this.pageNumber = pageNumber;
        this.width = width;
        this.height = height;
        this.orientation = calculateOrientation(width, height);
        this.boxUsed = boxUsed;
        this.isValid = true;
    }
    
    public PdfPageInfo(int pageNumber, boolean isValid) {
        this.pageNumber = pageNumber;
        this.width = 0.0f;
        this.height = 0.0f;
        this.orientation = "unknown";
        this.boxUsed = "none";
        this.isValid = isValid;
    }
    
    private static String calculateOrientation(float width, float height) {
        return width >= height ? "landscape" : "portrait";
    }
    
    public int getPageNumber() {
        return pageNumber;
    }
    
    public float getWidth() {
        return width;
    }
    
    public float getHeight() {
        return height;
    }
    
    public String getOrientation() {
        return orientation;
    }
    
    public String getBoxUsed() {
        return boxUsed;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    @Override
    public String toString() {
        return String.format("Page %d: %.2f x %.2f pts (%s) [%s]", 
            pageNumber, width, height, orientation, boxUsed);
    }
}
