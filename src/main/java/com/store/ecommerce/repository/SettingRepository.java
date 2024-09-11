package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.enums.SettingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {
    List<Setting> findByCategory(SettingCategory settingCategory);
}
