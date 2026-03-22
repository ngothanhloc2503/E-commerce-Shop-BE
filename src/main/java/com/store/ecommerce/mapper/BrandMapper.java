package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BrandMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "logo", target = "logo")
    @Mapping(source = "listCategoryIDs", target = "listCategoryIDs")
    @Mapping(source = "nameOfCategories", target = "nameOfCategories")
    public BrandDTO toBrandDTO(Brand brand);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "logo", target = "logo")
    @Mapping(target = "categories", ignore = true)
    public Brand toBrand(BrandDTO brandDTO);
}
