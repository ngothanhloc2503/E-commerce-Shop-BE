package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CategoryMapperTest {

    private CategoryMapper categoryMapper;

    @BeforeEach
    void setUp() {
        categoryMapper = Mappers.getMapper(CategoryMapper.class);
    }

    @Test
    void testToCategoryDTO_AllFieldsMapped() {
        // Given
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setDescription("Electronic devices and gadgets");
        category.setImage("electronics.png");
        category.setEnabled(true);

        // Setup parent để test field @Transient getParentID() và getListParentName()
        Category parent = new Category();
        parent.setId(0L);
        parent.setName("Root");
        category.setParent(parent);

        Set<Category> children = new HashSet<>();
        Category child = new Category();
        child.setId(2L);
        child.setName("Smartphones");
        children.add(child);
        category.setChildren(children);

        // When
        CategoryDTO categoryDTO = categoryMapper.toCategoryDTO(category);

        // Then
        assertNotNull(categoryDTO);
        assertEquals(1L, categoryDTO.getId());
        assertEquals("Electronics", categoryDTO.getName());
        assertEquals("Electronic devices and gadgets", categoryDTO.getDescription());
        assertEquals("electronics.png", categoryDTO.getImage());
        assertTrue(categoryDTO.isEnabled());

        // Kiểm tra field @Transient được map đúng
        assertEquals(0L, categoryDTO.getParentID());
        assertNotNull(categoryDTO.getListParentName());
        assertEquals(1, categoryDTO.getListParentName().size());
        assertEquals("Root", categoryDTO.getListParentName().get(0));

        // Kiểm tra children
        assertNotNull(categoryDTO.getChildren());
        assertEquals(1, categoryDTO.getChildren().size());
        assertEquals("Smartphones", categoryDTO.getChildren().iterator().next().getName());
    }

    @Test
    void testToCategoryDTO_NullInput() {
        CategoryDTO categoryDTO = categoryMapper.toCategoryDTO(null);
        assertNull(categoryDTO);
    }

    @Test
    void testToCategoryDTO_NullFields() {
        // Given
        Category category = new Category();
        category.setId(1L);
        category.setName("Simple Category");
        // parent = null, children = new HashSet<>() (mặc định trong Entity)

        // When
        CategoryDTO categoryDTO = categoryMapper.toCategoryDTO(category);

        // Then
        assertNotNull(categoryDTO);
        assertEquals(1L, categoryDTO.getId());
        assertEquals("Simple Category", categoryDTO.getName());
        assertNull(categoryDTO.getDescription());
        assertNull(categoryDTO.getImage());
        assertFalse(categoryDTO.isEnabled()); // default boolean value

        // Kiểm tra @Transient khi parent = null
        assertEquals(0L, categoryDTO.getParentID()); // Theo logic Entity: parent != null ? parent.getId() : 0
        assertNotNull(categoryDTO.getListParentName());
        assertTrue(categoryDTO.getListParentName().isEmpty());
    }

    @Test
    void testToCategory_FromCategoryDTO() {
        // Given
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(1L);
        categoryDTO.setName("Computers");
        categoryDTO.setDescription("Computer products");
        categoryDTO.setImage("computers.png");
        categoryDTO.setEnabled(true);
        categoryDTO.setParentID(5L); // Test map ngược parentID

        // When
        Category category = categoryMapper.toCategory(categoryDTO);

        // Then
        assertNotNull(category);
        assertEquals(1L, category.getId());
        assertEquals("Computers", category.getName());
        assertEquals("Computer products", category.getDescription());
        assertEquals("computers.png", category.getImage());
        assertTrue(category.isEnabled());

        assertNotNull(category.getParent());
        assertEquals(5L, category.getParent().getId());

        assertNotNull(category.getChildren());
        assertTrue(category.getChildren().isEmpty());
    }

    @Test
    void testToCategory_NullDTO() {
        Category category = categoryMapper.toCategory(null);
        assertNull(category);
    }

    @Test
    void testToCategoryDTO_ListMapping() {
        // Given
        List<Category> categories = new ArrayList<>();

        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("Category 1");
        categories.add(category1);

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Category 2");
        categories.add(category2);

        // When: Gọi trực tiếp hàm MapStruct tự sinh (không dùng vòng lặp for thủ công)
        List<CategoryDTO> categoryDTOs = categoryMapper.toCategoryDTOList(categories);

        // Then
        assertNotNull(categoryDTOs);
        assertEquals(2, categoryDTOs.size());
        assertEquals("Category 1", categoryDTOs.get(0).getName());
        assertEquals("Category 2", categoryDTOs.get(1).getName());
    }

    @Test
    void testToCategoryDTO_EmptyList() {
        List<Category> categories = new ArrayList<>();
        List<CategoryDTO> categoryDTOs = categoryMapper.toCategoryDTOList(categories);

        assertNotNull(categoryDTOs);
        assertTrue(categoryDTOs.isEmpty());
    }

    @Test
    void testToCategoryDTO_WithNullChildren() {
        // Given
        Category category = new Category();
        category.setId(1L);
        category.setName("Parent Category");
        category.setChildren(null);

        // When
        CategoryDTO categoryDTO = categoryMapper.toCategoryDTO(category);

        // Then
        assertNotNull(categoryDTO);
        assertEquals(1L, categoryDTO.getId());
        assertEquals("Parent Category", categoryDTO.getName());

        assertNull(categoryDTO.getChildren());
    }

    @Test
    void testToCategoryDTO_WithEmptyChildren() {
        // Given
        Category category = new Category();
        category.setId(1L);
        category.setName("Parent Category");

        // When
        CategoryDTO categoryDTO = categoryMapper.toCategoryDTO(category);

        // Then
        assertNotNull(categoryDTO);
        assertNotNull(categoryDTO.getChildren());
        assertTrue(categoryDTO.getChildren().isEmpty());
    }
}