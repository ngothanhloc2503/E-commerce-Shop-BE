package com.store.ecommerce.controller;

import com.store.ecommerce.dto.response.SettingResponseDTO;
import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.SettingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {
    private final SettingService settingService;

    private final AWSS3Service awsS3Service;

    @GetMapping("/general-settings")
    public ResponseEntity<?> getAllGeneralSettings() {
        List<Setting> listSettings = settingService.getGeneralSettingBag().list();
        Map<String, String> mapSettings = new HashMap<String, String>();
        listSettings.forEach(s -> mapSettings.put(s.getKey(), s.getValue()));
        return ResponseEntity.ok(SettingResponseDTO.builder()
                .listSettings(mapSettings)
                .logoImageBaseURI(awsS3Service.getBaseURI() + "/site-logo/").build());
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllSettings() {
        List<Setting> listSettings = settingService.getAllSettings();
        Map<String, String> mapSettings = new HashMap<String, String>();
        listSettings.forEach(s -> mapSettings.put(s.getKey(), s.getValue()));

        return ResponseEntity.ok(SettingResponseDTO.builder()
                .listSettings(mapSettings)
                .logoImageBaseURI(awsS3Service.getBaseURI() + "/site-logo/").build());
    }

    @PutMapping("/general-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveGeneralSettings(
            @RequestParam(name = "logoFile", required = false) MultipartFile logoFile,
            HttpServletRequest request) throws IOException {

        settingService.saveGeneralSettings(logoFile, request);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/mail-templates-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveMailTemplatesSettings(HttpServletRequest request) {

        settingService.saveMailTemplatesSettings(request);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/payment-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> savePaymentSettings(HttpServletRequest request) {

        settingService.savePaymentSettings(request);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
