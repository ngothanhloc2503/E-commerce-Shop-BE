package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
public class BrandRepositoryTests {

    @Autowired
    private BrandRepository brandRepository;

    @Test
    public void testCreateFirstBrand() {
        Brand brand = new Brand();
        brand.setName("Apple");
        brand.addCategory(new Category(1L));

        Brand savedBrand = brandRepository.save(brand);
        assertThat(savedBrand).isNotNull();
        assertThat(savedBrand.getId()).isGreaterThan(0);
    }
}
