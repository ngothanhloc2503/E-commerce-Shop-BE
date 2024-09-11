package com.store.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "shipping_rates")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShippingRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    float rate;
    int days;

    @Column(name = "cod_supported")
    boolean codSupported;

    @Column(nullable = false)
    String country;

    @Column(nullable = false)
    String state;
}
