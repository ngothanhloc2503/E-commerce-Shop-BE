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
public class GeneralSettingsRequest {
    @JsonProperty("CURRENCY_ID")
    Long CURRENCY_ID;

    @JsonProperty("CURRENCY_SYMBOL")
    String CURRENCY_SYMBOL;

    @JsonProperty("CURRENCY_SYMBOL_POSITION")
    String CURRENCY_SYMBOL_POSITION;

    @JsonProperty("DECIMAL_DIGITS")
    Integer DECIMAL_DIGITS;

    @JsonProperty("DECIMAL_POINT_TYPE")
    String DECIMAL_POINT_TYPE;

    @JsonProperty("THOUSANDS_POINT_TYPE")
    String THOUSANDS_POINT_TYPE;

    @JsonProperty("SITE_LOGO")
    String SITE_LOGO;
}
