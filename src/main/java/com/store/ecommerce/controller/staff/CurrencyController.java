package com.store.ecommerce.controller.staff;

import com.store.ecommerce.dto.response.SettingResponseDTO;
import com.store.ecommerce.entity.Currency;
import com.store.ecommerce.entity.Setting;
import com.store.ecommerce.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/currencies")
@PreAuthorize("hasRole('ADMIN')")
public class CurrencyController {

    @Autowired
    private CurrencyRepository currencyRepository;

    @GetMapping()
    public ResponseEntity<?> getAllCurrencies() {
        List<Currency> listCurrencies = currencyRepository.findAllByOrderByNameAsc();
        return ResponseEntity.ok(listCurrencies);
    }
}
