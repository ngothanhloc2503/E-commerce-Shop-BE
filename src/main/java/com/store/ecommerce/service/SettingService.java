package com.store.ecommerce.service;

import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.entity.SettingBag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface SettingService {
    List<Setting> getAllSettings();

    SettingBag getGeneralSettingBag();

    void saveGeneralSettings(MultipartFile logoFile, HttpServletRequest request) throws IOException;

    void saveAll(List<Setting> listSettings);

    void saveMailTemplatesSettings(HttpServletRequest request);

    SettingBag getPaymentSettings();

    void savePaymentSettings(HttpServletRequest request);

    SettingBag getEmailSettings();
}
