package com.ccinfom.ui.common;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javax.swing.JTable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility for exporting JTable contents to a simple PDF.
 */
public final class PdfExporter {

    private PdfExporter() {
        // Utility class
    }

    public static void exportTable(JTable table, File destination, String title)
            throws DocumentException, IOException {
        if (table.getRowCount() == 0) {
            throw new IllegalStateException("No data to export.");
        }
        Document document = new Document(PageSize.A4.rotate());
        FileOutputStream out = new FileOutputStream(destination);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph(
                    "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            document.add(new Paragraph(" "));

            PdfPTable pdfTable = new PdfPTable(table.getColumnCount());
            pdfTable.setWidthPercentage(100f);

            for (int col = 0; col < table.getColumnCount(); col++) {
                PdfPCell header = new PdfPCell(new Phrase(table.getColumnName(col)));
                pdfTable.addCell(header);
            }

            for (int row = 0; row < table.getRowCount(); row++) {
                for (int col = 0; col < table.getColumnCount(); col++) {
                    Object value = table.getValueAt(row, col);
                    pdfTable.addCell(value == null ? "" : value.toString());
                }
            }
            document.add(pdfTable);
        } finally {
            document.close();
            out.close();
        }
    }
}
