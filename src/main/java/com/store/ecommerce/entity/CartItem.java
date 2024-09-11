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
        return this.product.getDiscountPrice() * this.quantity;
    }

    public CartItemDTO toCartItemDTO() {
        CartItemDTO cartItemDTO = new CartItemDTO();
        cartItemDTO.setId(this.id);
        cartItemDTO.setProductID(this.product.getId());
        cartItemDTO.setProductName(this.product.getName());
        cartItemDTO.setProductAlias(this.product.getAlias());
        cartItemDTO.setProductPrice(this.product.getPrice());
        cartItemDTO.setProductDiscountPercent(this.product.getDiscountPercent());
        cartItemDTO.setProductDiscountPrice(this.product.getDiscountPrice());
        cartItemDTO.setProductImage(product.getMainImage());
        cartItemDTO.setItemWeight(calculateFinalWeight());
        cartItemDTO.setQuantity(this.quantity);
        cartItemDTO.setSubtotal(this.getSubtotal());

        return cartItemDTO;
    }

    public float calculateFinalWeight() {
        float dimWeight = (this.product.getLength() * this.product.getWidth() * this.product.getHeight()) / DIM_DIVISOR;
        return Math.max(this.product.getWeight(), dimWeight) * this.quantity;
    }
}
