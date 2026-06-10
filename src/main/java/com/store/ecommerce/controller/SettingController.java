package com.store.ecommerce.controller;

import com.store.ecommerce.dto.request.GeneralSettingsRequest;
import com.store.ecommerce.dto.request.MailTemplatesSettingsRequest;
import com.store.ecommerce.dto.request.PaymentSettingsRequest;
import com.store.ecommerce.dto.response.ApiErrorResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.response.SettingResponse;
import com.store.ecommerce.dto.wrapper.MessageResponseWrapper;
import com.store.ecommerce.dto.wrapper.SettingWrapper;
import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.SettingService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "APIs for managing application settings")
public class SettingController {
    private final SettingService settingService;

    private final AWSS3Service awsS3Service;

    @Operation(
            summary = "Get general settings",
            description = "Public API to retrieve general system settings"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved settings",
            content = @Content(schema = @Schema(implementation = SettingWrapper.class))
    )
    @GetMapping("/general")
    public ResponseEntity<ApiSuccessResponse<SettingResponse>> getAllGeneralSettings() {
        List<Setting> listSettings = settingService.getGeneralSettingBag().list();

        SettingResponse data = SettingResponse.builder()
                .listSettings(toMap(listSettings))
                .logoImageBaseURI(awsS3Service.getBaseURI() + "/site-logo/")
                .build();

        return ResponseEntity.ok(
                ApiSuccessResponse.<SettingResponse>builder()
                        .success(true)
                        .message("General settings retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get all settings",
            description = "Retrieve all settings (Admin only)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved settings",
                    content = @Content(schema = @Schema(implementation = SettingWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<SettingResponse>> getAllSettings() {
        List<Setting> listSettings = settingService.getAllSettings();

        SettingResponse data = SettingResponse.builder()
                .listSettings(toMap(listSettings))
                .logoImageBaseURI(awsS3Service.getBaseURI() + "/site-logo/")
                .build();

        return ResponseEntity.ok(
                ApiSuccessResponse.<SettingResponse>builder()
                        .success(true)
                        .message("All settings retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Update general settings",
            description = "Update general settings and optionally upload a logo file"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "General settings updated successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/general", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> saveGeneralSettings(
            @ModelAttribute GeneralSettingsRequest request,
            @RequestParam(name = "logoFile", required = false) MultipartFile logoFile
    ) throws IOException, IllegalAccessException {

        settingService.saveGeneralSettings(logoFile, request);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("General settings updated successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Update mail template settings",
            description = "Update email template configurations"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mail template settings updated successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/mail-templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> saveMailTemplatesSettings(
            @RequestBody MailTemplatesSettingsRequest request) throws IllegalAccessException {

        settingService.saveMailTemplatesSettings(request);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Mail template settings updated successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Update payment settings",
            description = "Update payment-related configurations"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment settings updated successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> savePaymentSettings(
            @RequestBody PaymentSettingsRequest request) throws IllegalAccessException {

        settingService.savePaymentSettings(request);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Payment settings updated successfully")
                        .data(null)
                        .build()
        );
    }

    // ===== HELPER =====
    private Map<String, String> toMap(List<Setting> settings) {
        return settings.stream()
                .collect(Collectors.toMap(Setting::getKey, Setting::getValue));
    }
}
