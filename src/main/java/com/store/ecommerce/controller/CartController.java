package com.store.ecommerce.controller;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("")
    public ResponseEntity<?> getCart(Authentication authentication) {

        CartDTO cart = cartService.findByUserEmail(authentication.getName());
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItemToCart(Authentication authentication,
                                           @RequestParam(name = "productId") Long productId,
                                           @RequestParam(name = "quantity") int quantity) {

        CartDTO cart = cartService.addItemToCart(
                authentication.getName(), productId, quantity);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> deleteCartItem(Authentication authentication,
                                            @PathVariable(name = "cartItemId") Long cartId) {

        return ResponseEntity.ok(cartService.deleteCartItem(
                authentication.getName(), cartId));
    }
}
