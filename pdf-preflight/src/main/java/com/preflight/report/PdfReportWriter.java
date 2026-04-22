package com.preflight.report;

import com.preflight.model.PdfPreflightResult;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for writing preflight reports in various formats.
 */
public interface PdfReportWriter {
    
    /**
     * Writes a preflight result to the specified output stream.
     * 
     * @param result The preflight result to write
     * @param out The output stream to write to
     * @throws IOException if writing fails
     */
    void write(PdfPreflightResult result, OutputStream out) throws IOException;
}
