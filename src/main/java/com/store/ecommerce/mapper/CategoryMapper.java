package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.entity.Category;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CategoryMapper {

    CategoryDTO toCategoryDTO(Category category);

    @Mapping(target = "parent", source = "parentID", qualifiedByName = "mapParentIdToCategory")
    @Mapping(target = "children", ignore = true)
    Category toCategory(CategoryDTO categoryDTO);

    List<CategoryDTO> toCategoryDTOList(List<Category> categories);

    List<Category> toCategoryList(List<CategoryDTO> categoryDTOs);

    @Named("mapParentIdToCategory")
    default Category mapParentIdToCategory(Long parentId) {
        if (parentId == null) {
            return null;
        }
        Category parent = new Category();
        parent.setId(parentId);
        return parent;
    }
}
