package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.*;
import com.store.ecommerce.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mapper
public interface ProductMapper {
    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "alias", target = "alias")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "summary", target = "summary")
    @Mapping(source = "enabled", target = "enabled")
    @Mapping(source = "inStock", target = "inStock")
    @Mapping(source = "reviewCount", target = "reviewCount")
    @Mapping(source = "averageRating", target = "averageRating")
    @Mapping(source = "discountPercent", target = "discountPercent")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "cost", target = "cost")
    @Mapping(source = "discountPrice", target = "discountPrice")
    @Mapping(source = "length", target = "length")
    @Mapping(source = "width", target = "width")
    @Mapping(source = "height", target = "height")
    @Mapping(source = "weight", target = "weight")
    @Mapping(source = "createdTime", target = "createdTime", dateFormat = "yyyy-MM-ddThh:mm:ss")
    @Mapping(source = "updatedTime", target = "updatedTime", dateFormat = "yyyy-MM-ddThh:mm:ss")
    @Mapping(source = "category", target = "category")
    @Mapping(source = "brand", target = "brand")
    @Mapping(source = "mainImage", target = "mainImage")
    @Mapping(source = "images", target = "images", qualifiedByName = "imagesToImagesDTO")
    @Mapping(source = "details", target = "details", qualifiedByName = "detailsToDetailsDTO")
    public ProductDTO toProductDTO(Product product);

    @Named("imagesToImagesDTO")
    public static Set<ProductImageDTO> imagesToImagesDTO(Set<ProductImage> images) {
        Set<ProductImageDTO> result = new HashSet<>();
        for (ProductImage productImage : images ) {
            ProductImageDTO temp = new ProductImageDTO();
            temp.setId(productImage.getId());
            temp.setName(productImage.getName());
            temp.setProductID(productImage.getProduct().getId());
            result.add(temp);
        }

        return result;
    }

    @Named("detailsToDetailsDTO")
    public static List<ProductDetailDTO> detailsToDetailsDTO(List<ProductDetail> details) {
        List<ProductDetailDTO> result = new ArrayList<>();
        for (ProductDetail productDetail : details ) {
            ProductDetailDTO temp = new ProductDetailDTO();
            temp.setId(productDetail.getId());
            temp.setName(productDetail.getName());
            temp.setValue(productDetail.getValue());
            temp.setProductID(productDetail.getProduct().getId());
            result.add(temp);
        }

        return result;
    }

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "summary", target = "summary")
    @Mapping(source = "enabled", target = "enabled")
    @Mapping(source = "inStock", target = "inStock")
    @Mapping(source = "reviewCount", target = "reviewCount")
    @Mapping(source = "averageRating", target = "averageRating")
    @Mapping(source = "discountPercent", target = "discountPercent")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "cost", target = "cost")
    @Mapping(source = "length", target = "length")
    @Mapping(source = "width", target = "width")
    @Mapping(source = "height", target = "height")
    @Mapping(source = "weight", target = "weight")
    @Mapping(source = "createdTime", target = "createdTime", dateFormat = "yyyy-MM-ddThh:mm:ss")
    @Mapping(source = "updatedTime", target = "updatedTime", dateFormat = "yyyy-MM-ddThh:mm:ss")
    @Mapping(source = "category", target = "category", qualifiedByName = "categoryDtoToCategory")
    @Mapping(source = "brand", target = "brand", qualifiedByName = "brandDtoToBrand")
    @Mapping(source = "mainImage", target = "mainImage")
    public Product toProduct(ProductDTO productDTO);

    @Named("brandDtoToBrand")
    public static Brand brandDtoToBrand(BrandDTO brandDTO) {
        return new Brand(brandDTO.getId());
    }

    @Named("categoryDtoToCategory")
    public static Category categoryDtoToCategory(CategoryDTO categoryDTO) {
        return new Category(categoryDTO.getId());
    }
}
