package com.template.service;
import com.template.dto.SamplePdfData;
import java.io.ByteArrayOutputStream;

public interface PdfGeneratorService {
    /**
     * Generates a PDF document based on the provided data
     * @param data The data to be included in the PDF
     * @return ByteArrayOutputStream containing the generated PDF
     */
    ByteArrayOutputStream generatePdf(Object data);
    
    /**
     * Generates a PDF document with custom template
     * @param data The data to be included in the PDF
     * @param templateName The name of the template to use
     * @return ByteArrayOutputStream containing the generated PDF
     */
    ByteArrayOutputStream generatePdfWithTemplate(SamplePdfData data, String templateName);
} 