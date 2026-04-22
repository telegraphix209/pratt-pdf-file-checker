// Main Application Logic

const App = {
    selectedFile: null,
    currentPdfPath: null,
    preparedPdfPath: null,
    preparedFileName: null,

    // Initialize the application
    init() {
        this.setupEventListeners();
        console.log('PDF Preflight Web UI initialized');
    },

    // Setup all event listeners
    setupEventListeners() {
        const uploadZone = document.getElementById('uploadZone');
        const fileInput = document.getElementById('fileInput');
        
        // Drag and drop
        uploadZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadZone.classList.add('drag-over');
        });
        
        uploadZone.addEventListener('dragleave', () => {
            uploadZone.classList.remove('drag-over');
        });
        
        uploadZone.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadZone.classList.remove('drag-over');
            
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                this.handleFileSelect(files[0]);
            }
        });
        
        // File input
        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileSelect(e.target.files[0]);
            }
        });
        
        // Navigation buttons
        document.getElementById('prevPageBtn').addEventListener('click', () => {
            PDFPreview.prevPage();
        });
        
        document.getElementById('nextPageBtn').addEventListener('click', () => {
            PDFPreview.nextPage();
        });
        
        document.getElementById('zoomInBtn').addEventListener('click', () => {
            PDFPreview.zoomIn();
        });
        
        document.getElementById('zoomOutBtn').addEventListener('click', () => {
            PDFPreview.zoomOut();
        });
        
        // Export buttons
        document.getElementById('exportJsonBtn').addEventListener('click', () => {
            ReportViewer.exportJSON();
        });
        
        document.getElementById('exportTextBtn').addEventListener('click', () => {
            ReportViewer.exportText();
        });
        
        document.getElementById('resetBtn').addEventListener('click', () => {
            console.log('Reset button clicked');
            App.resetUI();
        });
        
        // Prepare for Print button
        document.getElementById('prepareForPrintBtn').addEventListener('click', () => {
            this.prepareForPrint();
        });
        
        // Modal close button
        document.getElementById('closeModalBtn').addEventListener('click', () => {
            this.closePrintPrepModal();
        });
        
        // Download prepared PDF button
        document.getElementById('downloadPreparedBtn').addEventListener('click', () => {
            this.downloadPreparedPDF();
        });
    },

    // Handle file selection
    handleFileSelect(file) {
        // Validate file type
        if (!file.type.includes('pdf') && !file.name.toLowerCase().endsWith('.pdf')) {
            this.showError('Please select a valid PDF file');
            return;
        }
        
        // Validate file size (1GB max)
        const maxSize = 1024 * 1024 * 1024; // 1GB
        if (file.size > maxSize) {
            this.showError('File size exceeds 1GB limit');
            return;
        }
        
        this.selectedFile = file;
        this.currentPdfPath = null; // Will be set after upload
        
        // Update UI
        const fileInfo = document.getElementById('fileInfo');
        fileInfo.textContent = `${file.name} (${this.formatFileSize(file.size)})`;
        
        console.log(`File selected: ${file.name}, size: ${file.size}`);
        
        // Auto-run preflight
        this.runPreflight();
    },

    // Run preflight validation
    async runPreflight() {
        if (!this.selectedFile) {
            this.showError('Please select a PDF file first');
            return;
        }
        
        try {
            // Show processing section
            this.showSection('processingSection');
            this.updateProcessingStatus('Uploading PDF...');
            
            // Get configuration
            const useCropBox = document.getElementById('useCropBox').checked;
            const tolerance = 0.5; // Default tolerance: 0.5 points
            
            // Prepare form data
            const formData = new FormData();
            formData.append('file', this.selectedFile);
            formData.append('useCropBox', useCropBox);
            formData.append('tolerance', tolerance);
            
            this.updateProcessingStatus('Validating PDF...');
            
            // Send to server
            const response = await fetch('/api/preflight', {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                throw new Error(`Server error: ${response.status}`);
            }
            
            const data = await response.json();
            
            if (!data.success) {
                throw new Error(data.message || 'Preflight validation failed');
            }
            
            // Store the PDF path for later use
            this.currentPdfPath = data.result.inputPdfPath || data.inputPdfPath;
            
            // Update processing status
            this.updateProcessingStatus('Generating preview...');
            
            // Display results
            await this.displayResults(data);
            
        } catch (error) {
            console.error('Preflight error:', error);
            this.showError(error.message || 'An error occurred during validation');
        }
    },
    
    // Auto prepare for print when file passes validation
    async autoPrepareForPrint(originalFileName) {
        try {
            console.log('Auto-preparing for print...');
            
            // Extract base name without extension
            const baseName = originalFileName.replace(/\.pdf$/i, '');
            
            // Prompt user for filename
            const suggestedName = `${baseName}_PFv.2.pdf`;
            const fileName = prompt('Enter filename for print-ready PDF:', suggestedName);
            
            // User cancelled
            if (fileName === null) {
                console.log('User cancelled filename prompt');
                return;
            }
            
            // Empty filename - use suggested
            const finalFileName = fileName.trim() || suggestedName;
            
            // Ensure it ends with .pdf
            const outputFileName = finalFileName.endsWith('.pdf') ? finalFileName : `${finalFileName}.pdf`;
            
            // Send request to server
            const response = await fetch('/api/prepare-for-print', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    inputPdfPath: this.currentPdfPath,
                    preflightResult: ReportViewer.currentResult?.result,
                    outputFileName: outputFileName
                })
            });
            
            if (!response.ok) {
                throw new Error(`Server error: ${response.status}`);
            }
            
            const data = await response.json();
            
            if (!data.success) {
                throw new Error(data.message || 'Failed to prepare PDF for print');
            }
            
            console.log('Print preparation complete:', outputFileName);
            
            // Download the file directly from the response
            await this.downloadFileFromPath(data.outputFilePath, outputFileName);
            
        } catch (error) {
            console.error('Auto prepare for print error:', error);
            // Don't show error - file already passed, user can manually prepare if needed
        }
    },
    
    // Download file from server path
    async downloadFileFromPath(filePath, fileName) {
        try {
            console.log('Downloading file from:', filePath);
            
            // Request the file from server
            const response = await fetch('/api/download-file', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    filePath: filePath,
                    fileName: fileName
                })
            });
            
            if (!response.ok) {
                throw new Error(`Download failed: ${response.status}`);
            }
            
            // Get the file as a blob
            const blob = await response.blob();
            
            // Create a download link
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            
            // Cleanup
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
            
            console.log('Download initiated:', fileName);
            
        } catch (error) {
            console.error('Download error:', error);
            alert('Failed to download file: ' + error.message);
        }
    },

    // Display validation results
    async displayResults(data) {
        const { result, fileName, fileSize, processingTime } = data;
        
        // Show results section
        this.showSection('resultsSection');
        
        // Check if preview should be skipped
        const skipPreview = document.getElementById('skipPreview').checked;
        
        // Get the preview and report panels
        const previewPanel = document.getElementById('previewPanel');
        const reportPanel = document.getElementById('reportPanel');
        const resultsContent = document.getElementById('resultsContent');
        
        if (skipPreview) {
            // Hide preview panel, show only report
            if (previewPanel) previewPanel.style.display = 'none';
            if (reportPanel) reportPanel.style.width = '100%';
            if (resultsContent) resultsContent.style.gridTemplateColumns = '1fr';
            
            // Display report immediately
            ReportViewer.displayResult(result, fileName, fileSize, processingTime);
        } else {
            // Show both panels
            if (previewPanel) previewPanel.style.display = 'block';
            if (reportPanel) reportPanel.style.width = '';
            if (resultsContent) resultsContent.style.gridTemplateColumns = '';
            
            // Display report
            ReportViewer.displayResult(result, fileName, fileSize, processingTime);
            
            // Load PDF preview
            try {
                const totalPages = await PDFPreview.loadPDF(this.selectedFile);
                
                // Set mismatched pages
                const mismatchedPages = result.mismatches ? 
                    result.mismatches.map(m => m.pageNumber) : [];
                PDFPreview.setMismatchedPages(mismatchedPages);
                
                // Generate thumbnails and render first page
                await PDFPreview.generateThumbnails();
                await PDFPreview.renderPage(1);
                
            } catch (error) {
                console.error('Error loading PDF preview:', error);
                // Continue even if preview fails
            }
        }
        
        // If file passes, automatically prepare for print
        if (result.passed) {
            console.log('File passed - auto-preparing for print');
            await this.autoPrepareForPrint(fileName);
        }
    },

    // Show error
    showError(message) {
        document.getElementById('errorMessage').textContent = message;
        this.showSection('errorSection');
    },

    // Show specific section, hide others
    showSection(sectionId) {
        const sections = ['uploadSection', 'processingSection', 'resultsSection', 'errorSection'];
        sections.forEach(id => {
            const element = document.getElementById(id);
            if (id === sectionId) {
                element.style.display = 'block';
            } else {
                element.style.display = 'none';
            }
        });
    },

    // Update processing status text
    updateProcessingStatus(text) {
        document.getElementById('processingStatus').textContent = text;
    },

    // Reset UI to initial state
    resetUI() {
        console.log('resetUI called');
        this.selectedFile = null;
        this.currentPdfPath = null;
        this.preparedPdfPath = null;
        this.preparedFileName = null;
        
        // Reset file info
        const fileInfo = document.getElementById('fileInfo');
        if (fileInfo) fileInfo.textContent = '';
        
        const fileInput = document.getElementById('fileInput');
        if (fileInput) fileInput.value = '';
        
        // Hide Prepare for Print button
        const printBtn = document.getElementById('prepareForPrintBtn');
        if (printBtn) printBtn.style.display = 'none';
        
        // Close modal if open
        this.closePrintPrepModal();
        
        // Reset preview and report panel styles
        const previewPanel = document.getElementById('previewPanel');
        if (previewPanel) previewPanel.style.display = '';
        
        const reportPanel = document.getElementById('reportPanel');
        if (reportPanel) reportPanel.style.width = '';
        
        const resultsContent = document.getElementById('resultsContent');
        if (resultsContent) resultsContent.style.gridTemplateColumns = '';
        
        // Reset components
        PDFPreview.reset();
        ReportViewer.reset();
        
        // Show upload section
        this.showSection('uploadSection');
        
        console.log('resetUI completed');
    },

    // Format file size
    formatFileSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
        if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
        return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
    },

    // Prepare PDF for print
    async prepareForPrint() {
        if (!this.currentPdfPath) {
            this.showError('No PDF file available');
            return;
        }
        
        try {
            // Show modal with processing indicator
            this.showPrintPrepModal();
            document.getElementById('prepProcessing').style.display = 'block';
            document.getElementById('prepResults').style.display = 'none';
            
            // Send request to server
            const response = await fetch('/api/prepare-for-print', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    inputPdfPath: this.currentPdfPath,
                    preflightResult: ReportViewer.currentResult?.result
                })
            });
            
            if (!response.ok) {
                throw new Error(`Server error: ${response.status}`);
            }
            
            const data = await response.json();
            
            if (!data.success) {
                throw new Error(data.message || 'Failed to prepare PDF for print');
            }
            
            // Store the output file path for download
            this.preparedPdfPath = data.outputFilePath;
            this.preparedFileName = data.outputFileName || 'prepared.pdf';
            
            // Display results
            this.displayPrintPrepResults(data);
            
        } catch (error) {
            console.error('Prepare for print error:', error);
            this.closePrintPrepModal();
            this.showError(error.message || 'Failed to prepare PDF for print');
        }
    },
    
    // Display print preparation results
    displayPrintPrepResults(data) {
        // Hide processing, show results
        document.getElementById('prepProcessing').style.display = 'none';
        document.getElementById('prepResults').style.display = 'block';
        
        // Display fixes applied
        const fixesDiv = document.getElementById('fixesApplied');
        if (data.fixesApplied && data.fixesApplied.length > 0) {
            fixesDiv.innerHTML = data.fixesApplied.map(fix => 
                `<div class="fix-item">✓ ${fix}</div>`
            ).join('');
        } else {
            fixesDiv.innerHTML = '<p style="color: var(--gray-600);">No fixes were necessary</p>';
        }
        
        // Display warnings
        const warningsDiv = document.getElementById('remainingWarnings');
        if (data.warnings && data.warnings.length > 0) {
            warningsDiv.innerHTML = data.warnings.map(warning => 
                `<div class="warning-item">⚠️ ${warning}</div>`
            ).join('');
        } else {
            warningsDiv.innerHTML = '<p style="color: var(--success-color);">✓ No remaining warnings</p>';
        }
    },
    
    // Download prepared PDF
    downloadPreparedPDF() {
        if (!this.preparedPdfPath) {
            alert('No prepared PDF available');
            return;
        }
        
        // Create a temporary link to download the file
        const link = document.createElement('a');
        link.href = `/api/download/${encodeURIComponent(this.preparedFileName)}`;
        link.download = this.preparedFileName;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    },
    
    // Show print preparation modal
    showPrintPrepModal() {
        document.getElementById('printPrepModal').style.display = 'block';
    },
    
    // Close print preparation modal
    closePrintPrepModal() {
        document.getElementById('printPrepModal').style.display = 'none';
    }
};

// Make resetUI globally accessible
function resetUI() {
    App.resetUI();
}

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    App.init();
});
