package com.store.ecommerce.service;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.entity.*;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.CartItemRepository;
import com.store.ecommerce.repository.CartRepository;
import com.store.ecommerce.repository.ProductRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.impl.AWSS3ServiceImpl;
import com.store.ecommerce.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl Unit Tests")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AWSS3ServiceImpl awsS3Service;

    @Mock
    private AddressService addressService;

    @Mock
    private ShippingRateService shippingRateService;

    @InjectMocks
    private CartServiceImpl cartService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;
    private CartItem testCartItem;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2);

        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);
        testCart.setTotal(200.0F);
        Set<CartItem> items = new HashSet<>();
        items.add(testCartItem);
        testCart.setItems(items);
        testUser.setCart(testCart);

        testAddress = new Address();
        testAddress.setId(1L);
        testAddress.setUser(testUser);
        testAddress.setCountry("USA");
        testAddress.setCity("New York");
        testAddress.setPostalCode("10001");
        testAddress.setPhoneNumber("123456789");
        testAddress.setDefaultForShipping(true);
    }

    // ======================== FIND BY USER EMAIL ========================

    @Nested
    @DisplayName("findByUserEmail() Tests")
    class FindByUserEmailTests {

        @Test
        @DisplayName("Should return cart DTO when user and cart exist")
        void findByUserEmail_Success() throws NotFoundException {
            // Arrange
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(cartRepository.findByUserEmail(testUser.getEmail())).thenReturn(testCart);
            when(addressService.getDefaultAddress(testUser.getEmail())).thenReturn(testAddress);
            when(shippingRateService.isShippingSupported(testAddress)).thenReturn(true);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.jpg");
            // Act
            CartDTO result = cartService.findByUserEmail(testUser.getEmail());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(200.0f);
            assertThat(result.isShippingSupported()).isTrue();
            verify(userRepository).findByEmail(testUser.getEmail());
            verify(cartRepository).findByUserEmail(testUser.getEmail());
            verify(addressService).getDefaultAddress(testUser.getEmail());
            verify(shippingRateService).isShippingSupported(testAddress);
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void findByUserEmail_ThrowsNotFoundException_WhenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.findByUserEmail("notfound@example.com"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not found any user with email");
        }
    }

    // ======================== ADD ITEM TO CART ========================

    @Nested
    @DisplayName("addItemToCart() Tests")
    class AddItemToCartTests {

        @Test
        @DisplayName("Should create new cart and add item when user has no cart")
        void addItemToCart_NewCart_Success() throws NotFoundException, ConflictException {
            // Arrange
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
                Cart savedCart = invocation.getArgument(0);
                savedCart.setId(1L);
                return savedCart;
            });

            // Act
            CartDTO result = cartService.addItemToCart(testUser.getEmail(), testProduct.getId(), 1);

            // Assert
            assertThat(result).isNotNull();
            verify(userRepository).findByEmail(testUser.getEmail());
            verify(productRepository).findById(testProduct.getId());
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should add new product to existing cart")
        void addItemToCart_ExistingCart_AddNewProduct_Success() throws NotFoundException, ConflictException {
            // Arrange
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(productRepository.findById(2L)).thenReturn(Optional.of(testProduct));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.jpg");
            // Act
            CartDTO result = cartService.addItemToCart(testUser.getEmail(), 2L, 1);

            // Assert
            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should increase quantity when adding existing product to cart")
        void addItemToCart_ExistingProduct_IncreaseQuantity_Success() throws NotFoundException, ConflictException {
            // Arrange
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.jpg");
            // Act
            CartDTO result = cartService.addItemToCart(testUser.getEmail(), 1L, 1);

            // Assert
            assertThat(result).isNotNull();
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void addItemToCart_ThrowsNotFoundException_WhenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.addItemToCart("notfound@example.com", 1L, 1))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not found any user with email");
        }

        @Test
        @DisplayName("Should throw NotFoundException when product not found")
        void addItemToCart_ThrowsNotFoundException_WhenProductNotFound() {
            // Arrange
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.addItemToCart(testUser.getEmail(), 999L, 1))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not found any user with id");
        }

        @Test
        @DisplayName("Should throw ConflictException when quantity is negative")
        void addItemToCart_ThrowsConflictException_WhenNegativeQuantity() {
            // Arrange
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(productRepository.findById(2L)).thenReturn(Optional.of(testProduct));

            // Act & Assert
            assertThatThrownBy(() -> cartService.addItemToCart(testUser.getEmail(), 2L, -1))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Quantity must be greater than 0.");
        }
    }

    // ======================== DELETE CART ITEM ========================

    @Nested
    @DisplayName("deleteCartItem() Tests")
    class DeleteCartItemTests {

        @Test
        @DisplayName("Should delete cart item and return updated cart")
        void deleteCartItem_Success() throws NotFoundException, ConflictException {
            // Arrange
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(cartItemRepository.findById(testCartItem.getId())).thenReturn(Optional.of(testCartItem));
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            CartDTO result = cartService.deleteCartItem(testUser.getEmail(), testCartItem.getId());

            // Assert
            assertThat(result).isNotNull();
            verify(userRepository).findByEmail(testUser.getEmail());
            verify(cartItemRepository).findById(testCartItem.getId());
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should throw NotFoundException when user not found")
        void deleteCartItem_ThrowsNotFoundException_WhenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.deleteCartItem("notfound@example.com", 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any user with email");
        }

        @Test
        @DisplayName("Should throw NotFoundException when user has no cart")
        void deleteCartItem_ThrowsNotFoundException_WhenCartNotFound() {
            // Arrange
            User userWithoutCart = new User();
            userWithoutCart.setId(1L);
            userWithoutCart.setEmail("test@example.com");
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(userWithoutCart));

            // Act & Assert
            assertThatThrownBy(() -> cartService.deleteCartItem(testUser.getEmail(), 1L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Cart not found");
        }

        @Test
        @DisplayName("Should throw NotFoundException when cart item not found")
        void deleteCartItem_ThrowsNotFoundException_WhenCartItemNotFound() {
            // Arrange
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(cartItemRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.deleteCartItem(testUser.getEmail(), 999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("CartItem not found");
        }

        @Test
        @DisplayName("Should throw ConflictException when cart item does not belong to user")
        void deleteCartItem_ThrowsConflictException_WhenCartItemDoesNotBelongToUser() {
            // Arrange
            CartItem otherCartItem = new CartItem();
            otherCartItem.setId(999L);
            when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
            when(cartItemRepository.findById(999L)).thenReturn(Optional.of(otherCartItem));

            // Act & Assert
            assertThatThrownBy(() -> cartService.deleteCartItem(testUser.getEmail(), 999L))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("CartItem does not belong to the user");
        }
    }

    // ======================== DELETE BY CART ID ========================

    @Nested
    @DisplayName("deleteByCartId() Tests")
    class DeleteByCartIdTests {

        @Test
        @DisplayName("Should delete all cart items and reset cart total")
        void deleteByCartId_Success() {
            // Arrange
            when(cartRepository.findById(testCart.getId())).thenReturn(Optional.of(testCart));
            doNothing().when(cartItemRepository).deleteByCartId(testCart.getId());
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            cartService.deleteByCartId(testCart.getId());

            // Assert
            verify(cartRepository).findById(testCart.getId());
            verify(cartItemRepository).deleteByCartId(testCart.getId());
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should delete cart items but not save cart when cart not found")
        void deleteByCartId_CartNotFound_DoesNothing() {
            // Arrange
            when(cartRepository.findById(999L)).thenReturn(Optional.empty());
            doNothing().when(cartItemRepository).deleteByCartId(999L);

            // Act
            cartService.deleteByCartId(999L);

            // Assert
            verify(cartItemRepository).deleteByCartId(999L);
            verify(cartRepository, never()).save(any(Cart.class));
        }
    }
}