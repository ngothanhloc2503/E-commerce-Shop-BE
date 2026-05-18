package com.store.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentSettingsRequest {
    @NotBlank(message = "PayPal API Base URL is required")
    @JsonProperty("PAYPAL_API_BASE_URL")
    String PAYPAL_API_BASE_URL;

    @NotBlank(message = "PayPal API Client ID is required")
    @JsonProperty("PAYPAL_API_CLIENT_ID")
    String PAYPAL_API_CLIENT_ID;

    @NotBlank(message = "PayPal API Client Secret is required")
    @JsonProperty("PAYPAL_API_CLIENT_SECRET")
    String PAYPAL_API_CLIENT_SECRET;
}
