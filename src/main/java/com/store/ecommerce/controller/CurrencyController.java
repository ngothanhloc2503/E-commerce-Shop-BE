package com.store.ecommerce.controller;

import com.store.ecommerce.entity.Currency;
import com.store.ecommerce.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/currencies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CurrencyController {
    private final CurrencyRepository currencyRepository;

    @GetMapping()
    public ResponseEntity<?> getAllCurrencies() {
        List<Currency> listCurrencies = currencyRepository.findAllByOrderByNameAsc();
        return ResponseEntity.ok(listCurrencies);
    }
}
