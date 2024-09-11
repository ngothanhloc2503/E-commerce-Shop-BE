package com.store.ecommerce.service;

import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;

public interface PaypalService {
    boolean validateOrder(String orderId) throws BadRequestException;
}
