package com.store.ecommerce.repository;

import com.store.ecommerce.entity.*;
import com.store.ecommerce.enums.AuthenticationType;
import com.store.ecommerce.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("CartItemRepository Integration Tests")
class CartItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartItemRepository cartItemRepository;

    private User user1;
    private User user2;
    private Cart cart1;
    private Cart cart2;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        // Persist users
        user1 = persistUser("john@example.com", "John", "Doe");
        user2 = persistUser("jane@example.com", "Jane", "Smith");

        // Persist category and brand for products
        Category electronics = persistCategory("Electronics");
        Brand apple = persistBrand("Apple");

        // Persist products
        product1 = persistProduct("iPhone 15", electronics, apple);
        product2 = persistProduct("MacBook Pro", electronics, apple);
        product3 = persistProduct("AirPods", electronics, apple);

        // Persist carts
        cart1 = persistCart(user1);
        cart2 = persistCart(user2);

        // Cart 1 items (John's cart — 3 items)
        persistCartItem(cart1, product1, 2);
        persistCartItem(cart1, product2, 1);
        persistCartItem(cart1, product3, 3);

        // Cart 2 items (Jane's cart — 1 item)
        persistCartItem(cart2, product1, 1);
    }

    // ======================== DELETE BY CART ID ========================

    @Test
    @DisplayName("Should delete all cart items by cart id")
    void deleteByCartId_Success() {
        // Arrange — John's cart has 3 items
        List<CartItem> itemsBefore = findCartItemsByCartId(cart1.getId());
        assertThat(itemsBefore).hasSize(3);

        // Act
        cartItemRepository.deleteByCartId(cart1.getId());

        // Assert
        entityManager.flush();
        entityManager.clear();

        List<CartItem> itemsAfter = findCartItemsByCartId(cart1.getId());
        assertThat(itemsAfter).isEmpty();
    }

    @Test
    @DisplayName("Should not affect other cart items when deleting by cart id")
    void deleteByCartId_OtherCartUnaffected() {
        // Arrange — Jane's cart has 1 item
        List<CartItem> janeItemsBefore = findCartItemsByCartId(cart2.getId());
        assertThat(janeItemsBefore).hasSize(1);

        // Act — delete John's cart items
        cartItemRepository.deleteByCartId(cart1.getId());

        // Assert — Jane's cart items should remain
        entityManager.flush();
        entityManager.clear();

        List<CartItem> janeItemsAfter = findCartItemsByCartId(cart2.getId());
        assertThat(janeItemsAfter).hasSize(1);
    }

    @Test
    @DisplayName("Should handle deleting items from empty cart gracefully")
    void deleteByCartId_EmptyCart() {
        // Arrange — create an empty cart (no items)
        User user3 = persistUser("bob@example.com", "Bob", "Wilson");
        Cart emptyCart = persistCart(user3);

        // Act — should not throw
        cartItemRepository.deleteByCartId(emptyCart.getId());

        // Assert
        entityManager.flush();
        assertThat(cartItemRepository.count()).isEqualTo(4); // original 4 items unchanged
    }

    @Test
    @DisplayName("Should handle deleting with non-existent cart id")
    void deleteByCartId_NonExistentCartId() {
        // Arrange
        long countBefore = cartItemRepository.count();

        // Act — should not throw
        cartItemRepository.deleteByCartId(99999L);

        // Assert — nothing deleted
        entityManager.flush();
        assertThat(cartItemRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("Should delete only targeted cart items, not all")
    void deleteByCartId_PartialDelete() {
        // Arrange — 4 total items (3 in cart1, 1 in cart2)
        assertThat(cartItemRepository.count()).isEqualTo(4);

        // Act — delete cart1 items only
        cartItemRepository.deleteByCartId(cart1.getId());

        // Assert — only 1 item remaining (cart2)
        entityManager.flush();
        entityManager.clear();

        assertThat(cartItemRepository.count()).isEqualTo(1);
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve cart item")
    void save_AndFindById() {
        // Arrange
        CartItem item = new CartItem();
        item.setCart(cart1);
        item.setProduct(product1);
        item.setQuantity(5);

        // Act
        CartItem saved = cartItemRepository.save(item);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should delete single cart item by id")
    void deleteById_Success() {
        // Arrange
        List<CartItem> items = findCartItemsByCartId(cart1.getId());
        Long itemId = items.get(0).getId();

        // Act
        cartItemRepository.deleteById(itemId);

        // Assert
        assertThat(cartItemRepository.findById(itemId)).isEmpty();
    }

    @Test
    @DisplayName("Should count cart items correctly")
    void count_Success() {
        // 3 from cart1 + 1 from cart2 = 4
        assertThat(cartItemRepository.count()).isEqualTo(4);
    }

    // ======================== HELPER METHODS ========================

    private User persistUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setAuthenticationType(AuthenticationType.DATABASE);
        user.setAddressLine1("123 Main St");
        user.setCity("New York");
        user.setState("NY");
        user.setPostalCode("10001");
        user.setCountry("US");
        user.setPhoneNumber("555-1234");
        user.setBirthOfDate(DateUtil.toDateTime(LocalDate.of(1990, 1, 1)));
        return entityManager.persistAndFlush(user);
    }

    private Category persistCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(name + " category description");
        category.setImage(name.toLowerCase().replace(" ", "-") + ".png");
        category.setEnabled(true);
        return entityManager.persistAndFlush(category);
    }

    private Brand persistBrand(String name) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setLogo(name.toLowerCase().replace(" ", "-") + "-logo.png");
        return entityManager.persistAndFlush(brand);
    }

    private Product persistProduct(String name, Category category, Brand brand) {
        Product product = new Product();
        product.setName(name);
        product.setAlias(name.toLowerCase().replace(" ", "-"));
        product.setCategory(category);
        product.setBrand(brand);
        product.setEnabled(true);
        product.setAverageRating(4.0f);
        product.setCost(500.0f);
        product.setPrice(699.0f);
        product.setDescription(name + " - full product description for testing");
        product.setSummary(name + " - short summary");
        product.setMainImage(name.toLowerCase().replace(" ", "-") + "-main.png");
        product.setDiscountPercent(0.0f);
        product.setReviewCount(0);
        product.setInStock(true);
        product.setLength(10.0f);
        product.setWidth(5.0f);
        product.setHeight(2.0f);
        product.setWeight(0.5f);
        return entityManager.persistAndFlush(product);
    }

    private Cart persistCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setTotal(0.0f);
        return entityManager.persistAndFlush(cart);
    }

    private CartItem persistCartItem(Cart cart, Product product, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        return entityManager.persistAndFlush(item);
    }

    private List<CartItem> findCartItemsByCartId(Long cartId) {
        return entityManager.getEntityManager()
                .createQuery("SELECT c FROM CartItem c WHERE c.cart.id = :cartId", CartItem.class)
                .setParameter("cartId", cartId)
                .getResultList();
    }
}