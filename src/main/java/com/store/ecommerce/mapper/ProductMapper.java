package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.*;
import com.store.ecommerce.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, BrandMapper.class})
public interface ProductMapper {

    @Mapping(source = "createdTime", target = "createdTime", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    @Mapping(source = "updatedTime", target = "updatedTime", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    @Mapping(source = "images", target = "images", qualifiedByName = "imagesToImagesDTO")
    @Mapping(source = "details", target = "details", qualifiedByName = "detailsToDetailsDTO")
    ProductDTO toProductDTO(Product product);

    List<ProductDTO> toProductDTOList(List<Product> products);

    @Named("imagesToImagesDTO")
    default Set<ProductImageDTO> imagesToImagesDTO(Set<ProductImage> images) {
        if (images == null) {
            return null;
        }
        return images.stream()
                .map(this::toProductImageDTO)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Named("detailsToDetailsDTO")
    default Set<ProductDetailDTO> detailsToDetailsDTO(Set<ProductDetail> details) {
        if (details == null) {
            return null;
        }
        return details.stream()
                .map(this::toProductDetailDTO)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Mapping(source = "product.id", target = "productID")
    ProductImageDTO toProductImageDTO(ProductImage image);

    @Mapping(source = "product.id", target = "productID")
    ProductDetailDTO toProductDetailDTO(ProductDetail detail);

    @Mapping(source = "createdTime", target = "createdTime", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    @Mapping(source = "updatedTime", target = "updatedTime", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    @Mapping(source = "category", target = "category", qualifiedByName = "categoryDtoToCategory")
    @Mapping(source = "brand", target = "brand", qualifiedByName = "brandDtoToBrand")
    Product toProduct(ProductDTO productDTO);

    @Named("brandDtoToBrand")
    default Brand brandDtoToBrand(BrandDTO brandDTO) {
        if (brandDTO == null) {
            return null;
        }
        Brand brand = new Brand();
        brand.setId(brandDTO.getId());
        return brand;
    }

    @Named("categoryDtoToCategory")
    default Category categoryDtoToCategory(CategoryDTO categoryDTO) {
        if (categoryDTO == null) {
            return null;
        }
        Category category = new Category();
        category.setId(categoryDTO.getId());
        return category;
    }
}
