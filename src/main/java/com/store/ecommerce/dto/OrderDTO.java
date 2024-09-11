package com.store.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.store.ecommerce.enums.OrderStatus;
import com.store.ecommerce.enums.PaymentMethod;
import com.store.ecommerce.util.CustomSensitiveSerializer;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDTO {
    Long id;
    Long userId;
    String firstName;
    String lastName;
    String phoneNumber;
    String addressLine1;
    String addressLine2;
    String city;
    String state;
    String country;
    String postalCode;
    float shippingCost;

    @JsonSerialize(using = CustomSensitiveSerializer.class)
    float productCost;

    float subtotal;
    float tax;
    float total;
    Date orderTime;
    int deliverDays;
    Date deliverDate;
    PaymentMethod paymentMethod;
    OrderStatus status;
    String userFullName;

    Set<OrderDetailDTO> orderDetails = new HashSet<>();
    List<OrderTrackDTO> orderTrack = new ArrayList<>();

    @JsonIgnore
    public String getAddress() {
        String address = firstName;

        if (lastName != null && !lastName.isEmpty()) address += " " + lastName;
        if (!addressLine1.isEmpty()) address += ", " + addressLine1;
        if (addressLine2 != null && !addressLine2.isEmpty()) address += ", " + addressLine2;
        if (!city.isEmpty()) address += ", " + city;

        address += ", " + state;
        address += ", " + country;

        if (!postalCode.isEmpty()) address += ". Postal Code: " + postalCode;
        if (!phoneNumber.isEmpty()) address += ". Phone Number: " + phoneNumber;

        return address;
    }
}
