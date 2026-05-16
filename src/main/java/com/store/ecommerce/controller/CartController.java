package com.store.ecommerce.controller;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.wrapper.CartWrapper;
import com.store.ecommerce.service.CartService;
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

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "APIs for managing user shopping cart")
public class CartController {
    private final CartService cartService;

    @Operation(
            summary = "Get cart",
            description = "Retrieve the current authenticated user's shopping cart"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cart retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CartWrapper.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<CartDTO>> getCart(Authentication authentication) {

        CartDTO cart = cartService.findByUserEmail(authentication.getName());

        return ResponseEntity.ok(
                ApiSuccessResponse.<CartDTO>builder()
                        .success(true)
                        .message("Cart retrieved successfully")
                        .data(cart)
                        .build()
        );
    }

    @Operation(
            summary = "Add item to cart",
            description = "Add a product to cart or update quantity if it already exists"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item added to cart successfully",
                    content = @Content(schema = @Schema(implementation = CartWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid product or quantity"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/items")
    public ResponseEntity<ApiSuccessResponse<CartDTO>> addItemToCart(
            Authentication authentication,
            @Parameter(description = "Product ID", required = true)
            @RequestParam(name = "productId") Long productId,
            @Parameter(description = "Quantity", required = true)
            @RequestParam(name = "quantity") int quantity) {

        CartDTO cart = cartService.addItemToCart(
                authentication.getName(), productId, quantity);

        return ResponseEntity.ok(
                ApiSuccessResponse.<CartDTO>builder()
                        .success(true)
                        .message("Item added to cart successfully")
                        .data(cart)
                        .build()
        );
    }

    @Operation(
            summary = "Remove item from cart",
            description = "Remove a specific item from the user's cart"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item removed successfully",
                    content = @Content(schema = @Schema(implementation = CartWrapper.class))
            ),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiSuccessResponse<CartDTO>> deleteCartItem(Authentication authentication,
                                            @PathVariable(name = "cartItemId") Long cartItemId) {

        CartDTO cart = cartService.deleteCartItem(
                authentication.getName(),
                cartItemId
        );

        return ResponseEntity.ok(
                ApiSuccessResponse.<CartDTO>builder()
                        .success(true)
                        .message("Item removed from cart successfully")
                        .data(cart)
                        .build()
        );
    }
}
