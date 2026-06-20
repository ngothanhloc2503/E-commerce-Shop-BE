package com.store.ecommerce.service;

import com.store.ecommerce.dto.response.WishlistResponse;
import java.util.List;

public interface WishlistService {
    List<WishlistResponse> getWishlistByUserId(Long userId);
    WishlistResponse addToWishlist(Long userId, Long productId);
    boolean removeFromWishlist(Long userId, Long productId);
    boolean isInWishlist(Long userId, Long productId);
    long getWishlistCount(Long userId);
}