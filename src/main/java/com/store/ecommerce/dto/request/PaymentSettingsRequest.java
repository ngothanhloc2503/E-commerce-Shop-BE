package com.store.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("PAYPAL_API_BASE_URL")
    String PAYPAL_API_BASE_URL;

    @JsonProperty("PAYPAL_API_CLIENT_ID")
    String PAYPAL_API_CLIENT_ID;

    @JsonProperty("PAYPAL_API_CLIENT_SECRET")
    String PAYPAL_API_CLIENT_SECRET;
}
