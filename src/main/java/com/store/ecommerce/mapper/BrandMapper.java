package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface BrandMapper {

    @Mapping(source = "categories", target = "listCategoryIDs", qualifiedByName = "categoryIds")
    @Mapping(source = "categories", target = "nameOfCategories", qualifiedByName = "categoryNames")
    BrandDTO toBrandDTO(Brand brand);

    @Mapping(target = "categories", ignore = true)
    Brand toBrand(BrandDTO brandDTO);

    @Named("categoryIds")
    default List<Long> mapCategoryIds(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        return categories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());
    }

    @Named("categoryNames")
    default String mapCategoryNames(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return "";
        }
        return categories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));
    }

    List<BrandDTO> toBrandDTOs(List<Brand> brands);

    List<Brand> toBrands(List<BrandDTO> brandDTOs);
}
