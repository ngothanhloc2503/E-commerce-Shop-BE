package com.store.ecommerce.service.impl;

import com.store.ecommerce.dto.request.GeneralSettingsRequest;
import com.store.ecommerce.dto.request.MailTemplatesSettingsRequest;
import com.store.ecommerce.dto.request.PaymentSettingsRequest;
import com.store.ecommerce.entity.Currency;
import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.enums.SettingCategory;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.CurrencyRepository;
import com.store.ecommerce.repository.SettingRepository;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.SettingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
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
        List<Setting> settings = new ArrayList<>(settingRepository.findByCategory(SettingCategory.GENERAL));
        settings.addAll(settingRepository.findByCategory(SettingCategory.CURRENCY));

        return new SettingBag(settings);
    }

    @Override
    public void saveGeneralSettings(MultipartFile logoFile, GeneralSettingsRequest request)
            throws IOException, NotFoundException, IllegalAccessException {
        SettingBag settingBag = getGeneralSettingBag();

        saveSiteLogo(logoFile, settingBag);
        if (!saveCurrencySymbol(request.getCURRENCY_ID(), request.getCURRENCY_SYMBOL(), settingBag)) {
            throw new NotFoundException("Could not find any currency with ID: "
                    + request.getCURRENCY_ID());
        }

        updateSettings(request, settingBag.list());
    }

    @Override
    public void saveAll(List<Setting> listSettings) {
        settingRepository.saveAll(listSettings);
    }

    @Override
    public void saveMailTemplatesSettings(MailTemplatesSettingsRequest request) throws IllegalAccessException {
        List<Setting> mailTemplatesSettings = settingRepository
                .findByCategory(SettingCategory.MAIL_TEMPLATES);

        updateSettings(request, mailTemplatesSettings);
    }

    @Override
    public SettingBag getPaymentSettings() {
        return new SettingBag(settingRepository.findByCategory(SettingCategory.PAYMENT));
    }

    @Override
    public void savePaymentSettings(PaymentSettingsRequest request) throws IllegalAccessException {
        SettingBag paymentSettings = getPaymentSettings();

        updateSettings(request, paymentSettings.list());
    }

    @Override
    public SettingBag getEmailSettings() {
        List<Setting> settings = new ArrayList<>(settingRepository.findByCategory(SettingCategory.MAIL_SERVER));
        settings.addAll(settingRepository.findByCategory(SettingCategory.MAIL_TEMPLATES));

        return new SettingBag(settings);
    }

    // ====== HELPER ======
    private <T> void updateSettings(T request, List<Setting> listSettings) throws IllegalAccessException {
        Class<?> clazz = request.getClass();

        for (Setting setting : listSettings) {
            if (setting.getKey().equals("CURRENCY_SYMBOL")) continue;
            if (setting.getKey().equals("SITE_LOGO")) continue;

            try {
                Field field = clazz.getDeclaredField(setting.getKey());
                field.setAccessible(true);

                Object value = field.get(request);
                if (value != null) {
                    setting.setValue(String.valueOf(value));
                }
            } catch (NoSuchFieldException e) {
                log.warn("No field found for key: {}", setting.getKey());
            }
        }

        saveAll(listSettings);
    }

    private void saveSiteLogo(MultipartFile logoFile, SettingBag settingBag) throws IOException {
        if (isFileNullOrEmpty(logoFile)) return;

        String newFileName =  UUID.randomUUID() + "_" + StringUtils.cleanPath(logoFile.getOriginalFilename());
        String uploadDir = "site-logo";

        Setting siteLogoSetting = settingBag.get("SITE_LOGO");
        String oldFileName = (siteLogoSetting != null) ? siteLogoSetting.getValue() : null;

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
            try {
                awsS3Service.deleteFile(uploadDir + "/" + newFileName);
            } catch (Exception ex) {
                log.warn("Failed to clean up partially uploaded file: {}", newFileName, ex);
            }

            throw new IOException("Failed to upload site logo to S3", e);
        }
    }

    private boolean saveCurrencySymbol(Long currencyId, String currencySymbol, SettingBag settingBag) {
        Optional<Currency> findByIdResult = currencyRepository.findById(currencyId);

        if (findByIdResult.isEmpty()) {
            return false;
        } else {
            Currency currency = findByIdResult.get();
            if (!Objects.equals(currencySymbol, currency.getSymbol())) return false;

            settingBag.update("CURRENCY_SYMBOL", currency.getSymbol());

            return true;
        }
    }
}
