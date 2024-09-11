package com.store.ecommerce.controller.customer;

import com.store.ecommerce.dto.response.SettingResponseDTO;
import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("SettingController")
@RequestMapping("/api/customer/settings")
public class SettingController {
    @Autowired
    private SettingService settingService;

    @Autowired
    private AWSS3Service awss3Service;

    @GetMapping("")
    public ResponseEntity<?> getAllGeneralSettings() {
        List<Setting> listSettings = settingService.getGeneralSettingBag().list();
        Map<String, String> mapSettings = new HashMap<String, String>();
        listSettings.forEach(s -> mapSettings.put(s.getKey(), s.getValue()));
        return ResponseEntity.ok(SettingResponseDTO.builder()
                .listSettings(mapSettings)
                .logoImageBaseURI(awss3Service.getBaseURI() + "/site-logo/").build());
    }
}
