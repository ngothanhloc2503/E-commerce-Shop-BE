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
public class MailTemplatesSettingsRequest {
    @JsonProperty("CUSTOMER_VERIFY_SUBJECT")
    String CUSTOMER_VERIFY_SUBJECT;

    @JsonProperty("CUSTOMER_VERIFY_CONTENT")
    String CUSTOMER_VERIFY_CONTENT;

    @JsonProperty("ORDER_CONFIRMATION_SUBJECT")
    String ORDER_CONFIRMATION_SUBJECT;

    @JsonProperty("ORDER_CONFIRMATION_CONTENT")
    String ORDER_CONFIRMATION_CONTENT;
}
