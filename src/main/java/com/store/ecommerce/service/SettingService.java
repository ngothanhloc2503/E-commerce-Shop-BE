package com.store.ecommerce.service;

import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.entity.SettingBag;

import java.util.List;

public interface SettingService {
    List<Setting> getAllSettings();

    SettingBag getGeneralSettingBag();

    void saveAll(List<Setting> listSettings);

    List<Setting> getMailServerSettings();

    List<Setting> getMailTemplatesSettings();

    SettingBag getPaymentSettings();

    SettingBag getEmailSettings();
}
