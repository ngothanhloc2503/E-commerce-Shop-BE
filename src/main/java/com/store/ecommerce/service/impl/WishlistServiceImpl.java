package com.store.ecommerce.service.impl;

import com.store.ecommerce.dto.response.WishlistResponse;
import com.store.ecommerce.entity.Product;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.entity.Wishlist;
import com.store.ecommerce.exception.DuplicateWishlistItemException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.ProductRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.repository.WishlistRepository;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.WishlistService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AWSS3Service awsS3Service;

    @Override
    public List<WishlistResponse> getWishlistByUserId(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserId(userId);
        return wishlists.stream()
                .map(this::toWishlistResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WishlistResponse addToWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new DuplicateWishlistItemException("Product already in wishlist");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        return toWishlistResponse(savedWishlist);
    }

    @Override
    public boolean removeFromWishlist(Long userId, Long productId) {
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            return false;
        }
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        return true;
    }

    @Override
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }

    private WishlistResponse toWishlistResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();

        float discountPrice = product.getPrice();
        if (product.getDiscountPercent() > 0) {
            discountPrice = product.getPrice() * (100 - product.getDiscountPercent()) / 100;
        }

        String brandName = product.getBrand() != null ? product.getBrand().getName() : "";
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "";

        String dir = "product-images/" + product.getId();
        String mainImagePath = awsS3Service.getImagePath(dir, product.getMainImage());

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productAlias(product.getAlias())
                .productSummary(product.getSummary())
                .price(product.getPrice())
                .discountPercent(product.getDiscountPercent())
                .discountPrice(discountPrice)
                .mainImage(mainImagePath)
                .inStock(product.isInStock())
                .brandName(brandName)
                .categoryName(categoryName)
                .addedDate(wishlist.getCreatedAt())
                .build();
    }
}