package com.store.ecommerce.service.impl;

import com.store.ecommerce.entity.Currency;
import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.enums.SettingCategory;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.CurrencyRepository;
import com.store.ecommerce.repository.SettingRepository;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.SettingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingServiceImpl implements SettingService {
    private final SettingRepository settingRepository;

    private final CurrencyRepository currencyRepository;

    private final AWSS3Service awsS3Service;

    @Override
    public List<Setting> getAllSettings() {
        return settingRepository.findAll();
    }

    @Override
    public SettingBag getGeneralSettingBag() {
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.GENERAL);
        settings.addAll(settingRepository.findByCategory(SettingCategory.CURRENCY));

        return new SettingBag(settings);
    }

    @Override
    public void saveGeneralSettings(MultipartFile logoFile, HttpServletRequest request)
            throws IOException, NotFoundException {
        SettingBag settingBag = getGeneralSettingBag();

        saveSiteLogo(logoFile, settingBag);
        if (!saveCurrencySymbol(request, settingBag)) {
            throw new NotFoundException("Could not find any currency with ID: "
                    + request.getParameter("CURRENCY_ID"));
        }

        updateSettingsValueFromForm(request, settingBag.list());
    }

    @Override
    public void saveAll(List<Setting> listSettings) {
        settingRepository.saveAll(listSettings);
    }

    @Override
    public void saveMailTemplatesSettings(HttpServletRequest request) {
        List<Setting> mailTemplatesSettings = settingRepository
                .findByCategory(SettingCategory.MAIL_TEMPLATES);

        updateSettingsValueFromForm(request, mailTemplatesSettings);
    }

    @Override
    public SettingBag getPaymentSettings() {
        return new SettingBag(settingRepository.findByCategory(SettingCategory.PAYMENT));
    }

    @Override
    public void savePaymentSettings(HttpServletRequest request) {
        SettingBag paymentSettings = getPaymentSettings();

        updateSettingsValueFromForm(request, paymentSettings.list());
    }

    @Override
    public SettingBag getEmailSettings() {
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
        settings.addAll(settingRepository.findByCategory(SettingCategory.MAIL_TEMPLATES));

        return new SettingBag(settings);
    }

    // ====== HELPER ======
    private void updateSettingsValueFromForm(HttpServletRequest request, List<Setting> listSettings) {
        for (Setting setting : listSettings) {
            if (setting.getKey().equals("CURRENCY_SYMBOL")) continue;
            if (setting.getKey().equals("SITE_LOGO")) continue;
            String value = request.getParameter(setting.getKey());
            if (value != null) {
                setting.setValue(value);
            }
        }
        saveAll(listSettings);
    }

    private void saveSiteLogo(MultipartFile logoFile, SettingBag settingBag) throws IOException {
        if (isFileNullOrEmpty(logoFile)) return;

        String newFileName =  UUID.randomUUID() + "_" + StringUtils.cleanPath(logoFile.getOriginalFilename());
        String uploadDir = "site-logo";
        String oldFileName = settingBag.get("SITE_LOGO").getValue();

        try {
            // 1. Upload new file
            awsS3Service.uploadFile(
                    uploadDir,
                    newFileName,
                    logoFile.getInputStream(),
                    logoFile.getSize(),
                    logoFile.getContentType()
            );

            settingBag.update("SITE_LOGO", newFileName);

            // 2. Delete old file
            if (oldFileName != null && !oldFileName.isBlank()) {
                awsS3Service.deleteFile(uploadDir + "/" + oldFileName);
            }
        } catch (Exception e) {
            // rollback: Delete new file if error
            awsS3Service.deleteFile(uploadDir + "/" + newFileName);

            throw e;
        }
    }

    private boolean saveCurrencySymbol(HttpServletRequest request, SettingBag settingBag) {
        Long currencyId = Long.parseLong(request.getParameter("CURRENCY_ID"));
        Optional<Currency> findByIdResult = currencyRepository.findById(currencyId);

        if (findByIdResult.isEmpty()) {
            return false;
        } else {
            Currency currency = findByIdResult.get();
            settingBag.update("CURRENCY_SYMBOL", currency.getSymbol());
            return true;
        }
    }
}
