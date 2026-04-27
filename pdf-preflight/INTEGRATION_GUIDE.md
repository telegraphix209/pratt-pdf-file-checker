# PDF Preflight Module - Web-to-Print Integration Guide

## 📦 Module Architecture

The PDF Preflight system is now designed as a **standalone module** that can be integrated into a larger web-to-print workflow. It communicates with other modules via **events** and **webhooks**.

```
┌─────────────────────────────────────────────────────────────┐
│               Your Web-to-Print Application                  │
│                                                              │
│  ┌──────────────┐      ┌──────────────┐      ┌───────────┐ │
│  │   File       │─────▶│   PREFLIGHT  │─────▶│   NEXT    │ │
│  │   Upload     │      │    MODULE    │      │  MODULE   │ │
│  │   Module     │      │              │      │ (RIP, etc)│ │
│  └──────────────┘      └──────────────┘      └───────────┘ │
│                         │                    ▲               │
│                         │ Webhook on PASS    │               │
│                         └────────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔧 Integration Methods

### **Method 1: Webhook Integration (Recommended)**

The preflight module sends HTTP POST notifications to your application when validation completes.

#### **Configuration**:

```java
PreflightModuleConfig config = PreflightModuleConfig.builder()
    .workflowId("order-12345")
    .sessionId("session-abc")
    .userId("user-789")
    .webhookOnPass("https://your-app.com/api/webhooks/preflight-pass")
    .webhookOnFail("https://your-app.com/api/webhooks/preflight-fail")
    .showRasterizerOnlyOnFailure(true)  // Only show when complex issues detected
    .build();
```

#### **Webhook Payload (PASS)**:

```json
{
  "eventType": "VALIDATION_PASS",
  "timestamp": 1714089600000,
  "workflowId": "order-12345",
  "sessionId": "session-abc",
  "userId": "user-789",
  "file": {
    "inputPdfPath": "/tmp/preflight-abc123.pdf",
    "fileName": "business-cards.pdf",
    "fileSize": 2458624
  },
  "preflight": {
    "passed": true,
    "totalPages": 4,
    "mismatchCount": 0,
    "warningCount": 2,
    "referencePage": {
      "pageNumber": 1,
      "width": 612.0,
      "height": 792.0
    }
  },
  "preparedPdfPath": "/tmp/prepared-xyz789.pdf"
}
```

#### **Webhook Payload (FAIL)**:

```json
{
  "eventType": "VALIDATION_FAIL",
  "timestamp": 1714089600000,
  "workflowId": "order-12345",
  "sessionId": "session-abc",
  "userId": "user-789",
  "file": {
    "inputPdfPath": "/tmp/preflight-abc123.pdf",
    "fileName": "brochure.pdf",
    "fileSize": 15728640
  },
  "preflight": {
    "passed": false,
    "totalPages": 12,
    "mismatchCount": 3,
    "warningCount": 5,
    "referencePage": {
      "pageNumber": 1,
      "pageNumber": 1,
      "width": 612.0,
      "height": 792.0
    }
  }
}
```

#### **Your Webhook Handler** (Example in Node.js):

```javascript
app.post('/api/webhooks/preflight-pass', (req, res) => {
  const event = req.body;
  
  // File passed validation - send to next module
  console.log(`File ${event.file.fileName} passed preflight`);
  
  // Forward to RIP module, imposition module, etc.
  await sendToNextModule({
    workflowId: event.workflowId,
    sessionId: event.sessionId,
    pdfPath: event.preparedPdfPath || event.file.inputPdfPath,
    preflightResult: event.preflight
  });
  
  res.status(200).send('OK');
});

app.post('/api/webhooks/preflight-fail', (req, res) => {
  const event = req.body;
  
  // File failed - notify user or trigger manual review
  console.log(`File ${event.file.fileName} failed preflight`);
  
  // Show rasterizer if complex PostScript/transparency issues
  if (hasComplexIssues(event.preflight)) {
    showRasterizerUI(event.file.inputPdfPath);
  }
  
  res.status(200).send('OK');
});
```

---

### **Method 2: JavaScript Event Callbacks (Embed Mode)**

When embedding the preflight UI in an iframe or as a component:

```javascript
// In your parent application
window.addEventListener('message', (event) => {
  if (event.data.type === 'PREFLIGHT_PASS') {
    console.log('File passed:', event.data);
    
    // Send to next module
    fetch('/api/next-module', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        workflowId: event.data.workflowId,
        pdfPath: event.data.preparedPdfPath || event.data.inputPdfPath,
        preflightResult: event.data.preflightResult
      })
    });
  }
  
  if (event.data.type === 'PREFLIGHT_FAIL') {
    console.log('File failed:', event.data);
    
    // Show rasterizer only if complex issues
    if (event.data.hasComplexIssues) {
      enableRasterizer(event.data.inputPdfPath);
    }
  }
});
```

---

### **Method 3: REST API (Headless Mode)**

For server-to-server integration without UI:

```bash
# Upload and validate
curl -X POST http://localhost:8080/api/preflight \
  -F "file=@document.pdf" \
  -F "workflowId=order-12345" \
  -F "webhookOnPass=https://your-app.com/webhook/pass" \
  -F "webhookOnFail=https://your-app.com/webhook/fail"
