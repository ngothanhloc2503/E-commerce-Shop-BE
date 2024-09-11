package com.store.ecommerce.dto;

import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.CartItem;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutInfo {
    List<CartItemDTO> listItems;
    String address;
    float productTotal;
    float shippingCostTotal;
    float paymentTotal;
    int deliverDays;
    Date deliverDate;
    boolean codSupported;
    String currencyCode;
}
