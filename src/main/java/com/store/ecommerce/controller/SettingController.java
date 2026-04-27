package com.store.ecommerce.controller;

import com.store.ecommerce.dto.request.GeneralSettingsRequest;
import com.store.ecommerce.dto.request.MailTemplatesSettingsRequest;
import com.store.ecommerce.dto.request.PaymentSettingsRequest;
import com.store.ecommerce.dto.response.SettingResponseDTO;
import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
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
    @ApiResponse(responseCode = "200", description = "Successfully retrieved settings")
    @GetMapping("/general")
    public ResponseEntity<SettingResponseDTO> getAllGeneralSettings() {
        List<Setting> listSettings = settingService.getGeneralSettingBag().list();

        return ResponseEntity.ok(SettingResponseDTO.builder()
                .listSettings(toMap(listSettings))
                .logoImageBaseURI(awsS3Service.getBaseURI() + "/site-logo/").build());
    }

    @Operation(
            summary = "Get all settings",
            description = "Retrieve all settings (Admin only)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved settings"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SettingResponseDTO> getAllSettings() {
        List<Setting> listSettings = settingService.getAllSettings();

        return ResponseEntity.ok(SettingResponseDTO.builder()
                .listSettings(toMap(listSettings))
                .logoImageBaseURI(awsS3Service.getBaseURI() + "/site-logo/").build());
    }

    @Operation(
            summary = "Update general settings",
            description = "Update general settings and optionally upload a logo file"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "General settings updated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping(value = "/general")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveGeneralSettings(
            @ModelAttribute GeneralSettingsRequest request,
            @RequestParam(name = "logoFile", required = false) MultipartFile logoFile
    ) throws IOException, IllegalAccessException {

        settingService.saveGeneralSettings(logoFile, request);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update mail template settings",
            description = "Update email template configurations"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mail template settings updated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/mail-templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveMailTemplatesSettings(@RequestBody MailTemplatesSettingsRequest request) throws IllegalAccessException {

        settingService.saveMailTemplatesSettings(request);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Update payment settings",
            description = "Update payment-related configurations"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Payment settings updated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/payment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> savePaymentSettings(@RequestBody PaymentSettingsRequest request) throws IllegalAccessException {
        System.out.println(request);

        settingService.savePaymentSettings(request);

        return ResponseEntity.noContent().build();
    }

    // Helper
    private Map<String, String> toMap(List<Setting> settings) {
        return settings.stream()
                .collect(Collectors.toMap(Setting::getKey, Setting::getValue));
    }
}