```

---

## 🖼️ Conditional Rasterizer Display

The rasterizer is **only shown when needed** for complex issues:

### **When Rasterizer Appears**:

1. **Validation FAIL** with:
   - Complex PostScript operators detected
   - Transparency/blend mode issues
   - Overprint problems
   - ICC profile conflicts

2. **User explicitly requests** page inspection

### **Configuration**:

```java
PreflightModuleConfig config = PreflightModuleConfig.builder()
    .showRasterizerOnlyOnFailure(true)  // Default: true
    .rasterizerDpi(200)                  // Higher DPI for detailed inspection
    .maxRasterPages(5)                   // Limit for performance
    .onRasterizerRequested((event) -> {
        // Log when user requests rasterization
        log.info("User requested rasterization for complex file inspection");
    })
    .build();
```

### **Frontend Behavior**:

```javascript
// Rasterizer checkbox is HIDDEN by default
// Only shown when validation FAILS with complex issues

if (validationFailed && hasComplexIssues(result)) {
    showRasterizerOption();
}

function hasComplexIssues(result) {
    // Check for transparency, complex PostScript, etc.
    return result.hasTransparencyIssues || 
           result.hasComplexPostScript ||
           result.hasOverprintIssues;
}
```

---

## 🔄 Passing to Next Module

### **What Gets Passed**:

When validation **PASSES**, the module provides:

1. **File Path**: Path to prepared/validated PDF
2. **Preflight Report**: Complete validation results
3. **Context Data**: workflowId, sessionId, userId
4. **Metadata**: Any custom data you attached

### **Integration Example**:

```java
// In your web-to-print application
PreflightModuleConfig config = PreflightModuleConfig.builder()
    .workflowId("print-job-12345")
    .sessionId("session-abc")
    .userId("customer-789")
    
    // Webhook for server-side integration
    .webhookOnPass("https://your-app.com/api/preflight-passed")
    
    // Or JavaScript callback for client-side
    .onValidationPass((event) -> {
        // Send to next module (RIP, imposition, etc.)
        fetch('/api/rip-module', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                workflowId: event.getWorkflowId(),
                pdfPath: event.getPreparedPdfPath() || event.getInputPdfPath(),
                preflightResult: event.getPreflightResult(),
                metadata: event.getMetadata()
            })
        });
    })
    
    .build();
```

---

## 📋 Complete Integration Example

### **Scenario**: Customer uploads PDF → Preflight → RIP Module

```javascript
// 1. Customer uploads file
const uploadResponse = await uploadFile(file);

// 2. Configure preflight module
const config = {
    workflowId: uploadResponse.workflowId,
    sessionId: uploadResponse.sessionId,
    userId: currentUser.id,
    
    // Notify your backend when done
    webhookOnPass: 'https://your-app.com/api/preflight-complete',
    webhookOnFail: 'https://your-app.com/api/preflight-failed',
    
    // Only show rasterizer for complex failures
    showRasterizerOnlyOnFailure: true
};

// 3. Open preflight UI (iframe or embedded)
openPreflightUI(uploadResponse.filePath, config);

// 4. Your backend receives webhook
app.post('/api/preflight-complete', async (req, res) => {
    const { workflowId, preparedPdfPath, preflight } = req.body;
    
    // 5. Send to RIP module for processing
    const ripJob = await createRipJob({
        workflowId,
        pdfPath: preparedPdfPath,
        settings: {
            resolution: 2400,
            screening: 'stochastic',
            colorProfile: 'GRACoL'
        }
    });
    
    // 6. Update order status
    await updateOrderStatus(workflowId, 'RIP_PROCESSING');
    
    res.status(200).send('OK');
});
```

---

## 🎯 Module Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `autoStartValidation` | boolean | `true` | Start validation on file upload |
| `showRasterizerOnlyOnFailure` | boolean | `true` | Only show rasterizer for complex failures |
| `enablePrintPreparation` | boolean | `true` | Allow print-ready PDF generation |
| `enableExport` | boolean | `true` | Allow report export |
| `webhookOnPass` | string | `null` | URL to notify on validation pass |
| `webhookOnFail` | string | `null` | URL to notify on validation fail |
| `workflowId` | string | `null` | Your workflow/order ID |
| `sessionId` | string | `null` | Session identifier |
| `userId` | string | `null` | User/customer identifier |
| `rasterizerDpi` | int | `150` | DPI for page rasterization |
| `maxRasterPages` | int | `10` | Max pages to rasterize |

---

## 🔐 Security Considerations

1. **Webhook Authentication**: Add HMAC signature verification
2. **File Paths**: Use temporary paths with automatic cleanup
3. **Session Validation**: Verify sessionId before processing
4. **Rate Limiting**: Prevent abuse of webhook endpoints

---

## 🚀 Next Steps

1. **Define your webhook endpoints** in your web-to-print application
2. **Configure the module** with your workflow IDs
3. **Test the integration** with sample PDFs
4. **Monitor webhook delivery** and handle retries
5. **Implement error handling** for failed validations

---

**The preflight module is now ready for integration!** 🎉
