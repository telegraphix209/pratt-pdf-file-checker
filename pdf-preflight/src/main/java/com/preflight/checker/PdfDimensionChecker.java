package com.preflight.checker;

import com.preflight.config.PdfPreflightConfig;
import com.preflight.model.PageMismatch;
import com.preflight.model.PdfPageInfo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks all pages in a PDF for dimension and orientation consistency.
 * Uses a single-pass approach for efficiency with large documents.
 */
public class PdfDimensionChecker {
    
    /**
     * Validates all pages have the same dimensions and orientation as the first page.
     * 
     * @param document The PDF document to check
     * @param config Configuration settings
     * @return List of mismatches found (empty if all pages match)
     */
    public List<PageMismatch> checkDimensions(PDDocument document, PdfPreflightConfig config) {
        List<PageMismatch> mismatches = new ArrayList<>();
        
        int pageCount = document.getNumberOfPages();
        if (pageCount <= 1) {
            return mismatches;
        }
        
        // Get reference page (first page)
        PDPage referencePage = document.getPage(0);
        PDRectangle referenceBox = getPageBox(referencePage, config.isUseCropBox());
        PdfPageInfo referenceInfo = new PdfPageInfo(1, 
            referenceBox.getWidth(), 
            referenceBox.getHeight(),
            getBoxName(referencePage, config.isUseCropBox()));
        
        float tolerance = config.getTolerance();
        int pageNumber = 1;
        
        // Stream through all pages
        for (PDPage page : document.getPages()) {
            // Skip first page (it's the reference)
            if (pageNumber == 1) {
                pageNumber++;
                continue;
            }
            
            PDRectangle pageBox = getPageBox(page, config.isUseCropBox());
            String boxName = getBoxName(page, config.isUseCropBox());
            
            float actualWidth = pageBox.getWidth();
            float actualHeight = pageBox.getHeight();
            String actualOrientation = calculateOrientation(actualWidth, actualHeight);
            
            List<String> mismatchReasons = new ArrayList<>();
            
            // Check width
            if (Math.abs(actualWidth - referenceInfo.getWidth()) > tolerance) {
                mismatchReasons.add(String.format("Width mismatch: %.2f != %.2f",
                    actualWidth, referenceInfo.getWidth()));
            }
            
            // Check height
            if (Math.abs(actualHeight - referenceInfo.getHeight()) > tolerance) {
                mismatchReasons.add(String.format("Height mismatch: %.2f != %.2f",
                    actualHeight, referenceInfo.getHeight()));
            }
            
            // Check orientation
            if (!actualOrientation.equals(referenceInfo.getOrientation())) {
                mismatchReasons.add(String.format("Orientation mismatch: %s != %s",
                    actualOrientation, referenceInfo.getOrientation()));
            }
            
            // Record mismatch if any differences found
            if (!mismatchReasons.isEmpty()) {
                String reason = String.join(", ", mismatchReasons);
                mismatches.add(new PageMismatch(
                    pageNumber,
                    actualWidth,
                    actualHeight,
                    actualOrientation,
                    referenceInfo.getWidth(),
                    referenceInfo.getHeight(),
                    referenceInfo.getOrientation(),
                    reason
                ));
            }
            
            pageNumber++;
        }
        
        return mismatches;
    }
    
    /**
     * Gets the appropriate page box (CropBox or MediaBox) based on configuration.
     * Falls back to MediaBox if CropBox is unavailable or invalid.
     */
    private PDRectangle getPageBox(PDPage page, boolean preferCropBox) {
        if (preferCropBox) {
            PDRectangle cropBox = page.getCropBox();
            // Validate CropBox is available and has valid dimensions
            if (cropBox != null && cropBox.getWidth() > 0 && cropBox.getHeight() > 0) {
                return cropBox;
            }
        }
        // Fall back to MediaBox
        return page.getMediaBox();
    }
    
    /**
     * Returns the name of the box used for measurement.
     */
    private String getBoxName(PDPage page, boolean preferCropBox) {
        if (preferCropBox) {
            PDRectangle cropBox = page.getCropBox();
            if (cropBox != null && cropBox.getWidth() > 0 && cropBox.getHeight() > 0) {
                return "CropBox";
            }
        }
        return "MediaBox";
    }
    
    /**
     * Calculates orientation based on width and height.
     * Landscape: width >= height
     * Portrait: height > width
     */
    private String calculateOrientation(float width, float height) {
        return width >= height ? "landscape" : "portrait";
    }
}
