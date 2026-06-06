package com.store.ecommerce.service.impl;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.entity.*;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.CartMapper;
import com.store.ecommerce.repository.CartItemRepository;
import com.store.ecommerce.repository.CartRepository;
import com.store.ecommerce.repository.ProductRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.AddressService;
import com.store.ecommerce.service.CartService;
import com.store.ecommerce.service.ShippingRateService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AWSS3ServiceImpl awsS3Service;
    private final AddressService addressService;
    private final ShippingRateService shippingRateService;
    private final CartMapper cartMapper;

    @Override
    public CartDTO findByUserEmail(String email) throws NotFoundException {
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new NotFoundException("Could not find any user with email: " + email);
        }

        Optional<Cart> cart = cartRepository.findByUserEmail(email);
        if (cart.isEmpty()) return null;

        CartDTO cartDTO = cartMapper.toCartDTO(cart.get());
        setImagePathForCartItem(cartDTO);

        Address defaultAddress = addressService.getDefaultAddress(email);
        cartDTO.setShippingSupported(shippingRateService.isShippingSupported(defaultAddress));
        return cartDTO;
    }

    @Override
    public CartDTO addItemToCart(String email, Long productId, int quantity) throws NotFoundException, ConflictException {
        if (quantity <= 0) {
            throw new ConflictException("Quantity must be greater than 0.");
        }

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));

        Cart cart = user.getCart();
        if (cart == null) {
            cart = new Cart();
            user.setCart(cart);
            cart = userRepository.save(user).getCart();
        }

        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NotFoundException("Could not find any product with id: " + productId));

        Optional<CartItem> existingCartItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingCartItem.isPresent()) {
            CartItem cartItem = existingCartItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cart.addItem(cartItem);
        }

        CartDTO savedCart = cartMapper.toCartDTO(cartRepository.save(cart));
        setImagePathForCartItem(savedCart);
        return savedCart;
    }

    @Override
    public CartDTO deleteCartItem(String email, Long cartItemId) throws NotFoundException, ConflictException {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email: " + email));
        Cart cart = user.getCart();
        if (cart == null) {
            throw new NotFoundException("Cart not found for user with email " + email);
        }

        CartItem cartItemToRemove = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("CartItem not found"));

        cart.setTotal(cart.getTotal() - cartItemToRemove.getSubtotal());

        cart.getItems().remove(cartItemToRemove);

        CartDTO savedCart = cartMapper.toCartDTO(cartRepository.save(cart));
        setImagePathForCartItem(savedCart);
        return savedCart;
    }

    @Override
    public void deleteByCartId(Long cartId) {
        Optional<Cart> cartOpt = cartRepository.findById(cartId);

        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();

            cart.getItems().clear();
            cart.setTotal(0.0f);
            cartRepository.save(cart);
        }
    }

    private void setImagePathForCartItem(CartDTO cartDTO) {
        cartDTO.getItems().forEach(item -> {
            String dir = "product-images/" + item.getProductID();
            item.setProductImagePath(awsS3Service.getImagePath(dir, item.getProductImage()));
        });
    }
}