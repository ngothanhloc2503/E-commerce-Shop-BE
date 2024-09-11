package com.store.ecommerce.controller;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.UserRequestDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.security.jwt.JwtUtil;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    @Autowired
    private UserService userService;

    @Autowired
    private AWSS3Service awsS3Service;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("")
    public ResponseEntity<?> getAccountDetails(@RequestHeader("Authorization") String jwt) {
        try {
            return ResponseEntity.ok(userService.getUserByEmail(jwtUtil.extractSubject(jwt)));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveAccountDetails(
            @RequestHeader("Authorization") String jwt,
            @RequestPart(name = "accountDetails") UserRequestDTO userDTO,
            @RequestPart(name = "photo", required = false) MultipartFile photo) throws IOException {
        if (!isPhotoNullOrEmpty(photo)) {
            String fileName = StringUtils.cleanPath(photo.getOriginalFilename());
            userDTO.setPhoto(fileName);

            UserDTO savedUser = null;
            try {
                savedUser = userService.updateAccountDetails(jwtUtil.extractSubject(jwt), userDTO);
            } catch (NotFoundException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            }
            String uploadDir = "user-photos/" + savedUser.getId();

            awsS3Service.removeFolder(uploadDir + "/");
            awsS3Service.uploadFile(uploadDir, fileName, photo.getInputStream());

            return ResponseEntity.ok(savedUser);
        } else {
            if (StringUtils.isEmpty(userDTO.getPhoto())) userDTO.setPhoto(null);
            try {
                return ResponseEntity.ok(userService.updateAccountDetails(jwtUtil.extractSubject(jwt), userDTO));
            } catch (NotFoundException e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            }
        }
    }


    private boolean isPhotoNullOrEmpty(MultipartFile photo) {
        if (photo == null) {
            return true;
        }
        return photo.isEmpty();
    }
}
