package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import com.store.ecommerce.entity.Product;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
@RequiredArgsConstructor
public class ProductRepositoryTests {
    private final ProductRepository productRepository;
    private final TestEntityManager testEntityManager;

    @Test
    public void testCreateProduct() {
        Brand brand = testEntityManager.find(Brand.class, 5);
        Category category = testEntityManager.find(Category.class, 1);

        Product product = new Product();
        product.setName("Macbook M1 Pro");
        product.setSummary("Summary for Macbook M1 Pro");
        product.setDescription("Full description for Macbook M1 Pro");

        product.setBrand(brand);
        product.setCategory(category);

        product.setCost(600);
        product.setPrice(678);
        product.setEnabled(true);
        product.setInStock(true);

        product.setCreatedTime(new Date());
        product.setUpdatedTime(new Date());

        Product savedProduct = productRepository.save(product);

        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getId()).isGreaterThan(0);
    }
}
