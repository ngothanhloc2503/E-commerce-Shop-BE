package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.dto.CartItemDTO;
import com.store.ecommerce.entity.Cart;
import com.store.ecommerce.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface CartMapper {

    @Mapping(target = "shippingSupported", ignore = true)
    CartDTO toCartDTO(Cart cart);

    @Mapping(source = "product.id", target = "productID")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.alias", target = "productAlias")
    @Mapping(source = "product.mainImage", target = "productImage")
    @Mapping(source = "product.price", target = "productPrice")
    @Mapping(source = "product.discountPercent", target = "productDiscountPercent")
    @Mapping(source = "product.discountPrice", target = "productDiscountPrice")
    CartItemDTO toCartItemDTO(CartItem cartItem);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cart", ignore = true)
    CartItem toCartItem(CartItemDTO cartItemDTO);

    List<CartItemDTO> toCartItemDTOs(Set<CartItem> items);
}