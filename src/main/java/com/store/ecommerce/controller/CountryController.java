package com.store.ecommerce.controller;

import com.store.ecommerce.entity.Country;
import com.store.ecommerce.service.CountryService;
import com.store.ecommerce.service.StateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
public class CountryController {
    private final CountryService countryService;
    private final StateService stateService;

    @GetMapping("")
    public ResponseEntity<List<Country>> getAllCountries() {
        return ResponseEntity.ok(countryService.findAllByOrderByNameAsc());
    }

    @GetMapping("/{countryName}/states")
    public ResponseEntity<?> getListStatesByCountryName(@PathVariable("countryName") String countryName) {

        return ResponseEntity.ok(stateService.listStatesByCountryName(countryName));
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveCountry(@RequestPart("country") Country country) {
        Country savedCountry = countryService.saveCountry(country);

        return ResponseEntity.ok(savedCountry);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCountryByID(@PathVariable("id") Long id) {

        countryService.deleteByID(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
