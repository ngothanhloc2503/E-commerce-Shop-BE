package com.store.ecommerce.service;

import com.store.ecommerce.dto.request.CountryRequest;
import com.store.ecommerce.entity.Country;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;

import java.util.List;

public interface CountryService {
    List<Country> findAllByOrderByNameAsc();

    Country saveCountry(CountryRequest country) throws ConflictException;

    void deleteByID(Long id) throws NotFoundException;
}
