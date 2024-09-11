package com.store.ecommerce.service.impl;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.entity.*;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.CartItemRepository;
import com.store.ecommerce.repository.CartRepository;
import com.store.ecommerce.repository.ProductRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.AddressService;
import com.store.ecommerce.service.CartService;
import com.store.ecommerce.service.ShippingRateService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class CartServiceImpl implements CartService {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AWSS3ServiceImpl awsS3Service;

    @Autowired
    private AddressService addressService;

    @Autowired
    private ShippingRateService shippingRateService;

    @Override
    public CartDTO findByUserEmail(String email) throws NotFoundException {
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new NotFoundException("Could not found any user with email: " + email);
        }

        Cart cart = cartRepository.findByUserEmail(email);
        if (cart == null) return null;
        CartDTO cartDTO = cart.toCartDTO();
        setImagePathForCartItem(cartDTO);

        Address defaultAddress = addressService.getDefaultAddress(email);
        cartDTO.setShippingSupported(shippingRateService.isShippingSupported(defaultAddress));
        return cartDTO;
    }

    @Override
    public CartDTO addItemToCart(String email, Long productId, int quantity) throws NotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not found any user with email: " + email));

        // Check if the user has a cart; if not, create one
        Cart cart = user.getCart();
        if (cart == null) {
            cart = new Cart();
            user.setCart(cart);
            cart = userRepository.save(user).getCart(); // Save user with the new cart
        }

        // Add product to the cart
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NotFoundException("Could not found any user with id: " + productId));

        // Check if the product is already in the cart
        Optional<CartItem> existingCartItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingCartItem.isPresent()) {
            // If the product is already in the cart, update the quantity
            CartItem cartItem = existingCartItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            if (cartItem.getQuantity() <= 0) {
                throw new ConflictException("Quantity must be greater than 0.");
            }
            cart.setTotal(cart.getTotal() + quantity * cartItem.getProduct().getDiscountPrice());
        } else {
            // If the product is not in the cart, add it as a new item
            if (quantity <= 0) {
                throw new ConflictException("Quantity must be greater than 0.");
            }
            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cart.addItem(cartItem);
        }

        // Save the updated cart
        CartDTO savedCart = cartRepository.save(cart).toCartDTO();
        setImagePathForCartItem(savedCart);
        return savedCart;
    }

    @Override
    public CartDTO deleteCartItem(String email, Long cartItemId) throws NotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user with email" + email));
        Cart cart = user.getCart();
        if (cart == null) {
            throw new NotFoundException("Cart not found for user with email " + email);
        }

        CartItem cartItem = cartItemRepository.findById(cartItemId).orElseThrow(
                () -> new NotFoundException("CartItem not found"));

        if (!cart.getItems().contains(cartItem)) {
            throw new ConflictException("CartItem does not belong to the user");
        }

        cart.setTotal(cart.getTotal() - cartItem.getSubtotal());
        Set<CartItem> items = new HashSet<>();
        cart.getItems().forEach(item -> {
            if (!Objects.equals(item.getId(), cartItemId)) {
                items.add(item);
            }
        });
        cart.setItems(items);

        CartDTO savedCart = cartRepository.save(cart).toCartDTO();
        setImagePathForCartItem(savedCart);
        return savedCart;
    }

    @Override
    public void deleteByCartId(Long cartId) {
        Optional<Cart> cart = cartRepository.findById(cartId);

        // Delete cart item in cart
        cartItemRepository.deleteByCartId(cartId);

        // Update cart
        if (cart.isPresent()) {
            Cart cartById = cart.get();
            cartById.setTotal(0);
            cartRepository.save(cartById);
        }
    }

    private void setImagePathForCartItem(CartDTO cartDTO) {
        cartDTO.getItems().forEach(item -> {
            String dir = "product-images/" + item.getProductID();
            item.setProductImagePath(awsS3Service.getImagePath(dir, item.getProductImage()));
        });
    }
}
