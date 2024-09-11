package com.store.ecommerce.dto;

import com.store.ecommerce.entity.Product;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImageDTO {
    Long id;
    String name;
    Long productID;
    String imagePath;
}
