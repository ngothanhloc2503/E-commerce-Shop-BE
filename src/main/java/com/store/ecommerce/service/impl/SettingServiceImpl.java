package com.store.ecommerce.service.impl;

import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.entity.SettingBag;
import com.store.ecommerce.enums.SettingCategory;
import com.store.ecommerce.repository.CurrencyRepository;
import com.store.ecommerce.repository.SettingRepository;
import com.store.ecommerce.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {
    private final SettingRepository settingRepository;
    private final CurrencyRepository currencyRepository;

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
    public void saveAll(List<Setting> listSettings) {
        settingRepository.saveAll(listSettings);
    }

    @Override
    public List<Setting> getMailServerSettings() {
        return settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
    }

    @Override
    public List<Setting> getMailTemplatesSettings() {
        return settingRepository.findByCategory(SettingCategory.MAIL_TEMPLATES);
    }

    @Override
    public SettingBag getPaymentSettings() {
        return new SettingBag(settingRepository.findByCategory(SettingCategory.PAYMENT));
    }

    @Override
    public SettingBag getEmailSettings() {
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
        settings.addAll(settingRepository.findByCategory(SettingCategory.MAIL_TEMPLATES));

        return new SettingBag(settings);
    }
}
