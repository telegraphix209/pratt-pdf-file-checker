// Report Visualization Module

const ReportViewer = {
    currentResult: null,

    // Display preflight results
    displayResult(result, fileName, fileSize, processingTime) {
        this.currentResult = { result, fileName, fileSize, processingTime };
        
        // Update summary cards
        this.updateSummaryCards(result, processingTime);
        
        // Display reference page info
        this.displayReferenceInfo(result);
        
        // Display mismatches table
        this.displayMismatches(result);
        
        // Display page analysis table
        this.displayPageAnalyses(result);
    },

    // Update summary cards
    updateSummaryCards(result, processingTime) {
        const statusValue = document.getElementById('statusValue');
        const statusCard = document.getElementById('statusCard');
        
        if (statusValue && statusCard) {
            if (result.passed) {
                statusValue.textContent = 'PASS';
                statusValue.className = 'status-value pass';
                statusCard.className = 'card status-card pass';
            } else {
                statusValue.textContent = 'FAIL';
                statusValue.className = 'status-value fail';
                statusCard.className = 'card status-card fail';
            }
        }
        
        const totalPages = document.getElementById('totalPages');
        if (totalPages) totalPages.textContent = result.totalPages;
        
        const mismatchCount = document.getElementById('mismatchCount');
        if (mismatchCount) mismatchCount.textContent = result.mismatchCount || result.mismatches?.length || 0;
        
        const warningCount = document.getElementById('warningCount');
        if (warningCount) warningCount.textContent = result.warningCount || 0;
        
        const processingTimeEl = document.getElementById('processingTime');
        if (processingTimeEl) processingTimeEl.textContent = `${processingTime}ms`;
    },

    // Display reference page information
    displayReferenceInfo(result) {
        const refInfo = document.getElementById('referenceInfo');
        if (!refInfo) return;
        
        if (!result.referencePage) {
            refInfo.innerHTML = '<p>No reference page information available</p>';
            return;
        }
        
        const ref = result.referencePage;
        const widthInches = (ref.width / 72).toFixed(2);
        const heightInches = (ref.height / 72).toFixed(2);
        
        refInfo.innerHTML = `
            <h4>Reference Page (Page 1)</h4>
            <p><strong>Width:</strong> ${ref.width.toFixed(2)} pts (${widthInches} inches)</p>
            <p><strong>Height:</strong> ${ref.height.toFixed(2)} pts (${heightInches} inches)</p>
            <p><strong>Orientation:</strong> ${ref.orientation}</p>
            <p><strong>Box Used:</strong> ${ref.boxUsed}</p>
        `;
    },

    // Display mismatches table
    displayMismatches(result) {
        const tbody = document.getElementById('mismatchesBody');
        const section = document.getElementById('mismatchesSection');
        
        if (!tbody || !section) return;
        
        tbody.innerHTML = '';
        
        if (!result.mismatches || result.mismatches.length === 0) {
            section.innerHTML = `
                <h4>Mismatches Found</h4>
                <p style="color: #10b981; font-weight: 600; padding: 20px; text-align: center;">
                    ✓ No mismatches found - All pages match the reference dimensions and orientation
                </p>
            `;
            return;
        }
        
        section.innerHTML = '<h4>Mismatches Found</h4>';
        
        result.mismatches.forEach(mismatch => {
            const row = document.createElement('tr');
            
            row.innerHTML = `
                <td><span class="page-link" data-page="${mismatch.pageNumber}">Page ${mismatch.pageNumber}</span></td>
                <td>${mismatch.actualWidth.toFixed(2)} × ${mismatch.actualHeight.toFixed(2)}</td>
                <td>${mismatch.expectedWidth.toFixed(2)} × ${mismatch.expectedHeight.toFixed(2)}</td>
                <td>${mismatch.actualOrientation}</td>
                <td>${mismatch.mismatchReason}</td>
            `;
            
            tbody.appendChild(row);
            
            // Add click handler to navigate to page
            const pageLink = row.querySelector('.page-link');
            pageLink.addEventListener('click', () => {
                const pageNum = parseInt(pageLink.dataset.page);
                PDFPreview.renderPage(pageNum);
            });
        });
    },

    // Export report as JSON
    exportJSON() {
        if (!this.currentResult) return;
        
        const jsonStr = JSON.stringify(this.currentResult.result, null, 2);
        this.downloadFile(jsonStr, 'preflight-report.json', 'application/json');
    },

    // Export report as text
    exportText() {
        if (!this.currentResult) return;
        
        const result = this.currentResult.result;
        let text = '=== PDF Preflight Report ===\n';
        text += `Generated: ${new Date().toISOString()}\n`;
        text += `File: ${this.currentResult.fileName}\n`;
        text += `File Size: ${this.currentResult.fileSize}\n`;
        text += `Processing Time: ${this.currentResult.processingTime}ms\n\n`;
        
        text += `Status: ${result.passed ? 'PASS' : 'FAIL'}\n`;
        text += `Total Pages: ${result.totalPages}\n`;
        text += `Mismatches: ${result.mismatchCount || result.mismatches?.length || 0}\n\n`;
        
        if (result.referencePage) {
            const ref = result.referencePage;
            text += 'Reference Page (Page 1):\n';
            text += `  Width: ${ref.width.toFixed(2)} pts (${(ref.width / 72).toFixed(2)} inches)\n`;
            text += `  Height: ${ref.height.toFixed(2)} pts (${(ref.height / 72).toFixed(2)} inches)\n`;
            text += `  Orientation: ${ref.orientation}\n`;
            text += `  Box Used: ${ref.boxUsed}\n\n`;
        }
        
        if (result.mismatches && result.mismatches.length > 0) {
            text += `Mismatches Found: ${result.mismatches.length}\n\n`;
            
            result.mismatches.forEach(mismatch => {
                text += `Page ${mismatch.pageNumber}:\n`;
                text += `  Actual: ${mismatch.actualWidth.toFixed(2)} × ${mismatch.actualHeight.toFixed(2)} pts (${mismatch.actualOrientation})\n`;
                text += `  Expected: ${mismatch.expectedWidth.toFixed(2)} × ${mismatch.expectedHeight.toFixed(2)} pts (${mismatch.expectedOrientation})\n`;
                text += `  Reason: ${mismatch.mismatchReason}\n\n`;
            });
        }
        
        this.downloadFile(text, 'preflight-report.txt', 'text/plain');
    },

    // Download file helper
    downloadFile(content, filename, mimeType) {
        const blob = new Blob([content], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    },

    // Reset report viewer
    reset() {
        this.currentResult = null;
        
        const mismatchesBody = document.getElementById('mismatchesBody');
        if (mismatchesBody) mismatchesBody.innerHTML = '';
        
        const referenceInfo = document.getElementById('referenceInfo');
        if (referenceInfo) referenceInfo.innerHTML = '';
        
        const pageAnalysisBody = document.getElementById('pageAnalysisBody');
        if (pageAnalysisBody) pageAnalysisBody.innerHTML = '';
    },

    // Display page analysis table
    displayPageAnalyses(result) {
        const tbody = document.getElementById('pageAnalysisBody');
        const section = document.getElementById('pageAnalysisSection');
        
        tbody.innerHTML = '';
        
        if (!result.pageAnalyses || result.pageAnalyses.length === 0) {
            section.style.display = 'none';
            return;
        }
        
        section.style.display = 'block';
        
        result.pageAnalyses.forEach(analysis => {
            const row = document.createElement('tr');
            
            // Page number
            const pageCell = document.createElement('td');
            pageCell.setAttribute('data-label', 'Page');
            pageCell.textContent = analysis.pageNumber;
            pageCell.style.fontWeight = '600';
            row.appendChild(pageCell);
            
            // Dimensions
            const dimCell = document.createElement('td');
            dimCell.setAttribute('data-label', 'Dimensions');
            dimCell.textContent = `${analysis.widthInches}" × ${analysis.heightInches}"`;
            row.appendChild(dimCell);
            
            // Orientation
            const orientCell = document.createElement('td');
            orientCell.setAttribute('data-label', 'Orientation');
            orientCell.textContent = analysis.orientation === 'landscape' ? '🌄 Landscape' : '📄 Portrait';
            row.appendChild(orientCell);
            
            // Colorspaces
            const csCell = document.createElement('td');
            csCell.setAttribute('data-label', 'Colorspaces');
            csCell.innerHTML = analysis.colorspaces.map(cs => {
                const csClass = cs.toLowerCase().replace(/\s+/g, '-');
                return `<span class="colorscape-badge ${csClass}">${cs}</span>`;
            }).join('');
            row.appendChild(csCell);
            
            // Font Issues
            const fontCell = document.createElement('td');
            fontCell.setAttribute('data-label', 'Font Issues');
            if (analysis.fontIssues && analysis.fontIssues.length > 0) {
                fontCell.innerHTML = analysis.fontIssues.map(issue => {
                    const severityClass = issue.severity.toLowerCase();
                    const icon = issue.severity === 'CRITICAL' ? '❌' : '⚠️';
                    return `<div class="font-issue ${severityClass}">${icon} ${issue.fontName}</div>`;
                }).join('');
            } else {
                fontCell.innerHTML = '<span style="color: var(--success-color);">✓ No issues</span>';
            }
            row.appendChild(fontCell);
            
            // Resolution
            const resCell = document.createElement('td');
            resCell.setAttribute('data-label', 'Resolution');
            if (analysis.images && analysis.images.length > 0) {
                const lowResImages = analysis.images.filter(img => img.isLowRes);
                const minDPI = Math.min(...analysis.images.map(img => img.dpi));
                const resClass = minDPI < 150 ? 'poor' : minDPI < 300 ? 'medium' : 'good';
                resCell.innerHTML = `<span class="resolution-indicator ${resClass}">${minDPI.toFixed(0)} DPI</span>`;
                if (lowResImages.length > 0) {
                    resCell.innerHTML += `<br><span style="color: var(--fail-color); font-size: 11px;">${lowResImages.length} low-res</span>`;
                }
            } else {
                resCell.innerHTML = '<span style="color: var(--gray-600);">No images</span>';
            }
            row.appendChild(resCell);
            
            // Status
            const statusCell = document.createElement('td');
            statusCell.setAttribute('data-label', 'Status');
            const statusClass = analysis.status.toLowerCase();
            statusCell.innerHTML = `<span class="status-badge ${statusClass}">${analysis.status}</span>`;
            row.appendChild(statusCell);
            
            tbody.appendChild(row);
        });
    }
};
