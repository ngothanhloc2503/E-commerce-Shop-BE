package com.store.ecommerce.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.boot.context.properties.bind.DefaultValue;

@MappedSuperclass
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class AbstractAddress {
    @Column(nullable = false)
    String firstName;

    @Column(nullable = false)
    String lastName;

    @Column(nullable = false)
    String phoneNumber;

    @Column(name = "address_line_1", nullable = false)
    String addressLine1;

    @Column(name = "address_line_2")
    @ColumnDefault("''")
    String addressLine2;

    @ColumnDefault("''")
    String city;

    @Column(nullable = false)
    String state;

    @Column(nullable = false)
    String country;

    @Column(nullable = false)
    String postalCode;
}
