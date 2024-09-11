package com.store.ecommerce.entity;

import com.store.ecommerce.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Entity
@Table(name = "order_track")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String notes;
    Date updatedTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "order_id")
    Order order;
}
