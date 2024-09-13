package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
public class CategoryRepositoryTests {
    @Autowired
    private CategoryRepository repository;

    @Test
    public void testCreateFirstCategory() {
        Category category = new Category();
        category.setName("Laptop");
        category.setEnabled(true);

        Category savedCategory = repository.save(category);

        assertThat(savedCategory.getId()).isGreaterThan(0);
    }

    @Test
    public void testGetListAllCategory() {
        List<Category> listCategories = repository.findAll();

        assertThat(listCategories.size()).isGreaterThan(0);

        for (Category category : listCategories) {
            System.out.println(category);
        }
    }
}
