package com.preflight.module;

import java.util.function.Consumer;

/**
 * Configuration for integrating PDF Preflight Module into larger Web-to-Print workflow.
 * Supports event-driven architecture with callbacks for pass/fail scenarios.
 */
public class PreflightModuleConfig {
    
    // Module behavior flags
    private boolean autoStartValidation = true;
    private boolean showRasterizerOnlyOnFailure = true;
    private boolean enablePrintPreparation = true;
    private boolean enableExport = true;
    
    // Integration callbacks
    private Consumer<PreflightModuleEvent> onValidationPass;
    private Consumer<PreflightModuleEvent> onValidationFail;
    private Consumer<PreflightModuleEvent> onRasterizerRequested;
    private Consumer<PreflightModuleEvent> onModuleComplete;
    
    // Webhook URLs for external integration
    private String webhookOnPass;
    private String webhookOnFail;
    
    // Module context (for passing to next module)
    private String workflowId;
    private String sessionId;
    private String userId;
    
    // Rasterizer settings (only used when needed)
    private int rasterizerDpi = 150;
    private int maxRasterPages = 10;
    
    private PreflightModuleConfig() {}
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public boolean isAutoStartValidation() { return autoStartValidation; }
    public boolean isShowRasterizerOnlyOnFailure() { return showRasterizerOnlyOnFailure; }
    public boolean isEnablePrintPreparation() { return enablePrintPreparation; }
    public boolean isEnableExport() { return enableExport; }
    public Consumer<PreflightModuleEvent> getOnValidationPass() { return onValidationPass; }
    public Consumer<PreflightModuleEvent> getOnValidationFail() { return onValidationFail; }
    public Consumer<PreflightModuleEvent> getOnRasterizerRequested() { return onRasterizerRequested; }
    public Consumer<PreflightModuleEvent> getOnModuleComplete() { return onModuleComplete; }
    public String getWebhookOnPass() { return webhookOnPass; }
    public String getWebhookOnFail() { return webhookOnFail; }
    public String getWorkflowId() { return workflowId; }
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public int getRasterizerDpi() { return rasterizerDpi; }
    public int getMaxRasterPages() { return maxRasterPages; }
    
    public static class Builder {
        private final PreflightModuleConfig config = new PreflightModuleConfig();
        
        public Builder autoStartValidation(boolean value) {
            config.autoStartValidation = value;
            return this;
        }
        
        public Builder showRasterizerOnlyOnFailure(boolean value) {
            config.showRasterizerOnlyOnFailure = value;
            return this;
        }
        
        public Builder enablePrintPreparation(boolean value) {
            config.enablePrintPreparation = value;
            return this;
        }
        
        public Builder enableExport(boolean value) {
            config.enableExport = value;
            return this;
        }
        
        public Builder onValidationPass(Consumer<PreflightModuleEvent> callback) {
            config.onValidationPass = callback;
            return this;
        }
        
        public Builder onValidationFail(Consumer<PreflightModuleEvent> callback) {
            config.onValidationFail = callback;
            return this;
        }
        
        public Builder onRasterizerRequested(Consumer<PreflightModuleEvent> callback) {
            config.onRasterizerRequested = callback;
            return this;
        }
        
        public Builder onModuleComplete(Consumer<PreflightModuleEvent> callback) {
            config.onModuleComplete = callback;
            return this;
        }
        
        public Builder webhookOnPass(String url) {
            config.webhookOnPass = url;
            return this;
        }
        
        public Builder webhookOnFail(String url) {
            config.webhookOnFail = url;
            return this;
        }
        
        public Builder workflowId(String id) {
            config.workflowId = id;
            return this;
        }
        
        public Builder sessionId(String id) {
            config.sessionId = id;
            return this;
        }
        
        public Builder userId(String id) {
            config.userId = id;
            return this;
        }
        
        public Builder rasterizerDpi(int dpi) {
            config.rasterizerDpi = dpi;
            return this;
        }
        
        public Builder maxRasterPages(int pages) {
            config.maxRasterPages = pages;
            return this;
        }
        
        public PreflightModuleConfig build() {
            return config;
        }
    }
}
