package com.store.ecommerce.service;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.dto.CartItemDTO;
import com.store.ecommerce.entity.Cart;
import com.store.ecommerce.exception.NotFoundException;

public interface CartService {
    CartDTO findByUserEmail(String email) throws NotFoundException;

    CartDTO addItemToCart(String username, Long productId, int quantity) throws NotFoundException;

    CartDTO deleteCartItem(String email, Long cartItemId) throws NotFoundException;

    void deleteByCartId(Long cartId);
}
