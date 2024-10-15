package com.store.ecommerce.util.exporter.user;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.util.exporter.AbstractExporter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class UserPdfExporter extends AbstractExporter {
    public void export(HttpServletResponse response, List<UserDTO> listUsers) throws IOException {
        super.setResponseHeader(response, "application/pdf", ".pdf", "users_");

        PdfWriter pdfWriter = new PdfWriter(response.getOutputStream());
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        Document document = new Document(pdfDocument, PageSize.A4.rotate());

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        Paragraph title = new Paragraph("List of Users")
                .setFont(font)
                .setFontSize(18)
                .setFontColor(new DeviceRgb(0, 0, 255))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setMarginBottom(10);
        document.add(title);

        Table table = new Table(new float[]{1.2f, 3.5f, 3.0f, 3.0f, 3.0f, 1.7f});
        table.setWidth(UnitValue.createPercentValue(100));

        writeTableHeader(table);

        writeTableData(table, listUsers);

        document.add(table);
        document.close();
    }

    private void writeTableHeader(Table table) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        Color backgroundColor = new DeviceRgb(0, 0, 255);  // Blue background
        Color fontColor = new DeviceRgb(255, 255, 255);    // White font

        String[] headers = {"User ID", "E-mail", "First Name", "Last Name", "Roles", "Enabled"};
        for (String header : headers) {
            Cell headerCell = new Cell().add(new Paragraph(header)
                            .setFont(font)
                            .setFontColor(fontColor))
                    .setBackgroundColor(backgroundColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(5);
            table.addHeaderCell(headerCell);
        }
    }

    private void writeTableData(Table table, List<UserDTO> listUsers) {
        for (UserDTO user : listUsers) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(user.getId())))
                    .setTextAlignment(TextAlignment.CENTER));

            table.addCell(user.getEmail() != null ? user.getEmail() : "");
            table.addCell(user.getFirstName() != null ? user.getFirstName() : "");
            table.addCell(user.getLastName() != null ? user.getLastName() : "");
            table.addCell(user.getRoles().toString());

            table.addCell(new Cell().add(new Paragraph(String.valueOf(user.isEnabled())))
                    .setTextAlignment(TextAlignment.CENTER));
        }
    }
}
