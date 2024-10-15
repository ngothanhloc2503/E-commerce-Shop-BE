package com.store.ecommerce.util.exporter.user;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.util.exporter.AbstractExporter;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.List;

public class UserExcelExporter extends AbstractExporter {
    public void export(HttpServletResponse response, List<UserDTO> listUsers) throws IOException {
        super.setResponseHeader(response, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ".xlsx", "users_");

        Workbook workbook = new XSSFWorkbook();
        ServletOutputStream outputStream = response.getOutputStream();
        Sheet sheet = workbook.createSheet("Users");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"User ID", "E-mail", "First Name", "Last Name", "Roles", "Enabled"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderCellStyle(workbook));
        }

        int rowNum = 1;
        for (UserDTO user : listUsers) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getEmail());
            row.createCell(2).setCellValue(user.getFirstName());
            row.createCell(3).setCellValue(user.getLastName());
            row.createCell(4).setCellValue(user.getRoles().toString());
            row.createCell(5).setCellValue(user.isEnabled());
        }

        // Auto resize columns fit content
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
        workbook.close();
    }

    public CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}
