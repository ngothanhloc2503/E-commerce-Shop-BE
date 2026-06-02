package com.store.ecommerce.entity;

import com.store.ecommerce.enums.OrderStatus;
import com.store.ecommerce.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends AbstractAddress{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    float shippingCost;
    float productCost;
    float subtotal;
    float tax;
    float total;

    Date orderTime;
    int deliverDays;
    Date deliverDate;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "orders_payment_method")
    PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "orders_status")
    OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    Set<OrderDetail> orderDetails = new HashSet<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("updatedTime ASC")
    List<OrderTrack> orderTrack = new ArrayList<>();

    public Order(Long id, Date orderTime, float productCost, float subtotal, float total) {
        this.id = id;
        this.orderTime = orderTime;
        this.productCost = productCost;
        this.subtotal = subtotal;
        this.total = total;
    }

    @Transient
    public String getUserFullName() {
        return user != null ? user.getFullName() : null;
    }

    public void setAddress(Address address) {
        setFirstName(address.getFirstName());
        setLastName(address.getLastName());
        setPhoneNumber(address.getPhoneNumber());
        setAddressLine1(address.getAddressLine1());
        setAddressLine2(address.getAddressLine2());
        setCity(address.getCity());
        setCountry(address.getCountry());
        setState(address.getState());
        setPostalCode(address.getPostalCode());
    }
}
