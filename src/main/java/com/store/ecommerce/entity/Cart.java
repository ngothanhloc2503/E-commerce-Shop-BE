package com.store.ecommerce.entity;

import com.store.ecommerce.dto.CartDTO;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    float total;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = true)
    User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    Set<CartItem> items = new HashSet<>();

    public void addItem(CartItem cartItem) {
        cartItem.setCart(this);
        items.add(cartItem);
        total += cartItem.getSubtotal();
    }

    public void setItems(Set<CartItem> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
    }
}
