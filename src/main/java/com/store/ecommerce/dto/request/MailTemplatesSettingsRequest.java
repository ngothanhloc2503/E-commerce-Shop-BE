package com.store.ecommerce.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "Customer verify subject is required")
    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    @JsonProperty("CUSTOMER_VERIFY_SUBJECT")
    String CUSTOMER_VERIFY_SUBJECT;

    @NotBlank(message = "Customer verify content is required")
    @Size(max = 10000, message = "Content cannot exceed 10000 characters")
    @JsonProperty("CUSTOMER_VERIFY_CONTENT")
    String CUSTOMER_VERIFY_CONTENT;

    @NotBlank(message = "Order confirmation subject is required")
    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    @JsonProperty("ORDER_CONFIRMATION_SUBJECT")
    String ORDER_CONFIRMATION_SUBJECT;

    @NotBlank(message = "Order confirmation content is required")
    @Size(max = 10000, message = "Content cannot exceed 10000 characters")
    @JsonProperty("ORDER_CONFIRMATION_CONTENT")
    String ORDER_CONFIRMATION_CONTENT;
}
