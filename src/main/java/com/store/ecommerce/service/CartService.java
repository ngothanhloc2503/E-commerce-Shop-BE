package com.store.ecommerce.service;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;

public interface CartService {
    CartDTO findByUserEmail(String email) throws NotFoundException;

    CartDTO addItemToCart(String username, Long productId, int quantity) throws NotFoundException, ConflictException;

    CartDTO deleteCartItem(String email, Long cartItemId) throws NotFoundException, ConflictException;

    void deleteByCartId(Long cartId);
}
