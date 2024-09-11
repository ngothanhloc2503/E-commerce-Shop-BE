package com.store.ecommerce.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
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
