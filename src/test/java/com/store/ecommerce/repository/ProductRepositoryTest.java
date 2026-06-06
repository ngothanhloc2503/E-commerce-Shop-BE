package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import com.store.ecommerce.entity.Product;
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
@DisplayName("ProductRepository Integration Tests")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Category electronics;
    private Category mobile;
    private Brand apple;
    private Brand samsung;
    private Brand dell;

    @BeforeEach
    void setUp() {
        // Persist categories
        electronics = persistCategory("Electronics");
        mobile = persistCategory("Mobile");

        // Persist brands
        apple = persistBrand("Apple");
        samsung = persistBrand("Samsung");
        dell = persistBrand("Dell");

        // Electronics products (enabled)
        persistProduct("iPhone 15", "Latest iPhone", "Apple iPhone 15 with A17 chip",
                "iphone-15", electronics, apple, 4.5f, true);
        persistProduct("Samsung Galaxy S24", "Samsung flagship", "Galaxy S24 with AI features",
                "samsung-galaxy-s24", electronics, samsung, 4.3f, true);
        persistProduct("Dell XPS 15", "Dell laptop", "Dell XPS 15 with OLED display",
                "dell-xps-15", electronics, dell, 4.7f, true);

        // Mobile products (enabled)
        persistProduct("iPhone 15 Pro", "Pro iPhone", "iPhone 15 Pro with titanium",
                "iphone-15-pro", mobile, apple, 4.8f, true);

        // Disabled products
        persistProduct("Old Phone", "Discontinued phone", "No longer available",
                "old-phone", mobile, samsung, 2.0f, false);
        persistProduct("Legacy Laptop", "Old laptop model", "Discontinued laptop",
                "legacy-laptop", electronics, dell, 1.5f, false);

        persistProduct("Refurbished Tablet", "Refurbished tablet", "Certified refurbished tablet",
                "refurbished-tablet", mobile, samsung, 3.0f, false);
    }

    // ======================== FIND BY NAME ========================

    @Test
    @DisplayName("Should find product by name when exists")
    void findByName_Found() {
        // Act
        Optional<Product> result = productRepository.findByName("iPhone 15");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("iPhone 15");
    }

    @Test
    @DisplayName("Should return empty when product name not found")
    void findByName_NotFound() {
        // Act
        Optional<Product> result = productRepository.findByName("NonExistent Product");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find product by name case-sensitive")
    void findByName_CaseSensitive() {
        // Act
        Optional<Product> result = productRepository.findByName("iphone 15");

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== UPDATE ENABLED STATUS ========================

    @Test
    @DisplayName("Should disable an enabled product")
    void updateEnabledStatus_Disable() {
        // Arrange
        Product product = findProductByName("iPhone 15");
        assertThat(product.isEnabled()).isTrue();

        // Act
        productRepository.updateEnabledStatus(product.getId(), false);

        // Assert
        entityManager.flush();
        entityManager.clear();

        Product updated = entityManager.find(Product.class, product.getId());
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should enable a disabled product")
    void updateEnabledStatus_Enable() {
        // Arrange
        Product product = findProductByName("Old Phone");
        assertThat(product.isEnabled()).isFalse();

        // Act
        productRepository.updateEnabledStatus(product.getId(), true);

        // Assert
        entityManager.flush();
        entityManager.clear();

        Product updated = entityManager.find(Product.class, product.getId());
        assertThat(updated.isEnabled()).isTrue();
    }

    // ======================== FIND ALL (PAGEABLE) ========================

    @Test
    @DisplayName("Should return paginated products")
    void findAll_Pageable() {
        // Arrange — 7 products total
        PageRequest pageable = PageRequest.of(0, 3);

        // Act
        Page<Product> page = productRepository.findAll(pageable);

        // Assert
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should return last page of products")
    void findAll_Pageable_LastPage() {
        // Arrange
        PageRequest pageable = PageRequest.of(2, 3); // page 3, size 3 → 7 total

        // Act
        Page<Product> page = productRepository.findAll(pageable);

        // Assert
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.isLast()).isTrue();
    }

    // ======================== FIND ALL BY CATEGORY (LIST) ========================

    @Test
    @DisplayName("Should find all products by category ID")
    void findAllByCategory_Found() {
        // Act
        List<Product> result = productRepository.findByCategoryId(electronics.getId());

        // Assert — Electronics: iPhone 15, Galaxy S24, Dell XPS 15, Legacy Laptop = 4
        assertThat(result).hasSize(4);
        assertThat(result).allMatch(p -> p.getCategory().getId().equals(electronics.getId()));
    }

    @Test
    @DisplayName("Should find products by mobile category")
    void findAllByCategory_Mobile() {
        // Act
        List<Product> result = productRepository.findByCategoryId(mobile.getId());

        // Assert — Mobile: iPhone 15 Pro, Old Phone, Refurbished Tablet = 3
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Should return empty when category has no products")
    void findAllByCategory_NoProducts() {
        // Arrange
        Category emptyCat = persistCategory("Books");

        // Act
        List<Product> result = productRepository.findByCategoryId(emptyCat.getId());

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== FIND ALL BY KEYWORD (PAGEABLE) ========================

    @Test
    @DisplayName("Should find products by keyword matching name")
    void findAll_KeywordPageable_MatchName() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.searchByKeyword("iPhone", pageable);

        // Assert — matches "iPhone 15" and "iPhone 15 Pro"
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p -> p.getName().contains("iPhone"));
    }

    @Test
    @DisplayName("Should find products by keyword matching summary")
    void findAll_KeywordPageable_MatchSummary() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — "flagship" appears in Samsung Galaxy summary
        Page<Product> result = productRepository.searchByKeyword("flagship", pageable);

        // Assert
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).anyMatch(p -> p.getName().equals("Samsung Galaxy S24"));
    }

    @Test
    @DisplayName("Should find products by keyword matching description")
    void findAll_KeywordPageable_MatchDescription() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — "OLED" appears in Dell XPS description
        Page<Product> result = productRepository.searchByKeyword("OLED", pageable);

        // Assert
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).anyMatch(p -> p.getName().equals("Dell XPS 15"));
    }

    @Test
    @DisplayName("Should return empty page when keyword matches nothing")
    void findAll_KeywordPageable_NoMatch() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.searchByKeyword("XYZNonExistent", pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    // ======================== FIND ALL BY KEYWORD (SORT) ========================

    @Test
    @DisplayName("Should find products by keyword with sort ascending")
    void findAll_KeywordSort_Ascending() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act
        List<Product> result = productRepository.searchByKeyword("iPhone", sort);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getName)
                .containsExactly("iPhone 15", "iPhone 15 Pro");
    }

    @Test
    @DisplayName("Should find products by keyword with sort descending")
    void findAll_KeywordSort_Descending() {
        // Arrange
        Sort sort = Sort.by("name").descending();

        // Act
        List<Product> result = productRepository.searchByKeyword("iPhone", sort);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getName)
                .containsExactly("iPhone 15 Pro", "iPhone 15");
    }

    @Test
    @DisplayName("Should return empty list when keyword matches nothing with sort")
    void findAll_KeywordSort_NoMatch() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act
        List<Product> result = productRepository.searchByKeyword("XYZNonExistent", sort);

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== FIND ALL BY CATEGORY + KEYWORD + SORT ========================

    @Test
    @DisplayName("Should find products by category and keyword")
    void findAllByCategory_KeywordSort_Found() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act — search "iPhone" in Electronics
        List<Product> result = productRepository.searchByCategoryIdAndKeyword(
                electronics.getId(), "iPhone", sort);

        // Assert — only iPhone 15 is in Electronics
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("iPhone 15");
    }

    @Test
    @DisplayName("Should return empty when keyword does not match in category")
    void findAllByCategory_KeywordSort_NoKeywordMatch() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act — search "Dell" in Mobile category
        List<Product> result = productRepository.searchByCategoryIdAndKeyword(
                mobile.getId(), "Dell", sort);

        // Assert — Dell products are in Electronics, not Mobile
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should search by keyword in summary within category")
    void findAllByCategory_KeywordSort_MatchSummary() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act — "flagship" in Samsung summary, Electronics category
        List<Product> result = productRepository.searchByCategoryIdAndKeyword(
                electronics.getId(), "flagship", sort);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result).anyMatch(p -> p.getName().equals("Samsung Galaxy S24"));
    }

    @Test
    @DisplayName("Should search by keyword in description within category")
    void findAllByCategory_KeywordSort_MatchDescription() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act — "OLED" in Dell description, Electronics category
        List<Product> result = productRepository.searchByCategoryIdAndKeyword(
                electronics.getId(), "OLED", sort);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result).anyMatch(p -> p.getName().equals("Dell XPS 15"));
    }

    // ======================== FIND ALL BY CATEGORY (PAGEABLE) ========================

    @Test
    @DisplayName("Should find products by category with pagination")
    void findAllByCategory_Pageable() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 2);

        // Act
        Page<Product> result = productRepository.findByCategoryId(electronics.getId(), pageable);

        // Assert — 4 electronics products, page size 2
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return second page of category products")
    void findAllByCategory_Pageable_SecondPage() {
        // Arrange
        PageRequest pageable = PageRequest.of(1, 2);

        // Act
        Page<Product> result = productRepository.findByCategoryId(electronics.getId(), pageable);

        // Assert — 4 total - 2 from page 1
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.isLast()).isTrue();
    }

    // ======================== SEARCH BY CATEGORY (KEYWORD + PAGEABLE) ========================

    @Test
    @DisplayName("Should search products by category and keyword with pagination")
    void searchByCategory_Found() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.searchByCategoryIdAndKeyword(
                electronics.getId(), "iPhone", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("iPhone 15");
    }

    @Test
    @DisplayName("Should return empty page when no match in category")
    void searchByCategory_NoMatch() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — search "Dell" in Mobile
        Page<Product> result = productRepository.searchByCategoryIdAndKeyword(
                mobile.getId(), "Dell", pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    // ======================== FIND ALL (SORT - ENABLED ONLY) ========================

    @Test
    @DisplayName("Should find only enabled products with sort")
    void findAll_Sort_EnabledOnly() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act
        List<Product> result = productRepository.findAllByEnabledTrue(sort);

        // Assert — H2 sorts case-sensitive: uppercase before lowercase
        assertThat(result).hasSize(4);
        assertThat(result).allMatch(Product::isEnabled);
        assertThat(result).extracting(Product::getName)
                .containsExactly("Dell XPS 15", "Samsung Galaxy S24", "iPhone 15", "iPhone 15 Pro");
    }

    @Test
    @DisplayName("Should not include disabled products in enabled sort query")
    void findAll_Sort_NoDisabledProducts() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act
        List<Product> result = productRepository.findAllByEnabledTrue(sort);

        // Assert — Old Phone and Legacy Laptop should NOT appear
        assertThat(result).noneMatch(p -> p.getName().equals("Old Phone"));
        assertThat(result).noneMatch(p -> p.getName().equals("Legacy Laptop"));
    }

    // ======================== FIND BY ALIAS ========================

    @Test
    @DisplayName("Should find enabled product by alias")
    void findByAlias_Found() {
        // Act
        Optional<Product> result = productRepository.findByAliasAndEnabledTrue("iphone-15");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("iPhone 15");
        assertThat(result.get().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should not find disabled product by alias")
    void findByAlias_DisabledProduct() {
        // Act
        Optional<Product> result = productRepository.findByAliasAndEnabledTrue("old-phone");

        // Assert — disabled product should not be found
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when alias not found")
    void findByAlias_NotFound() {
        // Act
        Optional<Product> result = productRepository.findByAliasAndEnabledTrue("non-existent-alias");

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== FIND ALL ENABLED (ORDERED BY RATING) ========================

    @Test
    @DisplayName("Should find all enabled products ordered by rating descending")
    void findAllEnabled_OrderedByRating() {
        // Act
        Sort sort = Sort.by("averageRating").descending();
        List<Product> result = productRepository.findAllByEnabledTrue(sort);

        // Assert
        assertThat(result).hasSize(4);
        assertThat(result).allMatch(Product::isEnabled);
        assertThat(result).extracting(Product::getName)
                .containsExactly("iPhone 15 Pro", "Dell XPS 15", "iPhone 15", "Samsung Galaxy S24");
    }

    @Test
    @DisplayName("Should not include disabled products in enabled list")
    void findAllEnabled_NoDisabled() {
        // Act
        Sort sort = Sort.by("averageRating").descending();
        List<Product> result = productRepository.findAllByEnabledTrue(sort);

        // Assert
        assertThat(result).noneMatch(p -> !p.isEnabled());
    }

    // ======================== SEARCH PRODUCT (CUSTOMER - PAGEABLE) ========================

    @Test
    @DisplayName("Should search enabled products by keyword ignoring case")
    void searchProduct_Pageable_KeywordMatch() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — search "iphone" (lowercase) should match "iPhone 15" and "iPhone 15 Pro"
        Page<Product> result = productRepository.searchProduct("iphone", null, null, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(Product::isEnabled);
    }

    @Test
    @DisplayName("Should search products with minimum average rating filter")
    void searchProduct_Pageable_RatingFilter() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — rating >= 4.5 → iPhone 15 (4.5), Dell XPS (4.7), iPhone 15 Pro (4.8)
        Page<Product> result = productRepository.searchProduct("i", 4.5f, null, pageable);

        // Assert
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(p -> p.getAverageRating() >= 4.5f);
        assertThat(result.getContent()).allMatch(Product::isEnabled);
    }

    @Test
    @DisplayName("Should search products filtered by brand IDs")
    void searchProduct_Pageable_BrandFilter() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        List<Long> brandIds = List.of(apple.getId());

        // Act — search "iPhone" in Apple brand only
        Page<Product> result = productRepository.searchProduct("iPhone", null, brandIds, pageable);

        // Assert
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(p -> p.getBrand().getId().equals(apple.getId()));
    }

    @Test
    @DisplayName("Should search products with rating and brand filter combined")
    void searchProduct_Pageable_RatingAndBrandFilter() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);
        List<Long> brandIds = List.of(apple.getId());

        // Act — rating >= 4.5, Apple brand, keyword "i"
        Page<Product> result = productRepository.searchProduct("i", 4.5f, brandIds, pageable);

        // Assert — iPhone 15 (4.5, Apple) and iPhone 15 Pro (4.8, Apple)
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(p ->
                p.getAverageRating() >= 4.5f &&
                        p.getBrand().getId().equals(apple.getId()) &&
                        p.isEnabled()
        );
    }

    @Test
    @DisplayName("Should not include disabled products in customer search")
    void searchProduct_Pageable_NoDisabled() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.searchProduct("Phone", null, null, pageable);

        // Assert — "Old Phone" is disabled, should not appear
        assertThat(result.getContent()).noneMatch(p -> p.getName().equals("Old Phone"));
    }

    @Test
    @DisplayName("Should return empty when no product matches search criteria")
    void searchProduct_Pageable_NoMatch() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> result = productRepository.searchProduct("XYZNonExistent", null, null, pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null brandIds in search")
    void searchProduct_Pageable_NullBrandIds() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — null brandIds means no brand filter
        Page<Product> result = productRepository.searchProduct("iPhone", null, null, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
    }

    // ======================== SEARCH PRODUCT (CUSTOMER - LIST) ========================

    @Test
    @DisplayName("Should search enabled products by keyword returning list")
    void searchProduct_List_KeywordMatch() {
        // Act
        List<Product> result = productRepository.searchProduct("iPhone");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(Product::isEnabled);
        assertThat(result).extracting(Product::getName)
                .containsExactlyInAnyOrder("iPhone 15", "iPhone 15 Pro");
    }

    @Test
    @DisplayName("Should search products ignoring case in list query")
    void searchProduct_List_IgnoreCase() {
        // Act
        List<Product> result = productRepository.searchProduct("samsung");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Samsung Galaxy S24");
    }

    @Test
    @DisplayName("Should return empty list when keyword matches nothing")
    void searchProduct_List_NoMatch() {
        // Act
        List<Product> result = productRepository.searchProduct("XYZNonExistent");

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve product")
    void save_AndFindById() {
        // Arrange
        Product product = new Product();
        product.setName("New Product");
        product.setAlias("new-product");
        product.setSummary("A new product");
        product.setDescription("Detailed description");
        product.setCategory(electronics);
        product.setBrand(apple);
        product.setEnabled(true);
        product.setAverageRating(0.0f);
        product.setCost(50.0f);
        product.setPrice(75.0f);
        product.setMainImage("new-product.png");
        product.setDiscountPercent(0.0f);
        product.setReviewCount(0);
        product.setInStock(true);
        product.setLength(5.0f);
        product.setWidth(5.0f);
        product.setHeight(5.0f);
        product.setWeight(0.5f);

        // Act
        Product saved = productRepository.save(product);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getName()).isEqualTo("New Product");
    }

    @Test
    @DisplayName("Should delete product by id")
    void deleteById_Success() {
        // Arrange
        Product product = findProductByName("Dell XPS 15");
        Long productId = product.getId();

        // Act
        productRepository.deleteById(productId);

        // Assert
        assertThat(productRepository.findById(productId)).isEmpty();
    }

    @Test
    @DisplayName("Should count products correctly")
    void count_Success() {
        // 7 from setUp
        assertThat(productRepository.count()).isEqualTo(7);
    }

    // ======================== HELPER METHODS ========================

    private Category persistCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(name + " description");
        category.setImage(name.toLowerCase().replace(" ", "-") + ".png");
        category.setEnabled(true);
        return entityManager.persistAndFlush(category);
    }

    private Brand persistBrand(String name) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setLogo(name.toLowerCase().replace(" ", "-") + ".png");
        return entityManager.persistAndFlush(brand);
    }

    private Product persistProduct(String name, String summary, String description,
                                   String alias, Category category, Brand brand,
                                   float averageRating, boolean enabled) {
        Product product = new Product();
        product.setName(name);
        product.setSummary(summary);
        product.setDescription(description);
        product.setAlias(alias);
        product.setCategory(category);
        product.setBrand(brand);
        product.setAverageRating(averageRating);
        product.setEnabled(enabled);
        product.setCost(100.0f);
        product.setPrice(150.0f);
        product.setMainImage(name.toLowerCase().replace(" ", "-") + ".png");
        product.setDiscountPercent(0.0f);
        product.setReviewCount(0);
        product.setInStock(true);
        product.setLength(10.0f);
        product.setWidth(10.0f);
        product.setHeight(10.0f);
        product.setWeight(1.0f);
        return entityManager.persistAndFlush(product);
    }
    private Product findProductByName(String name) {
        return entityManager.getEntityManager()
                .createQuery("SELECT p FROM Product p WHERE p.name = :name", Product.class)
                .setParameter("name", name)
                .getSingleResult();
    }
}