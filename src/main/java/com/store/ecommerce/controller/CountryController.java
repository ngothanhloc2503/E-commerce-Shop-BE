package com.store.ecommerce.controller;

import com.store.ecommerce.entity.Country;
import com.store.ecommerce.exception.NotFoundException;
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
        try {
            return ResponseEntity.ok(stateService.listStatesByCountryName(countryName));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveCountry(@RequestPart("country") Country country) {
        Country savedCountry = null;
        try {
            savedCountry = countryService.saveCountry(country);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
        return ResponseEntity.ok(savedCountry);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCountryByID(@PathVariable("id") Long id) {
        try {
            countryService.deleteByID(id);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
