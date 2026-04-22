// PDF Preview Module using PDF.js

const PDFPreview = {
    pdfDoc: null,
    currentPage: 1,
    totalPages: 0,
    scale: 1.5,
    canvas: null,
    ctx: null,
    mismatchedPages: new Set(),
    rendering: false,

    // Initialize PDF.js worker
    init() {
        pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
        this.canvas = document.getElementById('pdfCanvas');
        this.ctx = this.canvas.getContext('2d');
    },

    // Load PDF file
    async loadPDF(file) {
        try {
            const arrayBuffer = await file.arrayBuffer();
            this.pdfDoc = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
            this.totalPages = this.pdfDoc.numPages;
            this.currentPage = 1;
            
            console.log(`PDF loaded: ${this.totalPages} pages`);
            return this.totalPages;
        } catch (error) {
            console.error('Error loading PDF:', error);
            throw new Error('Failed to load PDF for preview');
        }
    },

    // Set mismatched pages for highlighting
    setMismatchedPages(pages) {
        this.mismatchedPages = new Set(pages);
    },

    // Render current page
    async renderPage(pageNum = this.currentPage) {
        if (!this.pdfDoc || this.rendering) return;
        
        this.rendering = true;
        this.currentPage = pageNum;
        
        try {
            const page = await this.pdfDoc.getPage(pageNum);
            const viewport = page.getViewport({ scale: this.scale });
            
            this.canvas.height = viewport.height;
            this.canvas.width = viewport.width;
            
            const renderContext = {
                canvasContext: this.ctx,
                viewport: viewport
            };
            
            await page.render(renderContext).promise;
            
            // Update page info
            document.getElementById('pageInfo').textContent = 
                `Page ${pageNum} of ${this.totalPages}`;
            
            // Update navigation buttons
            document.getElementById('prevPageBtn').disabled = pageNum <= 1;
            document.getElementById('nextPageBtn').disabled = pageNum >= this.totalPages;
            
            // Highlight if mismatch
            if (this.mismatchedPages.has(pageNum)) {
                this.canvas.style.border = '3px solid #ef4444';
            } else {
                this.canvas.style.border = 'none';
            }
            
            // Update active thumbnail
            this.updateActiveThumbnail(pageNum);
            
        } catch (error) {
            console.error('Error rendering page:', error);
        } finally {
            this.rendering = false;
        }
    },

    // Generate thumbnails for all pages
    async generateThumbnails() {
        const strip = document.getElementById('thumbnailStrip');
        strip.innerHTML = '';
        
        // Limit thumbnails to first 50 pages for performance
        const maxThumbnails = Math.min(this.totalPages, 50);
        
        for (let i = 1; i <= maxThumbnails; i++) {
            const thumb = document.createElement('canvas');
            thumb.className = 'thumbnail';
            thumb.dataset.page = i;
            thumb.width = 80;
            thumb.height = 100;
            
            if (this.mismatchedPages.has(i)) {
                thumb.classList.add('mismatch');
            }
            
            thumb.addEventListener('click', () => {
                this.renderPage(i);
            });
            
            strip.appendChild(thumb);
            
            // Render thumbnail
            try {
                const page = await this.pdfDoc.getPage(i);
                const viewport = page.getViewport({ scale: 0.2 });
                const ctx = thumb.getContext('2d');
                thumb.height = viewport.height;
                thumb.width = viewport.width;
                
                await page.render({
                    canvasContext: ctx,
                    viewport: viewport
                }).promise;
            } catch (error) {
                console.error(`Error rendering thumbnail ${i}:`, error);
            }
        }
        
        if (this.totalPages > 50) {
            const note = document.createElement('div');
            note.style.cssText = 'padding: 10px; color: #6b7280; font-size: 0.875rem;';
            note.textContent = `+ ${this.totalPages - 50} more pages`;
            strip.appendChild(note);
        }
    },

    // Update active thumbnail
    updateActiveThumbnail(pageNum) {
        document.querySelectorAll('.thumbnail').forEach(thumb => {
            thumb.classList.remove('active');
            if (parseInt(thumb.dataset.page) === pageNum) {
                thumb.classList.add('active');
            }
        });
    },

    // Navigation
    nextPage() {
        if (this.currentPage < this.totalPages) {
            this.renderPage(this.currentPage + 1);
        }
    },

    prevPage() {
        if (this.currentPage > 1) {
            this.renderPage(this.currentPage - 1);
        }
    },

    // Zoom controls
    zoomIn() {
        if (this.scale < 3.0) {
            this.scale += 0.25;
            this.renderPage();
        }
    },

    zoomOut() {
        if (this.scale > 0.5) {
            this.scale -= 0.25;
            this.renderPage();
        }
    },

    // Reset state
    reset() {
        this.pdfDoc = null;
        this.currentPage = 1;
        this.totalPages = 0;
        this.scale = 1.5;
        this.mismatchedPages.clear();
        this.rendering = false;
        
        if (this.ctx && this.canvas) {
            this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        }
        
        const thumbnailStrip = document.getElementById('thumbnailStrip');
        if (thumbnailStrip) thumbnailStrip.innerHTML = '';
        
        const pageInfo = document.getElementById('pageInfo');
        if (pageInfo) pageInfo.textContent = 'Page 0 of 0';
    }
};

// Initialize on load
document.addEventListener('DOMContentLoaded', () => {
    PDFPreview.init();
});
