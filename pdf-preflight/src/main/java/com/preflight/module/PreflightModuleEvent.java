package com.preflight.module;

import com.preflight.model.PdfPreflightResult;
import java.util.Map;

/**
 * Event object passed to callbacks and webhooks during preflight workflow.
 * Contains all context needed for the next module in the chain.
 */
public class PreflightModuleEvent {
    
    public enum EventType {
        VALIDATION_PASS,
        VALIDATION_FAIL,
        RASTERIZER_REQUESTED,
        MODULE_COMPLETE
    }
    
    private final EventType type;
    private final String workflowId;
    private final String sessionId;
    private final String userId;
    private final String inputPdfPath;
    private final String fileName;
    private final long fileSize;
    private final PdfPreflightResult preflightResult;
    private final String preparedPdfPath;
    private final Map<String, Object> rasterizedImages;
    private final long timestamp;
    private final Map<String, Object> metadata;
    
    private PreflightModuleEvent(Builder builder) {
        this.type = builder.type;
        this.workflowId = builder.workflowId;
        this.sessionId = builder.sessionId;
        this.userId = builder.userId;
        this.inputPdfPath = builder.inputPdfPath;
        this.fileName = builder.fileName;
        this.fileSize = builder.fileSize;
        this.preflightResult = builder.preflightResult;
        this.preparedPdfPath = builder.preparedPdfPath;
        this.rasterizedImages = builder.rasterizedImages;
        this.timestamp = System.currentTimeMillis();
        this.metadata = builder.metadata;
    }
    
    public static Builder builder(EventType type) {
        return new Builder(type);
    }
    
    // Getters
    public EventType getType() { return type; }
    public String getWorkflowId() { return workflowId; }
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getInputPdfPath() { return inputPdfPath; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public PdfPreflightResult getPreflightResult() { return preflightResult; }
    public String getPreparedPdfPath() { return preparedPdfPath; }
    public Map<String, Object> getRasterizedImages() { return rasterizedImages; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    public boolean isPass() {
        return type == EventType.VALIDATION_PASS;
    }
    
    public boolean isFail() {
        return type == EventType.VALIDATION_FAIL;
    }
    
    public static class Builder {
        private final EventType type;
        private String workflowId;
        private String sessionId;
        private String userId;
        private String inputPdfPath;
        private String fileName;
        private long fileSize;
        private PdfPreflightResult preflightResult;
        private String preparedPdfPath;
        private Map<String, Object> rasterizedImages;
        private Map<String, Object> metadata;
        
        public Builder(EventType type) {
            this.type = type;
        }
        
        public Builder workflowId(String id) {
            this.workflowId = id;
            return this;
        }
        
        public Builder sessionId(String id) {
            this.sessionId = id;
            return this;
        }
        
        public Builder userId(String id) {
            this.userId = id;
            return this;
        }
        
        public Builder inputPdfPath(String path) {
            this.inputPdfPath = path;
            return this;
        }
        
        public Builder fileName(String name) {
            this.fileName = name;
            return this;
        }
        
        public Builder fileSize(long size) {
            this.fileSize = size;
            return this;
        }
        
        public Builder preflightResult(PdfPreflightResult result) {
            this.preflightResult = result;
            return this;
        }
        
        public Builder preparedPdfPath(String path) {
            this.preparedPdfPath = path;
            return this;
        }
        
        public Builder rasterizedImages(Map<String, Object> images) {
            this.rasterizedImages = images;
            return this;
        }
        
        public Builder metadata(Map<String, Object> meta) {
            this.metadata = meta;
            return this;
        }
        
        public PreflightModuleEvent build() {
            return new PreflightModuleEvent(this);
        }
    }
}
