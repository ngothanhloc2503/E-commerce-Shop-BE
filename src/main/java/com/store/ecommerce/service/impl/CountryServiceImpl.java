package com.store.ecommerce.service.impl;

import com.store.ecommerce.entity.Country;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.CountryRepository;
import com.store.ecommerce.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CountryServiceImpl implements CountryService {
    @Autowired
    private CountryRepository countryRepository;

    @Override
    public List<Country> findAllByOrderByNameAsc() {
        return countryRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Country saveCountry(Country country) throws Exception {
        if (!isNameUnique(country.getId(), country.getName())) {
            throw new Exception("Name is existing!");
        }
        if (!isCodeUnique(country.getId(), country.getCode())) {
            throw new Exception("Code is existing!");
        }

        return countryRepository.save(country);
    }

    @Override
    public void deleteByID(Long id) throws NotFoundException {
        if (countryRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Could not find any country with ID: " + id);
        }

        countryRepository.deleteById(id);
    }

    public boolean isNameUnique(Long id, String name) {
        Optional<Country> country = countryRepository.findByNameIgnoreCase(name);
        return country.isEmpty() || Objects.equals(country.get().getId(), id);
    }

    public boolean isCodeUnique(Long id, String code) {
        Optional<Country> country = countryRepository.findByCode(code);
        return country.isEmpty() || Objects.equals(country.get().getId(), id);
    }
}
