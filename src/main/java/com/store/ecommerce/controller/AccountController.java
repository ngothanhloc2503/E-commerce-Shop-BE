package com.store.ecommerce.controller;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.UserRequestDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final UserService userService;
    private final AWSS3Service awsS3Service;

    @GetMapping("")
    public ResponseEntity<?> getAccountDetails(Authentication authentication) {
        try {
            return ResponseEntity.ok(userService.getUserByEmail(authentication.getName()));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("")
    public ResponseEntity<?> saveAccountDetails(
            Authentication authentication,
            @RequestPart(name = "accountDetails") UserRequestDTO userDTO,
            @RequestPart(name = "photo", required = false) MultipartFile photo) throws IOException {
        try {
            String email = authentication.getName();

            // Handle photo
            if (!isFileNullOrEmpty(photo)) {
                String originalName = photo.getOriginalFilename();
                String fileName = UUID.randomUUID() + "_" + originalName;
                userDTO.setPhoto(fileName);
            } else if (StringUtils.isEmpty(userDTO.getPhoto())) {
                userDTO.setPhoto(null);
            }

            // Update user
            UserDTO savedUser = userService.updateAccountDetails(email, userDTO);

            // Upload photo if exists
            if (!isFileNullOrEmpty(photo)) {
                String uploadDir = "user-photos/" + savedUser.getId();

                awsS3Service.removeFolder(uploadDir + "/");
                awsS3Service.uploadFile(uploadDir, userDTO.getPhoto(), photo.getInputStream());
            }

            return ResponseEntity.ok(savedUser);

        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
