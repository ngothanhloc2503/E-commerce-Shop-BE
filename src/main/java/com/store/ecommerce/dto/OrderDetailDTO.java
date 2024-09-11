package com.store.ecommerce.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.store.ecommerce.util.CustomSensitiveSerializer;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetailDTO {
    Long id;
    Long productId;
    String productName;
    String productImageName;
    String productImagePath;
    int quantity;

    @JsonSerialize(using = CustomSensitiveSerializer.class)
    float productCost;
    @JsonSerialize(using = CustomSensitiveSerializer.class)
    float productCostTotal;

    float shippingCost;
    float unitPrice;
    float subtotal;
}
