package com.store.ecommerce.controller;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.UserRequestDTO;
import com.store.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final UserService userService;

    @GetMapping("")
    public ResponseEntity<?> getAccountDetails(Authentication authentication) {

        return ResponseEntity.ok(userService.getUserByEmail(authentication.getName()));
    }

    @PostMapping("")
    public ResponseEntity<?> saveAccountDetails(
            Authentication authentication,
            @RequestPart(name = "accountDetails") UserRequestDTO userDTO,
            @RequestPart(name = "photo", required = false) MultipartFile photo) throws IOException {

        String email = authentication.getName();

        UserDTO savedUser = userService.updateAccountDetails(email, userDTO, photo);

        return ResponseEntity.ok(savedUser);
    }
}
