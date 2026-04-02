package com.store.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import com.store.ecommerce.entity.ProductDetail;
import com.store.ecommerce.entity.ProductImage;
import com.store.ecommerce.util.CustomSensitiveSerializer;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class ProductDTO {
    Long id;
    String name;
    String alias;
    String description;
    String summary;
    boolean enabled;
    boolean inStock;
    int reviewCount;
    float averageRating;
    float discountPercent;
    float price;

    @JsonSerialize(using = CustomSensitiveSerializer.class)
    float cost;

    double discountPrice;
    float length;
    float width;
    float height;
    float weight;
    Date createdTime;
    Date updatedTime;
    CategoryDTO category;
    BrandDTO brand;

    String mainImage;
    String mainImagePath;

    Set<ProductImageDTO> images;

    List<ProductDetailDTO> details;

    @JsonIgnore
    public boolean containsImageName(String imageName) {
        for (ProductImageDTO image : images) {
            if (image.getName().equals(imageName)) {
                return true;
            }
        }

        return false;
    }
}
