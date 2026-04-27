package com.store.ecommerce.service;

import com.store.ecommerce.dto.request.GeneralSettingsRequest;
import com.store.ecommerce.dto.request.MailTemplatesSettingsRequest;
import com.store.ecommerce.dto.request.PaymentSettingsRequest;
import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.entity.SettingBag;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface SettingService {
    List<Setting> getAllSettings();

    SettingBag getGeneralSettingBag();

    void saveGeneralSettings(MultipartFile logoFile, GeneralSettingsRequest request) throws IOException, IllegalAccessException;

    void saveAll(List<Setting> listSettings);

    void saveMailTemplatesSettings(MailTemplatesSettingsRequest request) throws IllegalAccessException;

    SettingBag getPaymentSettings();

    void savePaymentSettings(PaymentSettingsRequest request) throws IllegalAccessException;

    SettingBag getEmailSettings();
}
