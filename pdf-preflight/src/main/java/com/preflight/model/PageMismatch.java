package com.preflight.model;

/**
 * Represents a mismatch between a page and the reference page.
 */
public class PageMismatch {
    
    private final int pageNumber;
    private final float actualWidth;
    private final float actualHeight;
    private final String actualOrientation;
    private final float expectedWidth;
    private final float expectedHeight;
    private final String expectedOrientation;
    private final String mismatchReason;
    
    public PageMismatch(int pageNumber, float actualWidth, float actualHeight,
                       String actualOrientation, float expectedWidth, 
                       float expectedHeight, String expectedOrientation,
                       String mismatchReason) {
        this.pageNumber = pageNumber;
        this.actualWidth = actualWidth;
        this.actualHeight = actualHeight;
        this.actualOrientation = actualOrientation;
        this.expectedWidth = expectedWidth;
        this.expectedHeight = expectedHeight;
        this.expectedOrientation = expectedOrientation;
        this.mismatchReason = mismatchReason;
    }
    
    public int getPageNumber() {
        return pageNumber;
    }
    
    public float getActualWidth() {
        return actualWidth;
    }
    
    public float getActualHeight() {
        return actualHeight;
    }
    
    public String getActualOrientation() {
        return actualOrientation;
    }
    
    public float getExpectedWidth() {
        return expectedWidth;
    }
    
    public float getExpectedHeight() {
        return expectedHeight;
    }
    
    public String getExpectedOrientation() {
        return expectedOrientation;
    }
    
    public String getMismatchReason() {
        return mismatchReason;
    }
    
    @Override
    public String toString() {
        return String.format("Page %d mismatch: %s", pageNumber, mismatchReason);
    }
}
