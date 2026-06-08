package com.store.ecommerce.controller;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.UserRequest;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.wrapper.UserWrapper;
import com.store.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "APIs for managing user account information")
public class AccountController {
    private final UserService userService;

    @Operation(
            summary = "Get account details",
            description = "Retrieve the currently authenticated user's account information"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserWrapper.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<UserDTO>> getAccountDetails(Authentication authentication) {

        UserDTO user = userService.getUserByEmail(authentication.getName());

        return ResponseEntity.ok(
                ApiSuccessResponse.<UserDTO>builder()
                        .success(true)
                        .message("Account details retrieved successfully")
                        .data(user)
                        .build()
        );
    }

    @Operation(
            summary = "Update account details",
            description = "Update user profile information. Supports multipart request including user data and optional profile photo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account updated successfully",
                    content = @Content(schema = @Schema(implementation = UserWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<UserDTO>> saveAccountDetails(
            Authentication authentication,
            @RequestPart(name = "accountDetails") UserRequest userDTO,
            @RequestPart(name = "photo", required = false) MultipartFile photo) throws IOException {

        String email = authentication.getName();

        UserDTO savedUser = userService.updateAccountDetails(email, userDTO, photo);

        return ResponseEntity.ok(
                ApiSuccessResponse.<UserDTO>builder()
                        .success(true)
                        .message("Account details updated successfully")
                        .data(savedUser)
                        .build()
        );
    }
}
