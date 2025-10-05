package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Cart;
import com.store.ecommerce.entity.CartItem;
import com.store.ecommerce.entity.Product;
import com.store.ecommerce.entity.User;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
@RequiredArgsConstructor
public class CartRepositoryTests {
    private final CartRepository cartRepository;
    private final TestEntityManager testEntityManager;

    @Test
    public void createFirstCart() {
        Cart cart = new Cart();
        User user = testEntityManager.find(User.class, 2);
        Product product = testEntityManager.find(Product.class, 1);
        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cart.setUser(user);
        cart.addItem(cartItem);

        Cart saved = cartRepository.save(cart);

        assertThat(saved.getId()).isGreaterThan(0);
    }
}
