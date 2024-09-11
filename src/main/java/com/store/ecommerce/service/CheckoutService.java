package com.store.ecommerce.service;

import com.store.ecommerce.dto.CheckoutInfo;
import com.store.ecommerce.exception.NotFoundException;

public interface CheckoutService {
    CheckoutInfo getCheckoutInformation(String email) throws NotFoundException;
}
