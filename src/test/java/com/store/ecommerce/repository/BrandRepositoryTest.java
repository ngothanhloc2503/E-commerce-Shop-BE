package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("BrandRepository Integration Tests")
class BrandRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BrandRepository brandRepository;

    private Category electronics;
    private Category mobile;
    private Category laptop;

    @BeforeEach
    void setUp() {
        // Persist categories first (they are needed for brand-category relationships)
        // Must set all NOT NULL fields: name, description, image
        electronics = persistCategory("Electronics", "Electronic devices and gadgets", "electronics.png", true);
        mobile = persistCategory("Mobile", "Smartphones and accessories", "mobile.png", true);
        laptop = persistCategory("Laptop", "Notebook and laptop computers", "laptop.png", true);
    }

    // ======================== SAVE / CREATE ========================

    @Test
    @DisplayName("Should save brand successfully")
    void testCreateBrand() {
        // Arrange
        Brand brand = createBrand("Apple");

        // Act
        Brand savedBrand = brandRepository.save(brand);

        // Assert
        assertThat(savedBrand).isNotNull();
        assertThat(savedBrand.getId()).isGreaterThan(0);
        assertThat(savedBrand.getName()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("Should save brand with categories")
    void testCreateBrand_WithCategories() {
        // Arrange
        Brand brand = createBrand("Apple");
        brand.addCategory(electronics);
        brand.addCategory(mobile);

        // Act
        Brand savedBrand = brandRepository.save(brand);

        // Assert
        assertThat(savedBrand).isNotNull();
        assertThat(savedBrand.getId()).isGreaterThan(0);
        assertThat(savedBrand.getCategories()).isNotEmpty();
        assertThat(savedBrand.getCategories()).contains(electronics, mobile);
    }

    // ======================== FIND BY NAME ========================

    @Test
    @DisplayName("Should find brand by name when exists")
    void testFindByName_Found() {
        // Arrange
        entityManager.persistAndFlush(createBrand("Samsung"));

        // Act
        Optional<Brand> found = brandRepository.findByName("Samsung");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Samsung");
    }

    @Test
    @DisplayName("Should return empty when brand name not found")
    void testFindByName_NotFound() {
        // Act
        Optional<Brand> found = brandRepository.findByName("NonExistentBrand");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find brand by name case-sensitive")
    void testFindByName_CaseSensitive() {
        // Arrange
        entityManager.persistAndFlush(createBrand("Samsung"));

        // Act
        Optional<Brand> found = brandRepository.findByName("samsung"); // lowercase

        // Assert
        assertThat(found).isEmpty(); // JPA default is case-sensitive
    }

    // ======================== FIND ALL BY CATEGORIES ========================

    @Test
    @DisplayName("Should find all brands by category")
    void testFindAllByCategories_Found() {
        // Arrange
        Brand apple = createBrand("Apple");
        apple.addCategory(mobile);
        entityManager.persistAndFlush(apple);

        Brand samsung = createBrand("Samsung");
        samsung.addCategory(mobile);
        entityManager.persistAndFlush(samsung);

        Brand dell = createBrand("Dell");
        dell.addCategory(laptop); // different category
        entityManager.persistAndFlush(dell);

        // Act
        List<Brand> mobileBrands = brandRepository.findAllByCategories(mobile);

        // Assert
        assertThat(mobileBrands).hasSize(2);
        assertThat(mobileBrands).extracting(Brand::getName)
                .containsExactlyInAnyOrder("Apple", "Samsung");
    }

    @Test
    @DisplayName("Should return empty list when no brand has the category")
    void testFindAllByCategories_NoMatch() {
        // Arrange
        Brand brand = createBrand("Apple");
        brand.addCategory(mobile);
        entityManager.persistAndFlush(brand);

        // Act
        List<Brand> laptopBrands = brandRepository.findAllByCategories(laptop);

        // Assert
        assertThat(laptopBrands).isEmpty();
    }

    @Test
    @DisplayName("Should find brand that belongs to multiple categories")
    void testFindAllByCategories_BrandWithMultipleCategories() {
        // Arrange
        Brand apple = createBrand("Apple");
        apple.addCategory(mobile);
        apple.addCategory(laptop);
        entityManager.persistAndFlush(apple);

        // Act
        List<Brand> mobileBrands = brandRepository.findAllByCategories(mobile);
        List<Brand> laptopBrands = brandRepository.findAllByCategories(laptop);

        // Assert - Apple appears in both category lists
        assertThat(mobileBrands).hasSize(1);
        assertThat(mobileBrands.get(0).getName()).isEqualTo("Apple");
        assertThat(laptopBrands).hasSize(1);
        assertThat(laptopBrands.get(0).getName()).isEqualTo("Apple");
    }

    // ======================== FIND ALL (PAGEABLE) ========================

    @Test
    @DisplayName("Should return paginated brands with categories fetched")
    void testFindAll_Pageable() {
        // Arrange
        createMultipleBrands(5);

        PageRequest pageable = PageRequest.of(0, 3);

        // Act
        Page<Brand> page = brandRepository.findAll(pageable);

        // Assert
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should return second page of brands")
    void testFindAll_Pageable_SecondPage() {
        // Arrange
        createMultipleBrands(5);

        PageRequest pageable = PageRequest.of(1, 3); // page 2, size 3

        // Act
        Page<Brand> page = brandRepository.findAll(pageable);

        // Assert
        assertThat(page.getContent()).hasSize(2); // 5 total - 3 from page 1
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.isLast()).isTrue();
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("Should return empty page when page number exceeds total")
    void testFindAll_Pageable_ExceedsTotal() {
        // Arrange
        createMultipleBrands(2);

        PageRequest pageable = PageRequest.of(5, 3); // page 6, only 2 brands exist

        // Act
        Page<Brand> page = brandRepository.findAll(pageable);

        // Assert
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    // ======================== FIND ALL BY KEYWORD (PAGEABLE) ========================

    @Test
    @DisplayName("Should find brands by keyword with pagination")
    void testFindAll_KeywordPageable_Found() {
        // Arrange
        Brand apple = createBrand("Apple");
        apple.addCategory(mobile);
        entityManager.persistAndFlush(apple);

        Brand samsung = createBrand("Samsung");
        samsung.addCategory(mobile);
        entityManager.persistAndFlush(samsung);

        Brand dell = createBrand("Dell");
        dell.addCategory(laptop);
        entityManager.persistAndFlush(dell);

        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Brand> result = brandRepository.findAll("Sam", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Samsung");
    }

    @Test
    @DisplayName("Should search brand by id as keyword")
    void testFindAll_KeywordPageable_SearchById() {
        // Arrange
        Brand savedApple = entityManager.persistAndFlush(createBrand("Apple"));
        entityManager.persistAndFlush(createBrand("Samsung"));

        PageRequest pageable = PageRequest.of(0, 10);

        // Act - search by id (CONCAT includes id)
        String keyword = String.valueOf(savedApple.getId());
        Page<Brand> result = brandRepository.findAll(keyword, pageable);

        // Assert
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent().stream()
                .anyMatch(b -> b.getName().equals("Apple"))).isTrue();
    }

    @Test
    @DisplayName("Should return empty page when keyword matches nothing")
    void testFindAll_KeywordPageable_NoMatch() {
        // Arrange
        entityManager.persistAndFlush(createBrand("Apple"));

        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Brand> result = brandRepository.findAll("XYZ", pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should find brands with partial keyword match")
    void testFindAll_KeywordPageable_PartialMatch() {
        // Arrange
        entityManager.persistAndFlush(createBrand("Acer"));
        entityManager.persistAndFlush(createBrand("Apple"));
        entityManager.persistAndFlush(createBrand("Samsung"));

        PageRequest pageable = PageRequest.of(0, 10);

        // Act - "Ap" should match "Apple"
        Page<Brand> result = brandRepository.findAll("Ap", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Apple");
    }

    // ======================== FIND ALL BY KEYWORD (SORT) ========================

    @Test
    @DisplayName("Should find brands by keyword with sort")
    void testFindAll_KeywordSort_Found() {
        // Arrange
        entityManager.persistAndFlush(createBrand("Apple"));
        entityManager.persistAndFlush(createBrand("Acer"));
        entityManager.persistAndFlush(createBrand("Samsung"));

        Sort sort = Sort.by("name").ascending();

        // Act - "A" matches "Apple" and "Acer"
        List<Brand> result = brandRepository.findAll("A", sort);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Brand::getName)
                .containsExactly("Acer", "Apple"); // sorted ascending
    }

    @Test
    @DisplayName("Should find brands by keyword with descending sort")
    void testFindAll_KeywordSort_Descending() {
        // Arrange
        entityManager.persistAndFlush(createBrand("Apple"));
        entityManager.persistAndFlush(createBrand("Acer"));
        entityManager.persistAndFlush(createBrand("Samsung"));

        Sort sort = Sort.by("name").descending();

        // Act
        List<Brand> result = brandRepository.findAll("A", sort);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Brand::getName)
                .containsExactly("Apple", "Acer"); // sorted descending
    }

    @Test
    @DisplayName("Should return empty list when keyword matches nothing with sort")
    void testFindAll_KeywordSort_NoMatch() {
        // Arrange
        entityManager.persistAndFlush(createBrand("Apple"));

        Sort sort = Sort.by("name").ascending();

        // Act
        List<Brand> result = brandRepository.findAll("XYZ", sort);

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== FIND ALL (NO PARAM) ========================

    @Test
    @DisplayName("Should return all brands")
    void testFindAll_NoParam() {
        // Arrange
        createMultipleBrands(3);

        // Act
        List<Brand> brands = brandRepository.findAll();

        // Assert
        assertThat(brands).hasSize(3);
    }

    @Test
    @DisplayName("Should return empty list when no brands exist")
    void testFindAll_NoParam_Empty() {
        // Act
        List<Brand> brands = brandRepository.findAll();

        // Assert
        assertThat(brands).isEmpty();
    }

    // ======================== DELETE ========================

    @Test
    @DisplayName("Should delete brand by id")
    void testDeleteById() {
        // Arrange
        Brand savedBrand = entityManager.persistAndFlush(createBrand("ToDelete"));
        Long brandId = savedBrand.getId();

        // Act
        brandRepository.deleteById(brandId);

        // Assert
        Optional<Brand> found = brandRepository.findById(brandId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should count brands correctly")
    void testCount() {
        // Arrange
        createMultipleBrands(3);

        // Act
        long count = brandRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    // ======================== HELPER METHODS ========================

    private Brand createBrand(String name) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setLogo(name.toLowerCase().replace(" ", "-") + "-logo.png");
        return brand;
    }

    private void createMultipleBrands(int count) {
        for (int i = 1; i <= count; i++) {
            Brand brand = createBrand("Brand" + i);
            brand.addCategory(electronics);
            entityManager.persistAndFlush(brand);
        }
    }

    private Category persistCategory(String name, String description, String image, boolean enabled) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setImage(image);
        category.setEnabled(enabled);
        return entityManager.persistAndFlush(category);
    }
}