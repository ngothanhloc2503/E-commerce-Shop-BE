package com.store.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayPalCheckoutRequest {
    @NotBlank(message = "Order ID is required")
    private String orderId;
}
