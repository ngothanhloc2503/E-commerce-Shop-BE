package com.store.ecommerce.service.impl;

import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.ShippingRate;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.CountryRepository;
import com.store.ecommerce.repository.ShippingRateRepository;
import com.store.ecommerce.service.ShippingRateService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ShippingRateServiceImpl implements ShippingRateService {
    @Autowired
    private ShippingRateRepository shippingRateRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Override
    public List<ShippingRate> getAllShippingRates() {
        return shippingRateRepository.findAll();
    }

    @Override
    public List<ShippingRate> getAllShippingRates(String keyword, String sortField, String sortDir) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();
        return shippingRateRepository.findAll(keyword, sort);
    }

    @Override
    public Page<ShippingRate> getShippingRatesByPage(PagingAndSortingHelper helper) {
        return (Page<ShippingRate>) helper.getPageEntities(shippingRateRepository);
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
            // Is Updating
            if (shippingRateRepository.findById(shippingRateId).isEmpty()) {
                throw new NotFoundException("Could not find any shipping rate with ID: " + shippingRateId);
            }
        }

        // Resolve create with country and state has been existing.
        ShippingRate byCountryAndState = shippingRateRepository.findByCountryAndState(shippingRate.getCountry(), shippingRate.getState());
        if (byCountryAndState != null) {
            shippingRate.setId(byCountryAndState.getId());
        }

        return shippingRateRepository.save(shippingRate);
    }

    @Override
    public ShippingRate updateCodSupported(Long id, boolean supported) throws NotFoundException {
        Optional<ShippingRate> shippingRate = shippingRateRepository.findById(id);

        if (shippingRate.isEmpty()) {
            throw new NotFoundException("Could not find any shipping rate with ID: " + id);
        }
        ShippingRate shippingRateInDB = shippingRate.get();
        shippingRateInDB.setCodSupported(supported);

        return shippingRateRepository.save(shippingRateInDB);
    }

    @Override
    public void deleteShippingRate(Long id) throws NotFoundException {
        if (shippingRateRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Could not find any shipping rate with ID: " + id);
        }

        shippingRateRepository.deleteById(id);
    }

    // For customer
    @Override
    public boolean isShippingSupported(Address address) {
        return shippingRateRepository.findByCountryAndState(address.getCountry(), address.getState()) != null;
    }

    @Override
    public ShippingRate getShippingRateByCountryAndState(String country, String state) {
        return shippingRateRepository.findByCountryAndState(country, state);
    }
}
