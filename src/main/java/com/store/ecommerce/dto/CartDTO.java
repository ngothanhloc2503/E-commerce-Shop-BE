package com.store.ecommerce.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartDTO {
    Long id;
    float total;
    List<CartItemDTO> items = new ArrayList<>();
    boolean shippingSupported;
}
