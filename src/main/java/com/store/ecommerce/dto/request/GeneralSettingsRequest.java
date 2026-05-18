package com.store.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
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
    @NotNull(message = "Currency ID is required")
    @JsonProperty("CURRENCY_ID")
    Long CURRENCY_ID;

    @NotBlank(message = "Currency symbol is required")
    @Size(max = 10, message = "Currency symbol cannot exceed 10 characters")
    @JsonProperty("CURRENCY_SYMBOL")
    String CURRENCY_SYMBOL;

    @NotBlank(message = "Currency symbol position is required")
    @Pattern(regexp = "^(LEFT|RIGHT)$", message = "Currency symbol position must be LEFT or RIGHT")
    @JsonProperty("CURRENCY_SYMBOL_POSITION")
    String CURRENCY_SYMBOL_POSITION;

    @NotNull(message = "Decimal digits is required")
    @Min(value = 0, message = "Decimal digits must be non-negative")
    @Max(value = 5, message = "Decimal digits cannot exceed 5")
    @JsonProperty("DECIMAL_DIGITS")
    Integer DECIMAL_DIGITS;

    @NotBlank(message = "Decimal point type is required")
    @Pattern(regexp = "^(DOT|COMMA)$", message = "Decimal point type must be DOT or COMMA")
    @JsonProperty("DECIMAL_POINT_TYPE")
    String DECIMAL_POINT_TYPE;

    @NotBlank(message = "Thousands point type is required")
    @Pattern(regexp = "^(DOT|COMMA|SPACE|NONE)$", message = "Thousands point type must be DOT, COMMA, SPACE, or NONE")
    @JsonProperty("THOUSANDS_POINT_TYPE")
    String THOUSANDS_POINT_TYPE;

    @Size(max = 500, message = "Site logo URL cannot exceed 500 characters")
    @JsonProperty("SITE_LOGO")
    String SITE_LOGO;
}
