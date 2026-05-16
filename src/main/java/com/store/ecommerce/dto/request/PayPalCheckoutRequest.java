package com.store.ecommerce.dto.request;

import lombok.Data;

@Data
public class PayPalCheckoutRequest {
    private String orderId;
}
