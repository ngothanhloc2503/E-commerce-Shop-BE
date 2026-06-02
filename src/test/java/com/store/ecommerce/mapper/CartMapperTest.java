package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.CartDTO;
import com.store.ecommerce.dto.CartItemDTO;
import com.store.ecommerce.entity.Cart;
import com.store.ecommerce.entity.CartItem;
import com.store.ecommerce.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CartMapperTest {

    private CartMapper cartMapper;

    @BeforeEach
    void setUp() {
        cartMapper = Mappers.getMapper(CartMapper.class);
    }

    @Test
    void testToCartDTO_AllFieldsMapped() {
        // Given
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setTotal(150.0F);

        Set<CartItem> items = new HashSet<>();

        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setAlias("test-product");
        product.setMainImage("product.jpg");
        product.setPrice(50.0f);

        CartItem item = new CartItem();
        item.setId(1L);
        item.setQuantity(3);
        item.setProduct(product);
        items.add(item);

        cart.setItems(items);

        // When
        CartDTO cartDTO = cartMapper.toCartDTO(cart);

        // Then
        assertNotNull(cartDTO);
        assertEquals(1L, cartDTO.getId());
        assertEquals(150.0F, cartDTO.getTotal());
        assertNotNull(cartDTO.getItems());
        assertEquals(1, cartDTO.getItems().size());

        CartItemDTO itemDTO = cartDTO.getItems().get(0);
        assertEquals(1L, itemDTO.getId());
        assertEquals(3, itemDTO.getQuantity());

        assertEquals(1L, itemDTO.getProductID());
        assertEquals("Test Product", itemDTO.getProductName());
        assertEquals("product.jpg", itemDTO.getProductImage());
        assertEquals(50.0f, itemDTO.getProductPrice());

        assertEquals(150.0, itemDTO.getSubtotal());
    }

    @Test
    void testToCartDTO_NullInput() {
        CartDTO cartDTO = cartMapper.toCartDTO(null);
        assertNull(cartDTO);
    }

    @Test
    void testToCartDTO_NullItems() {
        // Given
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setTotal(0.0F);
        cart.setItems(null);

        // When
        CartDTO cartDTO = cartMapper.toCartDTO(cart);

        // Then
        assertNotNull(cartDTO);
        assertEquals(1L, cartDTO.getId());

        assertNotNull(cartDTO.getItems());
        assertTrue(cartDTO.getItems().isEmpty());
    }

    @Test
    void testToCartItemDTO() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Widget");
        product.setMainImage("widget.png");
        product.setPrice(25.0f);

        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setQuantity(2);
        cartItem.setProduct(product);

        CartItemDTO itemDTO = cartMapper.toCartItemDTO(cartItem);

        assertNotNull(itemDTO);
        assertEquals(1L, itemDTO.getId());
        assertEquals(2, itemDTO.getQuantity());
        assertEquals(1L, itemDTO.getProductID());
        assertEquals("Widget", itemDTO.getProductName());
        assertEquals("widget.png", itemDTO.getProductImage());
        assertEquals(25.0f, itemDTO.getProductPrice());
        assertEquals(50.0, itemDTO.getSubtotal());
    }

    @Test
    void testToCartItem_FromCartItemDTO() {
        CartItemDTO itemDTO = new CartItemDTO();
        itemDTO.setId(1L);
        itemDTO.setQuantity(2);
        itemDTO.setProductID(1L);

        CartItem cartItem = cartMapper.toCartItem(itemDTO);

        assertNotNull(cartItem);
        assertEquals(2, cartItem.getQuantity());
        assertNull(cartItem.getId());
        assertNull(cartItem.getCart());
    }

    @Test
    void testCartItemsToCartItemDTOs_NullInput() {
        List<CartItemDTO> result = cartMapper.toCartItemDTOs(null);
        assertNull(result);
    }

    @Test
    void testCartItemsToCartItemDTOs_EmptySet() {
        Set<CartItem> items = new HashSet<>();
        List<CartItemDTO> result = cartMapper.toCartItemDTOs(items);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testToCartDTO_MultipleItems() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setTotal(200.0F);

        Set<CartItem> items = new HashSet<>();

        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setPrice(50.0f);
        CartItem item1 = new CartItem();
        item1.setId(1L);
        item1.setQuantity(2);
        item1.setProduct(product1);
        items.add(item1);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(100.0f);
        CartItem item2 = new CartItem();
        item2.setId(2L);
        item2.setQuantity(1);
        item2.setProduct(product2);
        items.add(item2);

        cart.setItems(items);

        CartDTO cartDTO = cartMapper.toCartDTO(cart);

        assertNotNull(cartDTO);
        assertEquals(1L, cartDTO.getId());
        assertEquals(200.0F, cartDTO.getTotal());
        assertNotNull(cartDTO.getItems());
        assertEquals(2, cartDTO.getItems().size());
    }

    @Test
    void testToCartItemDTO_NullProduct() {
        // Given
        CartItem cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setQuantity(1);
        cartItem.setProduct(null);

        // When
        CartItemDTO itemDTO = cartMapper.toCartItemDTO(cartItem);

        // Then
        assertNotNull(itemDTO);
        assertEquals(1L, itemDTO.getId());
        assertEquals(1, itemDTO.getQuantity());

        assertNull(itemDTO.getProductID());
        assertNull(itemDTO.getProductName());
        assertNull(itemDTO.getProductImage());
        assertEquals(0.0f, itemDTO.getProductPrice());

        assertEquals(0.0, itemDTO.getSubtotal());
    }

    @Test
    void testToCartDTO_EmptyItems() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setTotal(0.0F);
        cart.setItems(new HashSet<>());

        CartDTO cartDTO = cartMapper.toCartDTO(cart);

        assertNotNull(cartDTO);
        assertEquals(1L, cartDTO.getId());
        assertNotNull(cartDTO.getItems());
        assertTrue(cartDTO.getItems().isEmpty());
    }
}