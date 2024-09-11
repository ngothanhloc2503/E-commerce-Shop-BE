package com.store.ecommerce.service;

import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.ShippingRate;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ShippingRateService {
    List<ShippingRate> getAllShippingRates();

    List<ShippingRate> getAllShippingRates(String keyword, String sortField, String sortDir);

    Page<ShippingRate> getShippingRatesByPage(PagingAndSortingHelper helper);

    ShippingRate getShippingRateById(Long id) throws NotFoundException;

    ShippingRate saveShippingRate(ShippingRate shippingRate) throws NotFoundException;

    ShippingRate updateCodSupported(Long id, boolean supported) throws NotFoundException;

    void deleteShippingRate(Long id) throws NotFoundException;

    // For customer
    boolean isShippingSupported(Address address);

    ShippingRate getShippingRateByCountryAndState(String country, String state);
}
