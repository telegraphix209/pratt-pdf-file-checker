package com.preflight.config;

/**
 * Configuration class for PDF preflight operations.
 * Uses builder pattern for flexible and readable construction.
 */
public class PdfPreflightConfig {
    
    private final String inputPdfPath;
    private final String outputJsonPath;
    private final String outputTextPath;
    private final boolean useCropBox;
    private final float tolerance;
    private final boolean enableRasterization;
    private final String muPdfToolPath;
    private final int rasterDpi;
    private final int[] pagesToRasterize;
    private final String rasterOutputDir;
    
    private PdfPreflightConfig(Builder builder) {
        this.inputPdfPath = builder.inputPdfPath;
        this.outputJsonPath = builder.outputJsonPath;
        this.outputTextPath = builder.outputTextPath;
        this.useCropBox = builder.useCropBox;
        this.tolerance = builder.tolerance;
        this.enableRasterization = builder.enableRasterization;
        this.muPdfToolPath = builder.muPdfToolPath;
        this.rasterDpi = builder.rasterDpi;
        this.pagesToRasterize = builder.pagesToRasterize;
        this.rasterOutputDir = builder.rasterOutputDir;
    }
    
    public String getInputPdfPath() {
        return inputPdfPath;
    }
    
    public String getOutputJsonPath() {
        return outputJsonPath;
    }
    
    public String getOutputTextPath() {
        return outputTextPath;
    }
    
    public boolean isUseCropBox() {
        return useCropBox;
    }
    
    public float getTolerance() {
        return tolerance;
    }
    
    public boolean isEnableRasterization() {
        return enableRasterization;
    }
    
    public String getMuPdfToolPath() {
        return muPdfToolPath;
    }
    
    public int getRasterDpi() {
        return rasterDpi;
    }
    
    public int[] getPagesToRasterize() {
        return pagesToRasterize;
    }
    
    public String getRasterOutputDir() {
        return rasterOutputDir;
    }
    
    public static Builder builder(String inputPdfPath) {
        return new Builder(inputPdfPath);
    }
    
    public static class Builder {
        private final String inputPdfPath;
        private String outputJsonPath = "preflight-report.json";
        private String outputTextPath = "preflight-report.txt";
        private boolean useCropBox = true;
        private float tolerance = 0.01f;
        private boolean enableRasterization = false;
        private String muPdfToolPath = "mutool";
        private int rasterDpi = 150;
        private int[] pagesToRasterize;
        private String rasterOutputDir = "rasterized-pages";
        
        public Builder(String inputPdfPath) {
            this.inputPdfPath = inputPdfPath;
        }
        
        public Builder outputJsonPath(String path) {
            this.outputJsonPath = path;
            return this;
        }
        
        public Builder outputTextPath(String path) {
            this.outputTextPath = path;
            return this;
        }
        
        public Builder useCropBox(boolean useCropBox) {
            this.useCropBox = useCropBox;
            return this;
        }
        
        public Builder tolerance(float tolerance) {
            this.tolerance = tolerance;
            return this;
        }
        
        public Builder enableRasterization(boolean enable) {
            this.enableRasterization = enable;
            return this;
        }
        
        public Builder muPdfToolPath(String path) {
            this.muPdfToolPath = path;
            return this;
        }
        
        public Builder rasterDpi(int dpi) {
            this.rasterDpi = dpi;
            return this;
        }
        
        public Builder pagesToRasterize(int[] pages) {
            this.pagesToRasterize = pages;
            return this;
        }
        
        public Builder rasterOutputDir(String dir) {
            this.rasterOutputDir = dir;
            return this;
        }
        
        public PdfPreflightConfig build() {
            return new PdfPreflightConfig(this);
        }
    }
}
