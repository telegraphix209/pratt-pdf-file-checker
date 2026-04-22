package com.preflight.checker;

import com.preflight.model.PdfPageAnalysis;
import com.preflight.model.PdfPageAnalysis.FontIssue;
import com.preflight.model.PdfPageAnalysis.ImageInfo;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Analyzes individual PDF pages for comprehensive preflight information including
 * dimensions, colorspaces, font issues, and image resolution.
 */
public class PdfPageAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfPageAnalyzer.class);
    private static final float LOW_RES_DPI_THRESHOLD = 150.0f;
    
    /**
     * Performs comprehensive analysis of a PDF page.
     * 
     * @param page The PDF page to analyze
     * @param pageNum Page number (1-based)
     * @return Detailed page analysis result
     */
    public PdfPageAnalysis analyzePage(PDPage page, int pageNum) {
        try {
            PDRectangle mediaBox = page.getMediaBox();
            float width = mediaBox.getWidth();
            float height = mediaBox.getHeight();
            String orientation = width >= height ? "landscape" : "portrait";
            
            // Extract page resources
            PDResources resources = page.getResources();
            if (resources == null) {
                resources = new PDResources();
            }
            
            // Analyze components
            List<String> colorspaces = extractColorspaces(resources);
            List<FontIssue> fontIssues = analyzeFonts(resources);
            List<ImageInfo> images = analyzeImages(resources, width, height);
            List<String> warnings = generateWarnings(colorspaces, fontIssues, images);
            
            // Build analysis result
            return new PdfPageAnalysis.Builder(pageNum, width, height, orientation)
                .colorspaces(colorspaces)
                .fontIssues(fontIssues)
                .images(images)
                .warnings(warnings)
                .build();
                
        } catch (Exception e) {
            logger.error("Error analyzing page {}: {}", pageNum, e.getMessage(), e);
            // Return minimal analysis on error
            return new PdfPageAnalysis.Builder(pageNum, 0, 0, "unknown")
                .warnings(List.of("Error during page analysis: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Extracts all colorspaces used on the page.
     */
    private List<String> extractColorspaces(PDResources resources) {
        List<String> colorspaces = new ArrayList<>();
        
        try {
            // Get colorspaces from resources
            Iterable<COSName> csNames = resources.getColorSpaceNames();
            if (csNames != null) {
                for (COSName csName : csNames) {
                    try {
                        PDColorSpace colorSpace = resources.getColorSpace(csName);
                        String csType = categorizeColorSpace(colorSpace);
                        if (!colorspaces.contains(csType)) {
                            colorspaces.add(csType);
                        }
                    } catch (Exception e) {
                        logger.debug("Could not extract colorspace {}: {}", csName.getName(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting colorspaces: {}", e.getMessage());
        }
        
        return colorspaces.isEmpty() ? List.of("Unknown") : colorspaces;
    }
    
    /**
     * Categorizes a colorspace into a human-readable type.
     */
    private String categorizeColorSpace(PDColorSpace colorSpace) {
        if (colorSpace == null) return "Unknown";
        
        String name = colorSpace.getName();
        if (name == null) {
            name = colorSpace.getClass().getSimpleName();
        }
        
        if (name.contains("DeviceRGB") || name.contains("CalRGB")) {
            return "RGB";
        } else if (name.contains("DeviceCMYK") || name.contains("CalCMYK")) {
            return "CMYK";
        } else if (name.contains("DeviceGray") || name.contains("CalGray")) {
            return "Grayscale";
        } else if (name.contains("Indexed")) {
            return "Indexed";
        } else if (name.contains("Lab")) {
            return "Lab";
        } else if (name.contains("ICC")) {
            return "ICC Profile";
        } else if (name.contains("Separation")) {
            return "Separation";
        } else if (name.contains("Pattern")) {
            return "Pattern";
        }
        
        return name;
    }
    
    /**
     * Analyzes fonts on the page for embedding and other issues.
     */
    private List<FontIssue> analyzeFonts(PDResources resources) {
        List<FontIssue> issues = new ArrayList<>();
        
        try {
            Iterable<COSName> fontNames = resources.getFontNames();
            for (COSName fontName : fontNames) {
                try {
                    PDFont font = resources.getFont(fontName);
                    if (font == null) continue;
                    
                    String fontBaseName = font.getName();
                    
                    // Check if font is embedded
                    if (!font.isEmbedded()) {
                        // Standard 14 fonts don't need embedding
                        if (!isStandard14Font(font)) {
                            issues.add(new FontIssue(
                                FontIssue.IssueType.UNEMBEDDED,
                                fontBaseName,
                                FontIssue.Severity.WARNING,
                                String.format("Font '%s' is not embedded", fontBaseName)
                            ));
                        }
                    }
                    
                    // Check for CID font issues (Type 0 fonts)
                    if (font instanceof PDType0Font) {
                        PDType0Font type0Font = (PDType0Font) font;
                        // Type 0 fonts can have missing glyph issues
                        if (!font.isEmbedded()) {
                            issues.add(new FontIssue(
                                FontIssue.IssueType.CID_FONT_ISSUE,
                                fontBaseName,
                                FontIssue.Severity.CRITICAL,
                                String.format("CID font '%s' is not embedded - may cause rendering issues", fontBaseName)
                            ));
                        }
                    }
                    
                    // Check for Type 3 fonts (bitmap fonts, often problematic)
                    if (font instanceof PDType3Font) {
                        issues.add(new FontIssue(
                            FontIssue.IssueType.UNEMBEDDED,
                            fontBaseName,
                            FontIssue.Severity.WARNING,
                            String.format("Type 3 font '%s' may not render correctly", fontBaseName)
                        ));
                    }
                    
                } catch (Exception e) {
                    logger.debug("Error analyzing font {}: {}", fontName.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.debug("Error iterating fonts: {}", e.getMessage());
        }
        
        return issues;
    }
    
    /**
     * Checks if a font is one of the PDF Standard 14 fonts.
     */
    private boolean isStandard14Font(PDFont font) {
        if (font == null || font.getName() == null) return false;
        
        String fontName = font.getName().toLowerCase();
        return fontName.equals("times-roman") ||
               fontName.equals("times-bold") ||
               fontName.equals("times-italic") ||
               fontName.equals("times-bolditalic") ||
               fontName.equals("helvetica") ||
               fontName.equals("helvetica-bold") ||
               fontName.equals("helvetica-oblique") ||
               fontName.equals("helvetica-boldoblique") ||
               fontName.equals("courier") ||
               fontName.equals("courier-bold") ||
               fontName.equals("courier-oblique") ||
               fontName.equals("courier-boldoblique") ||
               fontName.equals("symbol") ||
               fontName.equals("zapfdingbats");
    }
    
    /**
     * Analyzes images on the page for resolution and colorspace information.
     */
    private List<ImageInfo> analyzeImages(PDResources resources, float pageWidth, float pageHeight) {
        List<ImageInfo> images = new ArrayList<>();
        
        try {
            Iterable<COSName> xObjectNames = resources.getXObjectNames();
            if (xObjectNames == null) return images;
            
            for (COSName xObjectName : xObjectNames) {
                try {
                    PDXObject xObject = resources.getXObject(xObjectName);
                    
                    // Only process image XObjects
                    if (xObject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject) xObject;
                        
                        int pixelWidth = image.getWidth();
                        int pixelHeight = image.getHeight();
                        
                        // Get display dimensions (approximation - use page size as reference)
                        float displayWidth = pageWidth;
                        float displayHeight = pageHeight;
                        
                        // Get image colorspace
                        String colorspace = "Unknown";
                        if (image.getColorSpace() != null) {
                            colorspace = categorizeColorSpace(image.getColorSpace());
                        }
                        
                        ImageInfo imageInfo = new ImageInfo(
                            xObjectName.getName(),
                            0, 0, // Position not easily available
                            displayWidth, displayHeight,
                            pixelWidth, pixelHeight,
                            colorspace
                        );
                        
                        images.add(imageInfo);
                    }
                } catch (Exception e) {
                    logger.debug("Error analyzing image {}: {}", xObjectName.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.debug("Error iterating XObjects: {}", e.getMessage());
        }
        
        return images;
    }
    
    /**
     * Generates warnings based on analysis results.
     */
    private List<String> generateWarnings(List<String> colorspaces, List<FontIssue> fontIssues, 
                                         List<ImageInfo> images) {
        List<String> warnings = new ArrayList<>();
        
        // Check for RGB colorspaces (problematic for print)
        if (colorspaces.contains("RGB")) {
            warnings.add("Page contains RGB colorspaces - may not be print-ready");
        }
        
        // Check for low-resolution images
        long lowResCount = images.stream().filter(ImageInfo::isLowRes).count();
        if (lowResCount > 0) {
            warnings.add(String.format("%d low-resolution image(s) found (< 150 DPI)", lowResCount));
        }
        
        // Check for unembedded fonts
        long unembeddedCount = fontIssues.stream()
            .filter(issue -> issue.getType() == FontIssue.IssueType.UNEMBEDDED)
            .count();
        if (unembeddedCount > 0) {
            warnings.add(String.format("%d unembedded font(s) found", unembeddedCount));
        }
        
        return warnings;
    }
}
