package com.store.ecommerce.service.impl;

import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.ShippingRate;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.CountryRepository;
import com.store.ecommerce.repository.ShippingRateRepository;
import com.store.ecommerce.service.ShippingRateService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ShippingRateServiceImpl implements ShippingRateService {
    private final ShippingRateRepository shippingRateRepository;
    private final CountryRepository countryRepository;

    @Override
    public List<ShippingRate> getAllShippingRates() {
        return shippingRateRepository.findAll();
    }

    @Override
    public List<ShippingRate> getAllShippingRates(String keyword, String sortField, String sortDir) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();
        return shippingRateRepository.searchByKeyword(keyword, sort);
    }

    @Override
    public Page<ShippingRate> getShippingRatesByPage(PagingAndSortingHelper helper) {
        Sort sort = Sort.by(helper.getSortField());
        sort = helper.getSortDir().equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();
        Pageable pageable = PageRequest.of(helper.getPageNum() - 1, helper.getPageSize(), sort);

        // ✅ SỬA LỖI LOGIC: Gọi đúng hàm searchByKeyword khi có keyword
        if (helper.getKeyword() != null && !helper.getKeyword().isBlank()) {
            return shippingRateRepository.searchByKeyword(helper.getKeyword(), pageable);
        } else {
            return shippingRateRepository.findAll(pageable);
        }
    }

    @Override
    public ShippingRate getShippingRateById(Long id) throws NotFoundException {
        return shippingRateRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Could not find any shipping rate with ID: " + id));
    }

    @Override
    public ShippingRate saveShippingRate(ShippingRate shippingRate) throws NotFoundException {
        if (countryRepository.findByNameIgnoreCase(shippingRate.getCountry()).isEmpty()) {
            throw new NotFoundException("Could not find any country " + shippingRate.getCountry());
        }

        Long shippingRateId = shippingRate.getId();
        if (shippingRateId != null && shippingRateId > 0) {
            if (shippingRateRepository.findById(shippingRateId).isEmpty()) {
                throw new NotFoundException("Could not find any shipping rate with ID: " + shippingRateId);
            }
        }

        Optional<ShippingRate> existingRate = shippingRateRepository.findByCountryAndState(
                shippingRate.getCountry(), shippingRate.getState());

        existingRate.ifPresent(rate -> shippingRate.setId(rate.getId()));

        return shippingRateRepository.save(shippingRate);
    }

    @Override
    public void updateCodSupported(Long id, boolean supported) throws NotFoundException {
        if (!shippingRateRepository.existsById(id)) {
            throw new NotFoundException("Could not find any shipping rate with ID: " + id);
        }

        shippingRateRepository.updateCODSupported(id, supported);
    }

    @Override
    public void deleteShippingRate(Long id) throws NotFoundException {
        if (!shippingRateRepository.existsById(id)) {
            throw new NotFoundException("Could not find any shipping rate with ID: " + id);
        }

        shippingRateRepository.deleteById(id);
    }

    // For customer
    @Override
    public boolean isShippingSupported(Address address) {
        return shippingRateRepository.findByCountryAndState(address.getCountry(), address.getState()).isPresent();
    }

    @Override
    public ShippingRate getShippingRateByCountryAndState(String country, String state) {
        return shippingRateRepository.findByCountryAndState(country, state)
                .orElseThrow(() -> new NotFoundException(
                        "Could not find shipping rate for country: " + country + ", state: " + state
                ));
    }
}