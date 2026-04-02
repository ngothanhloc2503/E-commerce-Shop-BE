package com.store.ecommerce.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class CategoryDTO {
    Long id;
    String name;
    String description;
    String image;
    boolean enabled;
    Long parentID;
    String imagePath;
    List<String> listParentName;
    Set<CategoryDTO> children = new HashSet<>();
}
