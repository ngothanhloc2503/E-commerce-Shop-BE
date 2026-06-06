package com.store.ecommerce.repository;

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
@DisplayName("CategoryRepository Integration Tests")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        // Enabled categories
        persistCategory("Electronics", "Electronic devices and gadgets", true);
        persistCategory("Mobile Phones", "Smartphones and accessories", true);
        persistCategory("Laptops", "Notebook and laptop computers", true);
        persistCategory("Cameras", "Digital cameras and lenses", true);

        // Disabled categories
        persistCategory("Vintage Tech", "Discontinued retro technology", false);
        persistCategory("Old Monitors", "CRT and legacy monitors", false);
    }

    // ======================== FIND BY NAME ========================

    @Test
    @DisplayName("Should find category by name when exists")
    void findByName_Found() {
        // Act
        Optional<Category> found = categoryRepository.findByName("Electronics");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Should return empty when category name not found")
    void findByName_NotFound() {
        // Act
        Optional<Category> found = categoryRepository.findByName("NonExistent");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find category by name case-sensitive")
    void findByName_CaseSensitive() {
        // Act
        Optional<Category> found = categoryRepository.findByName("electronics");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find disabled category by name")
    void findByName_DisabledCategory() {
        // Act — findByName does not filter by enabled status
        Optional<Category> found = categoryRepository.findByName("Vintage Tech");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().isEnabled()).isFalse();
    }

    // ======================== UPDATE ENABLED STATUS ========================

    @Test
    @DisplayName("Should disable an enabled category")
    void updateEnabledStatus_Disable() {
        // Arrange
        Category electronics = findCategoryByName("Electronics");
        assertThat(electronics.isEnabled()).isTrue();

        // Act
        categoryRepository.updateEnabledStatus(electronics.getId(), false);

        // Assert
        entityManager.flush();
        entityManager.clear();

        Category updated = entityManager.find(Category.class, electronics.getId());
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should enable a disabled category")
    void updateEnabledStatus_Enable() {
        // Arrange
        Category vintage = findCategoryByName("Vintage Tech");
        assertThat(vintage.isEnabled()).isFalse();

        // Act
        categoryRepository.updateEnabledStatus(vintage.getId(), true);

        // Assert
        entityManager.flush();
        entityManager.clear();

        Category updated = entityManager.find(Category.class, vintage.getId());
        assertThat(updated.isEnabled()).isTrue();
    }

    // ======================== FIND ALL (PAGEABLE) ========================

    @Test
    @DisplayName("Should return paginated categories")
    void findAll_Pageable() {
        // Arrange — 6 categories total
        PageRequest pageable = PageRequest.of(0, 3);

        // Act
        Page<Category> page = categoryRepository.findAll(pageable);

        // Assert
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return second page of categories")
    void findAll_Pageable_SecondPage() {
        // Arrange
        PageRequest pageable = PageRequest.of(1, 3);

        // Act
        Page<Category> page = categoryRepository.findAll(pageable);

        // Assert
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.isLast()).isTrue();
    }

    // ======================== FIND ALL BY KEYWORD (PAGEABLE) ========================

    @Test
    @DisplayName("Should find categories by keyword matching name")
    void findAll_KeywordPageable_MatchName() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Category> result = categoryRepository.searchByKeyword("Electronics", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Should find categories by keyword matching description")
    void findAll_KeywordPageable_MatchDescription() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — "Smartphones" appears in Mobile Phones description
        Page<Category> result = categoryRepository.searchByKeyword("Smartphones", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Mobile Phones");
    }

    @Test
    @DisplayName("Should find categories by keyword matching id")
    void findAll_KeywordPageable_MatchId() {
        // Arrange
        Category cameras = findCategoryByName("Cameras");
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Category> result = categoryRepository.searchByKeyword(
                String.valueOf(cameras.getId()), pageable);

        // Assert
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).anyMatch(c -> c.getId().equals(cameras.getId()));
    }

    @Test
    @DisplayName("Should find multiple categories by partial keyword")
    void findAll_KeywordPageable_PartialMatch() {
        // Arrange — "lap" matches "Laptops" name
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Category> result = categoryRepository.searchByKeyword("lap", pageable);

        // Assert
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).anyMatch(c -> c.getName().equals("Laptops"));
    }

    @Test
    @DisplayName("Should find categories by keyword matching across name and description")
    void findAll_KeywordPageable_MultipleMatches() {
        // Arrange — "cameras" appears in Cameras name AND Cameras description
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Category> result = categoryRepository.searchByKeyword("cameras", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should return paginated keyword search results")
    void findAll_KeywordPageable_Pagination() {
        // Arrange — create more categories to test pagination
        persistCategory("Camera Lenses", "Lenses for cameras", true);
        persistCategory("Camera Bags", "Bags for cameras", true);
        persistCategory("Camera Tripods", "Tripods for cameras", true);
        persistCategory("Camera Filters", "Filters for cameras", true);

        PageRequest pageable = PageRequest.of(0, 3);

        // Act — "camera" matches multiple
        Page<Category> result = categoryRepository.searchByKeyword("camera", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isGreaterThan(3);
    }

    @Test
    @DisplayName("Should return empty page when keyword matches nothing")
    void findAll_KeywordPageable_NoMatch() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<Category> result = categoryRepository.searchByKeyword("XYZNonExistent", pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
    }

    // ======================== FIND ALL BY KEYWORD (SORT) ========================

    @Test
    @DisplayName("Should find categories by keyword with sort ascending")
    void findAll_KeywordSort_Ascending() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act — "Cam" matches "Cameras" name
        List<Category> result = categoryRepository.searchByKeyword("Cam", sort);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result).anyMatch(c -> c.getName().equals("Cameras"));
    }

    @Test
    @DisplayName("Should find categories by keyword with sort descending")
    void findAll_KeywordSort_Descending() {
        // Arrange — create more matches
        persistCategory("Camera Accessories", "Camera accessories and gear", true);
        Sort sort = Sort.by("name").descending();

        // Act
        List<Category> result = categoryRepository.searchByKeyword("Cam", sort);

        // Assert — verify sorted descending
        assertThat(result.size()).isGreaterThan(1);
        List<String> names = result.stream().map(Category::getName).toList();
        assertThat(names).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    @DisplayName("Should return empty list when keyword matches nothing with sort")
    void findAll_KeywordSort_NoMatch() {
        // Arrange
        Sort sort = Sort.by("name").ascending();

        // Act
        List<Category> result = categoryRepository.searchByKeyword("XYZNonExistent", sort);

        // Assert
        assertThat(result).isEmpty();
    }

    // ======================== GET ALL CATEGORIES ENABLED ========================

    @Test
    @DisplayName("Should return only enabled categories")
    void getAllCategoriesEnabled_OnlyEnabled() {
        // Act
        List<Category> result = categoryRepository.findAllByEnabledTrue();

        // Assert — 4 enabled from setUp
        assertThat(result).hasSize(4);
        assertThat(result).allMatch(Category::isEnabled);
    }

    @Test
    @DisplayName("Should not include disabled categories in enabled list")
    void getAllCategoriesEnabled_NoDisabled() {
        // Act
        List<Category> result = categoryRepository.findAllByEnabledTrue();

        // Assert
        assertThat(result).noneMatch(c -> !c.isEnabled());
        assertThat(result).extracting(Category::getName)
                .doesNotContain("Vintage Tech", "Old Monitors");
    }

    @Test
    @DisplayName("Should include all enabled categories")
    void getAllCategoriesEnabled_AllPresent() {
        // Act
        List<Category> result = categoryRepository.findAllByEnabledTrue();

        // Assert
        assertThat(result).extracting(Category::getName)
                .containsExactlyInAnyOrder("Electronics", "Mobile Phones", "Laptops", "Cameras");
    }

    @Test
    @DisplayName("Should reflect changes after enabling/disabling categories")
    void getAllCategoriesEnabled_AfterStatusChange() {
        // Arrange — disable Electronics
        Category electronics = findCategoryByName("Electronics");
        categoryRepository.updateEnabledStatus(electronics.getId(), false);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Category> result = categoryRepository.findAllByEnabledTrue();

        // Assert — now 3 enabled
        assertThat(result).hasSize(3);
        assertThat(result).noneMatch(c -> c.getName().equals("Electronics"));
    }

    // ======================== GET CATEGORY BY NAME (Customer) ========================

    @Test
    @DisplayName("Should find enabled category by hyphenated name")
    void getCategoryByName_Found() {
        // Act — "Mobile Phones" → name replaced: "Mobile-Phones"
        Optional<Category> found = categoryRepository.getCategoryByName("Mobile-Phones");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Mobile Phones");
        assertThat(found.get().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should find Electronics by hyphenated name")
    void getCategoryByName_SingleWord() {
        // Act — "Electronics" has no spaces, so name stays the same
        Optional<Category> found = categoryRepository.getCategoryByName("Electronics");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("Should find Laptops by hyphenated name")
    void getCategoryByName_Laptops() {
        // Act
        Optional<Category> found = categoryRepository.getCategoryByName("Laptops");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Laptops");
    }

    @Test
    @DisplayName("Should not find disabled category by name")
    void getCategoryByName_DisabledCategory() {
        // Act — "Vintage Tech" is disabled, query filters enabled = true
        Optional<Category> found = categoryRepository.getCategoryByName("Vintage-Tech");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when category name not found")
    void getCategoryByName_NotFound() {
        // Act
        Optional<Category> found = categoryRepository.getCategoryByName("NonExistent");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should not find category with space instead of hyphen")
    void getCategoryByName_SpaceInsteadOfHyphen() {
        // Act — using space "Mobile Phones" instead of hyphen "Mobile-Phones"
        Optional<Category> found = categoryRepository.getCategoryByName("Mobile Phones");

        // Assert — REPLACE changes spaces to hyphens, so "Mobile Phones" won't match
        // because stored name is "Mobile Phones" → replaced = "Mobile-Phones"
        assertThat(found).isEmpty();
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve category")
    void save_AndFindById() {
        // Arrange
        Category category = new Category();
        category.setName("Tablets");
        category.setDescription("Tablet computers");
        category.setImage("tablets.png");
        category.setEnabled(true);

        // Act
        Category saved = categoryRepository.save(category);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getName()).isEqualTo("Tablets");
    }

    @Test
    @DisplayName("Should delete category by id")
    void deleteById_Success() {
        // Arrange
        Category cameras = findCategoryByName("Cameras");
        Long categoryId = cameras.getId();

        // Act
        categoryRepository.deleteById(categoryId);

        // Assert
        assertThat(categoryRepository.findById(categoryId)).isEmpty();
    }

    @Test
    @DisplayName("Should count categories correctly")
    void count_Success() {
        // 6 from setUp
        assertThat(categoryRepository.count()).isEqualTo(6);
    }

    @Test
    @DisplayName("Should find all categories")
    void findAll_Success() {
        List<Category> categories = categoryRepository.findAll();
        assertThat(categories).hasSize(6);
    }

    // ======================== HELPER METHODS ========================

    /**
     * Persists a Category with ALL NOT NULL fields populated.
     * H2 enforces NOT NULL constraints strictly.
     * NOT NULL columns: name, description, image (enabled is primitive boolean, defaults to false)
     */
    private Category persistCategory(String name, String description, boolean enabled) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setImage(name.toLowerCase().replace(" ", "-") + ".png");
        category.setEnabled(enabled);
        return entityManager.persistAndFlush(category);
    }

    private Category findCategoryByName(String name) {
        return entityManager.getEntityManager()
                .createQuery("SELECT c FROM Category c WHERE c.name = :name", Category.class)
                .setParameter("name", name)
                .getSingleResult();
    }
}