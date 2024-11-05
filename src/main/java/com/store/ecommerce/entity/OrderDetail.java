package com.store.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    int quantity;
    float productCost;
    float shippingCost;
    float unitPrice;
    float subtotal;

    @ManyToOne
    @JoinColumn(name = "product_id")
    Product product;

    @ManyToOne
    @JoinColumn(name = "order_id")
    Order order;

    public OrderDetail(String categoryName, int quantity, float productCost, float shippingCost, float subtotal) {
        this.product = new Product();
        this.product.setCategory(new Category(categoryName));
        this.quantity = quantity;
        this.productCost = productCost;
        this.shippingCost = shippingCost;
        this.subtotal = subtotal;
    }

    public OrderDetail(int quantity, String productName, float productCost, float shippingCost, float subtotal) {
        this.product = new Product(productName);
        this.quantity = quantity;
        this.productCost = productCost;
        this.shippingCost = shippingCost;
        this.subtotal = subtotal;
    }
}
