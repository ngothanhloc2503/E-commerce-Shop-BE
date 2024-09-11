package com.store.ecommerce.dto;

import com.store.ecommerce.entity.Product;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemDTO {
    Long id;
    Long productID;
    String productName;
    String productAlias;
    float productPrice;
    float productDiscountPercent;
    float productDiscountPrice;
    String productImage;
    String productImagePath;
    float itemWeight;
    int quantity;
    float shippingCost;
    double subtotal;
}
