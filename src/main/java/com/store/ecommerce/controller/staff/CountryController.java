package com.store.ecommerce.controller.staff;

import com.store.ecommerce.entity.Country;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("ManageCountryController")
@RequestMapping("/api/staff/countries")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CountryController {
    private final CountryService countryService;

    @GetMapping("")
    public ResponseEntity<List<Country>> getAllCountries() {
        return ResponseEntity.ok(countryService.findAllByOrderByNameAsc());
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveCountry(@RequestPart("country") Country country) {
        Country savedCountry = null;
        try {
            savedCountry = countryService.saveCountry(country);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
        return ResponseEntity.ok(savedCountry);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCountryByID(@PathVariable("id") Long id) {
        try {
            countryService.deleteByID(id);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
