package com.store.ecommerce.controller.staff;

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
import com.store.ecommerce.dto.request.UserRequestDTO;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.UserMapper;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.UserService;
import com.store.ecommerce.util.ExporterUtil;
import com.store.ecommerce.util.PagingAndSortingHelper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/staff/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AWSS3Service awsS3Service;

    @GetMapping("")
    public ResponseEntity<?> getUsersByPage(PagingAndSortingHelper helper) {
        if (helper.getPageSize() < 1) {
            List<UserDTO> users = userService.getAllUsers(helper.getKeyword(), helper.getSortField(), helper.getSortDir());
            return ResponseEntity.ok(PagedResponseDTO.builder()
                    .content(users)
                    .totalPages(1)
                    .totalItems((long) users.size()).build());
        }

        Page<UserDTO> page = userService.getUsersByPage(helper);
        return ResponseEntity.ok(PagedResponseDTO.builder()
                .content(page.getContent())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements()).build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Long id) {
        try {
            UserDTO savedUser = userService.getUserById(id);
            return ResponseEntity.ok(savedUser);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(path = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveUser(@RequestPart(name = "user") UserRequestDTO userDTO,
                          @RequestPart(name = "filePhoto", required = false) MultipartFile photo) throws IOException {
        if (!isFileNullAndEmpty(photo)) {
            String fileName = StringUtils.cleanPath(photo.getOriginalFilename());
            userDTO.setPhoto(fileName);

            UserDTO savedUser = null;
            try {
                savedUser = userService.saveUser(userDTO);
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
            }
            String uploadDir = "user-photos/" + savedUser.getId();

            awsS3Service.removeFolder(uploadDir + "/");
            awsS3Service.uploadFile(uploadDir, fileName, photo.getInputStream());

            return ResponseEntity.ok(savedUser);
        } else {
            if (StringUtils.isEmpty(userDTO.getPhoto())) userDTO.setPhoto(null);
            try {
                return ResponseEntity.ok(userService.saveUser(userDTO));
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
            }
        }
    }

    private boolean isFileNullAndEmpty(MultipartFile photo) {
        if (photo == null) {
            return true;
        }
        return photo.isEmpty();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        try {
            userService.delete(id);
            String userDir = "user-photos/" + id;
            awsS3Service.removeFolder(userDir + "/");
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-email")
    public boolean checkUniqueEmail(@RequestParam("id") Long id, @RequestParam("email") String email) {
        return userService.isEmailUnique(id, email);
    }

    @GetMapping("/{id}/enabled/{status}")
    public ResponseEntity<?> updateUserEnabledStatus(@PathVariable(name = "id") Long id,
                                          @PathVariable(name = "status") boolean enabled) {
        try {
            userService.updateUserEnabledStatus(id, enabled);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export/csv")
    public ResponseEntity<?> exportToCsv(HttpServletResponse response) throws IOException {
        List<UserDTO> listUsers = userService.getAllUsers();
        ExporterUtil.setResponseHeader(response, "text/csv", ".csv", "users_");

        String[] csvHeader = {"User ID", "E-mail", "First Name", "Last Name", "Roles", "Enabled"};
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(csvHeader)
                .setSkipHeaderRecord(false)
                .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(response.getWriter(), csvFormat)) {
            for (UserDTO user : listUsers) {
                csvPrinter.printRecord(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRoles(),
                    user.isEnabled()
                );
            }
        } catch (IOException e) {
            return new ResponseEntity<>("Error while writing CSV file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<?> exportToPdf(HttpServletResponse response) {
        List<UserDTO> listUsers = userService.getAllUsers();
        ExporterUtil.setResponseHeader(response, "application/pdf", ".pdf", "users_");

        try {
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
        } catch (IOException e) {
            return new ResponseEntity<>("Error while writing Pdf file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
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
