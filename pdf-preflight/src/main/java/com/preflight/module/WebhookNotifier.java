package com.preflight.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles webhook notifications to external systems.
 * Used to notify the next module in the web-to-print workflow.
 */
public class WebhookNotifier {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookNotifier.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int TIMEOUT_MS = 10000;
    
    /**
     * Sends webhook notification for a preflight event.
     * 
     * @param webhookUrl The URL to notify
     * @param event The preflight event data
     * @return true if notification was sent successfully
     */
    public static boolean sendWebhook(String webhookUrl, PreflightModuleEvent event) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.debug("No webhook URL configured, skipping notification");
            return false;
        }
        
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Event-Type", event.getType().name());
            conn.setRequestProperty("X-Workflow-Id", event.getWorkflowId() != null ? event.getWorkflowId() : "");
            conn.setRequestProperty("X-Session-Id", event.getSessionId() != null ? event.getSessionId() : "");
            
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            
            // Build payload
            Map<String, Object> payload = buildPayload(event);
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Webhook sent successfully to {} - Event: {}", webhookUrl, event.getType());
                return true;
            } else {
                logger.error("Webhook failed with response code: {} - URL: {}", responseCode, webhookUrl);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to send webhook to {}: {}", webhookUrl, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Builds the JSON payload for webhook notification.
     */
    private static Map<String, Object> buildPayload(PreflightModuleEvent event) {
        Map<String, Object> payload = new HashMap<>();
        
        // Event metadata
        payload.put("eventType", event.getType().name());
        payload.put("timestamp", event.getTimestamp());
        payload.put("workflowId", event.getWorkflowId());
        payload.put("sessionId", event.getSessionId());
        payload.put("userId", event.getUserId());
        
        // File information
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("inputPdfPath", event.getInputPdfPath());
        fileInfo.put("fileName", event.getFileName());
        fileInfo.put("fileSize", event.getFileSize());
        payload.put("file", fileInfo);
        
        // Preflight result
        if (event.getPreflightResult() != null) {
            Map<String, Object> preflightData = new HashMap<>();
            preflightData.put("passed", event.getPreflightResult().isPassed());
            preflightData.put("totalPages", event.getPreflightResult().getTotalPages());
            preflightData.put("mismatchCount", event.getPreflightResult().getMismatchCount());
            preflightData.put("warningCount", event.getPreflightResult().getWarningCount());
            
            if (event.getPreflightResult().getReferencePage() != null) {
                Map<String, Object> refPage = new HashMap<>();
                refPage.put("pageNumber", event.getPreflightResult().getReferencePage().getPageNumber());
                refPage.put("width", event.getPreflightResult().getReferencePage().getWidth());
                refPage.put("height", event.getPreflightResult().getReferencePage().getHeight());
                preflightData.put("referencePage", refPage);
            }
            
            payload.put("preflight", preflightData);
        }
        
        // Prepared PDF path (if available)
        if (event.getPreparedPdfPath() != null) {
            payload.put("preparedPdfPath", event.getPreparedPdfPath());
        }
        
        // Rasterized images (if available)
        if (event.getRasterizedImages() != null) {
            payload.put("rasterizedImages", event.getRasterizedImages());
        }
        
        // Additional metadata
        if (event.getMetadata() != null) {
            payload.put("metadata", event.getMetadata());
        }
        
        return payload;
    }
}
