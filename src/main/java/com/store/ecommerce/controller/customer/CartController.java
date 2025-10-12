package com.store.ecommerce.controller.customer;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/cart")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("")
    public ResponseEntity<?> getCart(Authentication authentication) {
        try {
            CartDTO cart = cartService.findByUserEmail(authentication.getName());
            return ResponseEntity.ok(cart);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/add-item")
    public ResponseEntity<?> addItemToCart(Authentication authentication,
                                       @RequestParam(name = "productId") Long productId,
                                       @RequestParam(name = "quantity") int quantity) {
        try {
            CartDTO cart = cartService.addItemToCart(
                    authentication.getName(), productId, quantity);
            return ResponseEntity.ok(cart);
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> deleteCartItem(Authentication authentication,
                                            @PathVariable(name = "cartItemId") Long cartId) {
        try {
            return ResponseEntity.ok(cartService.deleteCartItem(
                    authentication.getName(), cartId));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }
}
