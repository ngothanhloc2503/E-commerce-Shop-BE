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

    @Column(nullable = false, unique = true)
    String name;
    @Column(nullable = false, unique = true)
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
    List<ProductDetail> details = new ArrayList<>();

    public void setAlias() {
        this.alias = (this.name + "-id." + this.id).trim().replace("(", "")
                .replace(")", "")
                .replace("/", "-")
                .replace(" ", "-")
                .replaceAll("(-)+", "-");
    }

    @Transient
    public float getDiscountPrice() {
        if (discountPercent > 0) {
            return price * ((100 - discountPercent) / 100);
        }
        return this.price;
    }
}
