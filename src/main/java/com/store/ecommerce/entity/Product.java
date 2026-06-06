package com.store.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;
    @Column(nullable = false)
    String alias;
    @Column(nullable = false, length = 4096)
    String description;
    @Column(nullable = false, length = 1024)
    String summary;

    boolean enabled;
    boolean inStock;
    int reviewCount;
    float averageRating;

    float discountPercent;
    float price;
    float cost;

    float length;
    float width;
    float height;
    float weight;

    Date createdTime;
    Date updatedTime;

    @ManyToOne
    @JoinColumn(name = "category_id")
    Category category;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    Brand brand;

    @Column(nullable = false)
    String mainImage;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    Set<ProductImage> images = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<ProductDetail> details = new HashSet<>();

    public Product(String name) {
        this.name = name;
    }

    public void setAlias() {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);

        String rawAlias = this.name + "-" + randomSuffix;

        this.alias = rawAlias.trim()
                .replace("(", "")
                .replace(")", "")
                .replace("/", "-")
                .replace(".", "-")
                .replace(" ", "-")
                .replaceAll("-+", "-")
                .toLowerCase();
    }

    @Transient
    public float getDiscountPrice() {
        if (discountPercent > 0) {
            return price * ((100 - discountPercent) / 100);
        }
        return this.price;
    }
}
