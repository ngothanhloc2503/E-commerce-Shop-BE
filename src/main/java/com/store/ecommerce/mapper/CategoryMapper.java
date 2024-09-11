package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "image", target = "image")
    @Mapping(source = "enabled", target = "enabled")
    @Mapping(source = "children", target = "children")
    @Mapping(source = "listParentName", target = "listParentName")
    @Mapping(source = "parentID", target = "parentID")
    CategoryDTO toCategoryDTO(Category category);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "image", target = "image")
    @Mapping(source = "enabled", target = "enabled")
    Category toCategory(CategoryDTO categoryDTO);
}
