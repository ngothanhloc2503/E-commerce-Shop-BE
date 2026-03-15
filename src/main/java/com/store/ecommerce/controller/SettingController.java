package com.store.ecommerce.controller;

import com.store.ecommerce.dto.response.SettingResponseDTO;
import com.store.ecommerce.entity.Currency;
import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.repository.CurrencyRepository;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.SettingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {
    private final SettingService settingService;

    private final CurrencyRepository currencyRepository;

    private final AWSS3Service awsS3Service;

    @GetMapping("")
    public ResponseEntity<?> getAllGeneralSettings() {
        List<Setting> listSettings = settingService.getGeneralSettingBag().list();
        Map<String, String> mapSettings = new HashMap<String, String>();
        listSettings.forEach(s -> mapSettings.put(s.getKey(), s.getValue()));
        return ResponseEntity.ok(SettingResponseDTO.builder()
                .listSettings(mapSettings)
                .logoImageBaseURI(awsS3Service.getBaseURI() + "/site-logo/").build());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllSettings() {
        List<Setting> listSettings = settingService.getAllSettings();
        Map<String, String> mapSettings = new HashMap<String, String>();
        listSettings.forEach(s -> mapSettings.put(s.getKey(), s.getValue()));

        return ResponseEntity.ok(SettingResponseDTO.builder()
                .listSettings(mapSettings)
                .logoImageBaseURI(awsS3Service.getBaseURI() + "/site-logo/").build());
    }

    @PostMapping("/save-general-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveGeneralSettings(
            @RequestParam(name = "logoFile", required = false) MultipartFile logoFile,
            HttpServletRequest request) throws IOException {
        SettingBag settingBag = settingService.getGeneralSettingBag();

        saveSiteLogo(logoFile, settingBag);
        if (!saveCurrencySymbol(request, settingBag)) {
            return new ResponseEntity<>("Could not find any currency with ID: "
                    + request.getParameter("CURRENCY_ID"), HttpStatus.NOT_FOUND);
        }

        updateSettingsValueFromForm(request, settingBag.list());

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PostMapping("/save-mail-server-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveMailServerSettings(HttpServletRequest request){
        List<Setting> mailServerSettings = settingService.getMailServerSettings();

        updateSettingsValueFromForm(request, mailServerSettings);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PostMapping("/save-mail-templates-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveMailTemplatesSettings(HttpServletRequest request){
        List<Setting> mailTemplatesSettings = settingService.getMailTemplatesSettings();

        updateSettingsValueFromForm(request, mailTemplatesSettings);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PostMapping("/save-payment-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> savePaymentSettings(HttpServletRequest request){
        SettingBag paymentSettings = settingService.getPaymentSettings();

        updateSettingsValueFromForm(request, paymentSettings.list());

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    private void saveSiteLogo(MultipartFile logoFile, SettingBag settingBag) throws IOException {
        if (!isLogoNullOrEmpty(logoFile)) {
            String fileName = StringUtils.cleanPath(logoFile.getOriginalFilename());
            settingBag.update("SITE_LOGO", fileName);

            String uploadDir = "site-logo";
            awsS3Service.removeFolder(uploadDir + "/");
            awsS3Service.uploadFile(uploadDir, fileName, logoFile.getInputStream());
        }
    }

    private boolean saveCurrencySymbol(HttpServletRequest request, SettingBag settingBag) {
        Long currencyId = Long.parseLong(request.getParameter("CURRENCY_ID"));
        Optional<Currency> findByIdResult = currencyRepository.findById(currencyId);

        if (findByIdResult.isEmpty()) {
            return false;
        }
        else {
            Currency currency = findByIdResult.get();
            settingBag.update("CURRENCY_SYMBOL", currency.getSymbol());
            return true;
        }
    }

    private void updateSettingsValueFromForm(HttpServletRequest request, List<Setting> listSettings) {
        for (Setting setting : listSettings) {
            if (setting.getKey().equals("CURRENCY_SYMBOL")) continue;
            String value = request.getParameter(setting.getKey());
            if (value != null) {
                setting.setValue(value);
            }
        }
        settingService.saveAll(listSettings);
    }

    private boolean isLogoNullOrEmpty(MultipartFile logo) {
        if (logo == null) {
            return true;
        }
        return logo.isEmpty();
    }
}
