package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.enums.SettingCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("SettingRepository Integration Tests")
class SettingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SettingRepository settingRepository;

    @BeforeEach
    void setUp() {
        // MAIL_SERVER settings
        persistSetting("MAIL_HOST", "smtp.example.com", SettingCategory.MAIL_SERVER);
        persistSetting("MAIL_PORT", "587", SettingCategory.MAIL_SERVER);
        persistSetting("MAIL_USERNAME", "admin@example.com", SettingCategory.MAIL_SERVER);

        // GENERAL settings
        persistSetting("SITE_NAME", "MyStore", SettingCategory.GENERAL);
        persistSetting("SITE_LOGO", "logo.png", SettingCategory.GENERAL);

        // CURRENCY settings
        persistSetting("CURRENCY_ID", "1", SettingCategory.CURRENCY);
        persistSetting("CURRENCY_SYMBOL", "$", SettingCategory.CURRENCY);
        persistSetting("CURRENCY_SYMBOL_POSITION", "before", SettingCategory.CURRENCY);

        // PAYMENT settings
        persistSetting("PAYMENT_METHOD", "VNPAY", SettingCategory.PAYMENT);
    }

    // ======================== FIND BY CATEGORY ========================

    @Test
    @DisplayName("Should find settings by MAIL_SERVER category")
    void findByCategory_MailServer() {
        // Act
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);

        // Assert
        assertThat(settings).hasSize(3);
        assertThat(settings).extracting(Setting::getKey)
                .containsExactlyInAnyOrder("MAIL_HOST", "MAIL_PORT", "MAIL_USERNAME");
    }

    @Test
    @DisplayName("Should find settings by GENERAL category")
    void findByCategory_General() {
        // Act
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.GENERAL);

        // Assert
        assertThat(settings).hasSize(2);
        assertThat(settings).extracting(Setting::getKey)
                .containsExactlyInAnyOrder("SITE_NAME", "SITE_LOGO");
    }

    @Test
    @DisplayName("Should find settings by CURRENCY category")
    void findByCategory_Currency() {
        // Act
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.CURRENCY);

        // Assert
        assertThat(settings).hasSize(3);
        assertThat(settings).extracting(Setting::getKey)
                .containsExactlyInAnyOrder("CURRENCY_ID", "CURRENCY_SYMBOL", "CURRENCY_SYMBOL_POSITION");
    }

    @Test
    @DisplayName("Should find settings by PAYMENT category")
    void findByCategory_Payment() {
        // Act
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.PAYMENT);

        // Assert
        assertThat(settings).hasSize(1);
        assertThat(settings.get(0).getKey()).isEqualTo("PAYMENT_METHOD");
    }

    @Test
    @DisplayName("Should not mix settings from different categories")
    void findByCategory_NoCrossCategory() {
        // Act
        List<Setting> mailSettings = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
        List<Setting> generalSettings = settingRepository.findByCategory(SettingCategory.GENERAL);

        // Assert — no overlap
        assertThat(mailSettings).noneMatch(s -> s.getCategory() != SettingCategory.MAIL_SERVER);
        assertThat(generalSettings).noneMatch(s -> s.getCategory() != SettingCategory.GENERAL);
    }

    @Test
    @DisplayName("Should return empty list when category has no settings")
    void findByCategory_EmptyCategory() {
        // Act — assume there's a category with no settings
        // Try all categories; if some enum values have no data, they return empty
        // Here we delete all and test
        settingRepository.deleteAll();

        List<Setting> settings = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);

        // Assert
        assertThat(settings).isEmpty();
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve setting")
    void save_AndFindById() {
        // Arrange
        Setting setting = new Setting();
        setting.setKey("NEW_SETTING");
        setting.setValue("new_value");
        setting.setCategory(SettingCategory.GENERAL);

        // Act
        Setting saved = settingRepository.save(setting);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getKey()).isEqualTo("NEW_SETTING");
        assertThat(saved.getValue()).isEqualTo("new_value");
    }

    @Test
    @DisplayName("Should update existing setting value")
    void save_UpdateExisting() {
        // Arrange
        List<Setting> mailSettings = settingRepository.findByCategory(SettingCategory.MAIL_SERVER);
        Setting mailHost = mailSettings.stream()
                .filter(s -> s.getKey().equals("MAIL_HOST"))
                .findFirst()
                .orElseThrow();

        // Act
        mailHost.setValue("smtp.newhost.com");
        settingRepository.save(mailHost);

        // Assert
        entityManager.flush();
        entityManager.clear();

        Setting updated = entityManager.find(Setting.class, mailHost.getId());
        assertThat(updated.getValue()).isEqualTo("smtp.newhost.com");
    }

    @Test
    @DisplayName("Should delete setting by id")
    void deleteById_Success() {
        // Arrange
        List<Setting> settings = settingRepository.findByCategory(SettingCategory.PAYMENT);
        assertThat(settings).hasSize(1);
        Long settingId = settings.get(0).getId();

        // Act
        settingRepository.deleteById(settingId);

        // Assert
        assertThat(entityManager.find(Setting.class, settingId)).isNull();
    }

    @Test
    @DisplayName("Should count settings correctly")
    void count_Success() {
        // 3 MAIL_SERVER + 2 GENERAL + 3 CURRENCY + 1 PAYMENT = 9
        assertThat(settingRepository.count()).isEqualTo(9);
    }

    @Test
    @DisplayName("Should find all settings")
    void findAll_Success() {
        List<Setting> settings = settingRepository.findAll();
        assertThat(settings).hasSize(9);
    }

    // ======================== HELPER METHODS ========================

    private Setting persistSetting(String key, String value, SettingCategory category) {
        Setting setting = new Setting();
        setting.setKey(key);
        setting.setValue(value);
        setting.setCategory(category);
        return entityManager.persistAndFlush(setting);
    }
}