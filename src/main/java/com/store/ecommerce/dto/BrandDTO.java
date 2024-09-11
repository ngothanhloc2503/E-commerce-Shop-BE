package com.store.ecommerce.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrandDTO {
    Long id;
    String name;
    String logo;
    List<Long> listCategoryIDs;
    String nameOfCategories;
    String logoImagePath;

    public BrandDTO(Long id) {
        this.id = id;
    }
}
