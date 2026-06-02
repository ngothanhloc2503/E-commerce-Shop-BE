package com.store.ecommerce.entity;

import com.store.ecommerce.dto.CartItemDTO;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import static com.store.ecommerce.common.Constants.DIM_DIVISOR;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    int quantity;

    @Transient
    public float getSubtotal() {
        if (this.product == null) {
            return 0.0f;
        }
        return this.product.getDiscountPrice() * this.quantity;
    }

    public float calculateFinalWeight() {
        if (this.product == null) {
            return 0.0f;
        }
        float dimWeight = (this.product.getLength() * this.product.getWidth() * this.product.getHeight()) / DIM_DIVISOR;
        return Math.max(this.product.getWeight(), dimWeight) * this.quantity;
    }
}
