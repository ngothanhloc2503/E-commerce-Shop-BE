package com.store.ecommerce.controller;

import com.store.ecommerce.dto.response.ApiErrorResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.WishlistResponse;
import com.store.ecommerce.entity.CustomUserDetails;
import com.store.ecommerce.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "APIs for managing user wishlist/favorites")
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(
            summary = "Get wishlist",
            description = "Retrieve the current authenticated user's wishlist"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wishlist retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<List<WishlistResponse>>> getWishlist(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<WishlistResponse> wishlist = wishlistService.getWishlistByUserId(userId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<WishlistResponse>>builder()
                        .success(true)
                        .message("Wishlist retrieved successfully")
                        .data(wishlist)
                        .build()
        );
    }

    @Operation(
            summary = "Add item to wishlist",
            description = "Add a product to the user's wishlist"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item added to wishlist successfully",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Product already in wishlist",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product or User not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/items/{productId}")
    public ResponseEntity<ApiSuccessResponse<WishlistResponse>> addItemToWishlist(
            Authentication authentication,
            @Parameter(description = "Product ID", required = true)
            @PathVariable(name = "productId") Long productId) {

        Long userId = getUserIdFromAuthentication(authentication);
        WishlistResponse wishlistItem = wishlistService.addToWishlist(userId, productId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<WishlistResponse>builder()
                        .success(true)
                        .message("Item added to wishlist successfully")
                        .data(wishlistItem)
                        .build()
        );
    }

    @Operation(summary = "Remove item from wishlist", description = "Remove a specific product from the user's wishlist")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item removed successfully",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Wishlist item not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiSuccessResponse<Boolean>> removeItemFromWishlist(
            Authentication authentication,
            @PathVariable(name = "productId") Long productId) {

        Long userId = getUserIdFromAuthentication(authentication);
        boolean removed = wishlistService.removeFromWishlist(userId, productId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<Boolean>builder()
                        .success(true)
                        .message(removed ? "Item removed from wishlist successfully" : "Item not found in wishlist")
                        .data(removed)
                        .build()
        );
    }

    @Operation(summary = "Check if product is in wishlist", description = "Check if a specific product is in the user's wishlist")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wishlist status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiSuccessResponse<Boolean>> isInWishlist(
            Authentication authentication,
            @PathVariable(name = "productId") Long productId) {

        Long userId = getUserIdFromAuthentication(authentication);
        boolean isInWishlist = wishlistService.isInWishlist(userId, productId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<Boolean>builder()
                        .success(true)
                        .message(isInWishlist ? "Product is in wishlist" : "Product is not in wishlist")
                        .data(isInWishlist)
                        .build()
        );
    }

    @Operation(summary = "Get wishlist count", description = "Get the total number of items in the user's wishlist")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Wishlist count retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/count")
    public ResponseEntity<ApiSuccessResponse<Long>> getWishlistCount(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        long count = wishlistService.getWishlistCount(userId);

        return ResponseEntity.ok(
                ApiSuccessResponse.<Long>builder()
                        .success(true)
                        .message("Wishlist count retrieved successfully")
                        .data(count)
                        .build()
        );
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }
}