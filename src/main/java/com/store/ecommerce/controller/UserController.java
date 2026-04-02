package com.store.ecommerce.controller;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.UserRequestDTO;
import com.store.ecommerce.dto.request.UserStatusRequest;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.UserService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import com.store.ecommerce.util.exporter.user.UserCsvExporter;
import com.store.ecommerce.util.exporter.user.UserExcelExporter;
import com.store.ecommerce.util.exporter.user.UserPdfExporter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AWSS3Service awsS3Service;

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

    @PostMapping(path = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveUser(@RequestPart(name = "user") UserRequestDTO userDTO,
                          @RequestPart(name = "filePhoto", required = false) MultipartFile photo) throws IOException {

        try {
            // Handle photo
            if (!isFileNullOrEmpty(photo)) {
                String originalName = photo.getOriginalFilename();
                String fileName = UUID.randomUUID() + "_" + originalName;
                userDTO.setPhoto(fileName);
            } else if (StringUtils.isEmpty(userDTO.getPhoto())) {
                userDTO.setPhoto(null);
            }

            // Update user
            UserDTO savedUser = userService.saveUser(userDTO);

            // Upload photo if exists
            if (!isFileNullOrEmpty(photo)) {
                String uploadDir = "user-photos/" + savedUser.getId();

                awsS3Service.removeFolder(uploadDir + "/");
                awsS3Service.uploadFile(uploadDir, userDTO.getPhoto(), photo.getInputStream());
            }

            return ResponseEntity.ok(savedUser);

        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        try {
            userService.delete(id);
            String userDir = "user-photos/" + id;
            awsS3Service.removeFolder(userDir + "/");
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/email-unique")
    public boolean checkUniqueEmail(@RequestParam("id") Long id, @RequestParam("email") String email) {
        return userService.isEmailUnique(id, email);
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<?> updateUserEnabledStatus(@PathVariable(name = "id") Long id,
                                                     @RequestBody @Valid UserStatusRequest request) {
        try {
            userService.updateUserEnabledStatus(id, request.getStatus());
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/export/csv")
    public ResponseEntity<?> exportToCsv(HttpServletResponse response) {
        List<UserDTO> listUsers = userService.getAllUsers();
        UserCsvExporter exporter = new UserCsvExporter();
        try {
            exporter.export(response, listUsers);
        } catch (IOException e) {
            return new ResponseEntity<>("Error while writing CSV file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<?> exportToExcel(HttpServletResponse response) {
        List<UserDTO> listUsers = userService.getAllUsers();
        try {
            UserExcelExporter exporter = new UserExcelExporter();
            exporter.export(response, listUsers);
        } catch (IOException e) {
            return new ResponseEntity<>("Error while writing Excel file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<?> exportToPdf(HttpServletResponse response) {
        List<UserDTO> listUsers = userService.getAllUsers();

        try {
            UserPdfExporter exporter = new UserPdfExporter();
            exporter.export(response, listUsers);
        } catch (IOException e) {
            return new ResponseEntity<>("Error while writing Pdf file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
