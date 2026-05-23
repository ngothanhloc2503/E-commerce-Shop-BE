package com.store.ecommerce.service;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.entity.Category;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.CategoryMapper;
import com.store.ecommerce.repository.CategoryRepository;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.impl.CategoryServiceImpl;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Unit Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AWSS3Service awsS3Service;

    @Mock
    private PagingAndSortingHelper pagingAndSortingHelper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private CategoryDTO testCategoryDTO;
    private Category childCategory;
    private CategoryDTO childCategoryDTO;

    @BeforeEach
    void setUp() {
        // Parent category
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");
        testCategory.setEnabled(true);
        testCategory.setImage("electronics.png");
        testCategory.setParent(null);
        testCategory.setChildren(new HashSet<>());

        // Child category
        childCategory = new Category();
        childCategory.setId(2L);
        childCategory.setName("Smartphones");
        childCategory.setEnabled(true);
        childCategory.setImage("smartphones.png");
        childCategory.setParent(testCategory);
        childCategory.setChildren(new HashSet<>());

        testCategory.getChildren().add(childCategory);

        // Parent DTO
        testCategoryDTO = new CategoryDTO();
        testCategoryDTO.setId(1L);
        testCategoryDTO.setName("Electronics");
        testCategoryDTO.setEnabled(true);
        testCategoryDTO.setImage("electronics.png");
        testCategoryDTO.setParentID(0L);
        testCategoryDTO.setChildren(new HashSet<>());

        // Child DTO
        childCategoryDTO = new CategoryDTO();
        childCategoryDTO.setId(2L);
        childCategoryDTO.setName("Smartphones");
        childCategoryDTO.setEnabled(true);
        childCategoryDTO.setImage("smartphones.png");
        childCategoryDTO.setParentID(1L);
        childCategoryDTO.setChildren(new HashSet<>());
    }

    // ============================= getAllCategories =============================

    @Nested
    @DisplayName("getAllCategories - Lấy tất cả danh mục")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Should return list of CategoryDTOs with image paths")
        void shouldReturnListOfCategoryDTOs_WithImagePaths() {
            when(categoryRepository.findAll()).thenReturn(List.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            List<CategoryDTO> result = categoryService.getAllCategories();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Electronics");
            verify(awsS3Service).getImagePath(eq("category-images/1"), eq("electronics.png"));
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void shouldReturnEmptyList_WhenNoCategoriesExist() {
            when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

            List<CategoryDTO> result = categoryService.getAllCategories();

            assertThat(result).isEmpty();
            verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
        }

        @Test
        @DisplayName("Should set image path for each category")
        void shouldSetImagePath_ForEachCategory() {
            Category cat2 = new Category();
            cat2.setId(2L);
            cat2.setName("Clothing");
            cat2.setImage("clothing.png");

            CategoryDTO dto2 = new CategoryDTO();
            dto2.setId(2L);
            dto2.setName("Clothing");
            dto2.setImage("clothing.png");

            when(categoryRepository.findAll()).thenReturn(List.of(testCategory, cat2));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(categoryMapper.toCategoryDTO(cat2)).thenReturn(dto2);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            List<CategoryDTO> result = categoryService.getAllCategories();

            assertThat(result).hasSize(2);
            verify(awsS3Service, times(2)).getImagePath(anyString(), anyString());
        }
    }

    // ============================= getAllCategories (keyword, sort) =============================

    @Nested
    @DisplayName("getAllCategories - Lấy danh mục với từ khóa và sắp xếp")
    class GetAllCategoriesWithKeywordAndSortTests {

        @Test
        @DisplayName("Should return filtered and sorted categories ascending")
        void shouldReturnFilteredAndSortedCategories_Ascending() {
            when(categoryRepository.findAll(eq("elec"), any(Sort.class))).thenReturn(List.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            List<CategoryDTO> result = categoryService.getAllCategories("elec", "name", "asc");

            assertThat(result).hasSize(1);
            verify(categoryRepository).findAll(eq("elec"), any(Sort.class));
        }

        @Test
        @DisplayName("Should return filtered and sorted categories descending")
        void shouldReturnFilteredAndSortedCategories_Descending() {
            when(categoryRepository.findAll(eq("elec"), any(Sort.class))).thenReturn(List.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            List<CategoryDTO> result = categoryService.getAllCategories("elec", "name", "desc");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should default to descending when sortDir is not 'asc'")
        void shouldDefaultToDescending_WhenSortDirIsNotAsc() {
            when(categoryRepository.findAll(anyString(), any(Sort.class))).thenReturn(List.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.getAllCategories("test", "name", "invalid");

            verify(categoryRepository).findAll(eq("test"), any(Sort.class));
        }

        @Test
        @DisplayName("Should return empty list when keyword matches no categories")
        void shouldReturnEmptyList_WhenKeywordMatchesNoCategories() {
            when(categoryRepository.findAll(eq("nonexistent"), any(Sort.class))).thenReturn(Collections.emptyList());

            List<CategoryDTO> result = categoryService.getAllCategories("nonexistent", "name", "asc");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should set image path for filtered categories")
        void shouldSetImagePath_ForFilteredCategories() {
            when(categoryRepository.findAll(eq("elec"), any(Sort.class))).thenReturn(List.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.getAllCategories("elec", "name", "asc");

            verify(awsS3Service).getImagePath(eq("category-images/1"), eq("electronics.png"));
        }
    }

    // ============================= getCategoriesByPage =============================

    @Nested
    @DisplayName("getCategoriesByPage - Lấy danh mục phân trang")
    class GetCategoriesByPageTests {

        @Test
        @DisplayName("Should return paginated CategoryDTOs with image paths")
        @SuppressWarnings("unchecked")
        void shouldReturnPaginatedCategoryDTOs_WithImagePaths() {
            Page<Category> categoryPage = new PageImpl<>(List.of(testCategory));

            doReturn(categoryPage).when(pagingAndSortingHelper).getPageEntities(any(CategoryRepository.class));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            Page<CategoryDTO> result = categoryService.getCategoriesByPage(pagingAndSortingHelper);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(pagingAndSortingHelper).getPageEntities(categoryRepository);
        }

        @Test
        @DisplayName("Should return empty page when no categories exist")
        @SuppressWarnings("unchecked")
        void shouldReturnEmptyPage_WhenNoCategoriesExist() {
            Page<Category> emptyPage = new PageImpl<>(Collections.emptyList());

            doReturn(emptyPage).when(pagingAndSortingHelper).getPageEntities(any(CategoryRepository.class));

            Page<CategoryDTO> result = categoryService.getCategoriesByPage(pagingAndSortingHelper);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should set image path for each category in page")
        @SuppressWarnings("unchecked")
        void shouldSetImagePath_ForEachCategoryInPage() {
            Page<Category> categoryPage = new PageImpl<>(List.of(testCategory));

            doReturn(categoryPage).when(pagingAndSortingHelper).getPageEntities(any(CategoryRepository.class));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.getCategoriesByPage(pagingAndSortingHelper);

            verify(awsS3Service).getImagePath(eq("category-images/1"), eq("electronics.png"));
        }
    }

    // ============================= getCategoryById =============================

    @Nested
    @DisplayName("getCategoryById - Lấy danh mục theo ID")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("Should return CategoryDTO with image path when category exists")
        void shouldReturnCategoryDTO_WithImagePath_WhenCategoryExists() throws NotFoundException {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            CategoryDTO result = categoryService.getCategoryById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Electronics");
            verify(awsS3Service).getImagePath(eq("category-images/1"), eq("electronics.png"));
        }

        @Test
        @DisplayName("Should throw NotFoundException when category does not exist")
        void shouldThrowNotFoundException_WhenCategoryDoesNotExist() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any category with ID");

            verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
        }
    }

    // ============================= save =============================

    @Nested
    @DisplayName("save - Tạo danh mục mới")
    class SaveNewCategoryTests {

        @Test
        @DisplayName("Should create new category successfully when name is unique")
        void shouldCreateNewCategory_Successfully_WhenNameIsUnique() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());

            CategoryDTO newCatDTO = new CategoryDTO();
            newCatDTO.setId(null);
            newCatDTO.setName("New Category");
            newCatDTO.setParentID(0L);

            Category catEntity = new Category();
            catEntity.setName("New Category");
            when(categoryMapper.toCategory(newCatDTO)).thenReturn(catEntity);

            Category savedCat = new Category();
            savedCat.setId(1L);
            savedCat.setName("New Category");
            savedCat.setImage(null);
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Category");
            savedDTO.setImage(null);
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            CategoryDTO result = categoryService.save(newCatDTO, null);

            assertThat(result).isNotNull();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when category name already exists")
        void shouldThrowConflictException_WhenCategoryNameAlreadyExists() {
            Category existingCategory = new Category();
            existingCategory.setId(2L);
            existingCategory.setName("Electronics");
            when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(existingCategory));

            assertThatThrownBy(() -> categoryService.save(testCategoryDTO, null))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Category name already exists!");

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("Should set image filename with UUID prefix when image file is provided")
        void shouldSetImageFilename_WithUUIDPrefix_WhenImageFileIsProvided() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());

            CategoryDTO newCatDTO = new CategoryDTO();
            newCatDTO.setId(null);
            newCatDTO.setName("New Category");
            newCatDTO.setParentID(0L);

            MockMultipartFile image = new MockMultipartFile(
                    "image", "cat-image.png", "image/png", "image-data".getBytes());

            Category catEntity = new Category();
            catEntity.setName("New Category");
            when(categoryMapper.toCategory(newCatDTO)).thenReturn(catEntity);

            Category savedCat = new Category();
            savedCat.setId(1L);
            savedCat.setName("New Category");
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Category");
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.save(newCatDTO, image);

            assertThat(newCatDTO.getImage()).contains("cat-image.png");
            assertThat(newCatDTO.getImage()).isNotEqualTo("cat-image.png"); // has UUID prefix
        }

        @Test
        @DisplayName("Should upload image to S3 when image file is provided")
        void shouldUploadImageToS3_WhenImageFileIsProvided() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());

            CategoryDTO newCatDTO = new CategoryDTO();
            newCatDTO.setId(null);
            newCatDTO.setName("New Category");
            newCatDTO.setParentID(0L);

            MockMultipartFile image = new MockMultipartFile(
                    "image", "cat-image.png", "image/png", "image-data".getBytes());

            Category catEntity = new Category();
            catEntity.setName("New Category");
            when(categoryMapper.toCategory(newCatDTO)).thenReturn(catEntity);

            Category savedCat = new Category();
            savedCat.setId(1L);
            savedCat.setName("New Category");
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Category");
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.save(newCatDTO, image);

            verify(awsS3Service).removeFolder(eq("category-images/1/"));
            verify(awsS3Service).uploadFile(eq("category-images/1"), anyString(), any(), anyLong(), eq("image/png"));
        }

        @Test
        @DisplayName("Should not upload image when no image file is provided")
        void shouldNotUploadImage_WhenNoImageFileIsProvided() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());

            CategoryDTO newCatDTO = new CategoryDTO();
            newCatDTO.setId(null);
            newCatDTO.setName("New Category");
            newCatDTO.setParentID(0L);

            Category catEntity = new Category();
            catEntity.setName("New Category");
            when(categoryMapper.toCategory(newCatDTO)).thenReturn(catEntity);

            Category savedCat = new Category();
            savedCat.setId(1L);
            savedCat.setName("New Category");
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Category");
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.save(newCatDTO, null);

            verify(awsS3Service, never()).removeFolder(anyString());
            verify(awsS3Service, never()).uploadFile(anyString(), anyString(), any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("Should set image to null when no file and image has text")
        void shouldSetImageToNull_WhenNoFileAndImageHasText() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());

            CategoryDTO newCatDTO = new CategoryDTO();
            newCatDTO.setId(null);
            newCatDTO.setName("New Category");
            newCatDTO.setImage("old-image.png");
            newCatDTO.setParentID(0L);

            Category catEntity = new Category();
            catEntity.setName("New Category");
            when(categoryMapper.toCategory(newCatDTO)).thenReturn(catEntity);

            Category savedCat = new Category();
            savedCat.setId(1L);
            savedCat.setName("New Category");
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Category");
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.save(newCatDTO, null);

            assertThat(newCatDTO.getImage()).isNull();
        }

        @Test
        @DisplayName("Should set parent category when parentID is greater than 0")
        void shouldSetParentCategory_WhenParentIDIsGreaterThanZero() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("Smartphones")).thenReturn(Optional.empty());

            CategoryDTO newCatDTO = new CategoryDTO();
            newCatDTO.setId(null);
            newCatDTO.setName("Smartphones");
            newCatDTO.setParentID(1L);

            Category catEntity = new Category();
            catEntity.setName("Smartphones");
            when(categoryMapper.toCategory(newCatDTO)).thenReturn(catEntity);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            Category savedCat = new Category();
            savedCat.setId(2L);
            savedCat.setName("Smartphones");
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(2L);
            savedDTO.setName("Smartphones");
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.save(newCatDTO, null);

            verify(categoryRepository).findById(1L);
            verify(categoryRepository).save(argThat(cat ->
                    cat.getParent() != null && cat.getParent().getId().equals(1L)
            ));
        }

        @Test
        @DisplayName("Should set parent to null when parentID is 0")
        void shouldSetParentToNull_WhenParentIDIsZero() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());

            CategoryDTO newCatDTO = new CategoryDTO();
            newCatDTO.setId(null);
            newCatDTO.setName("New Category");
            newCatDTO.setParentID(0L);

            Category catEntity = new Category();
            catEntity.setName("New Category");
            when(categoryMapper.toCategory(newCatDTO)).thenReturn(catEntity);

            Category savedCat = new Category();
            savedCat.setId(1L);
            savedCat.setName("New Category");
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Category");
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.save(newCatDTO, null);

            verify(categoryRepository).save(argThat(cat ->
                    cat.getParent() == null
            ));
        }

        @Test
        @DisplayName("Should throw NotFoundException when parent category does not exist")
        void shouldThrowNotFoundException_WhenParentCategoryDoesNotExist() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("Smartphones")).thenReturn(Optional.empty());

            CategoryDTO newCatDTO = new CategoryDTO();
            newCatDTO.setId(null);
            newCatDTO.setName("Smartphones");
            newCatDTO.setParentID(999L);

            Category catEntity = new Category();
            catEntity.setName("Smartphones");
            when(categoryMapper.toCategory(newCatDTO)).thenReturn(catEntity);
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.save(newCatDTO, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Category parent is not exist");

            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("save - Cập nhật danh mục")
    class SaveUpdateCategoryTests {

        @Test
        @DisplayName("Should update existing category when name belongs to same category")
        void shouldUpdateExistingCategory_WhenNameBelongsToSameCategory() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(testCategory));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            testCategoryDTO.setImage("electronics.png");

            Category catEntity = new Category();
            catEntity.setId(1L);
            catEntity.setName("Electronics");
            when(categoryMapper.toCategory(testCategoryDTO)).thenReturn(catEntity);

            Category savedCat = new Category();
            savedCat.setId(1L);
            savedCat.setName("Electronics");
            savedCat.setImage("electronics.png");
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Electronics");
            savedDTO.setImage("electronics.png");
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            CategoryDTO result = categoryService.save(testCategoryDTO, null);

            assertThat(result).isNotNull();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should fetch existing image when image is null during update")
        void shouldFetchExistingImage_WhenImageIsNullDuringUpdate() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(testCategory));

            testCategoryDTO.setImage(null);

            Category existingCat = new Category();
            existingCat.setId(1L);
            existingCat.setImage("existing-image.png");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCat));

            Category catEntity = new Category();
            catEntity.setId(1L);
            catEntity.setName("Electronics");
            when(categoryMapper.toCategory(testCategoryDTO)).thenReturn(catEntity);

            Category savedCat = new Category();
            savedCat.setId(1L);
            savedCat.setName("Electronics");
            savedCat.setImage("existing-image.png");
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Electronics");
            savedDTO.setImage("existing-image.png");
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.save(testCategoryDTO, null);

            verify(categoryRepository).findById(1L);
            assertThat(testCategoryDTO.getImage()).isEqualTo("existing-image.png");
        }

        @Test
        @DisplayName("Should throw NotFoundException when updating category not found for image fetch")
        void shouldThrowNotFoundException_WhenUpdatingCategoryNotFoundForImageFetch() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(testCategory));

            testCategoryDTO.setImage(null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.save(testCategoryDTO, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Category not found");

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("Should set image path on saved category DTO")
        void shouldSetImagePath_OnSavedCategoryDTO() throws ConflictException, NotFoundException, IOException {
            when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(testCategory));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            Category catEntity = new Category();
            catEntity.setId(1L);
            catEntity.setName("Electronics");
            when(categoryMapper.toCategory(testCategoryDTO)).thenReturn(catEntity);

            Category savedCat = new Category();
            savedCat.setId(1L);
            savedCat.setName("Electronics");
            savedCat.setImage("electronics.png");
            when(categoryRepository.save(any(Category.class))).thenReturn(savedCat);

            CategoryDTO savedDTO = new CategoryDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Electronics");
            savedDTO.setImage("electronics.png");
            when(categoryMapper.toCategoryDTO(savedCat)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/electronics.png");

            CategoryDTO result = categoryService.save(testCategoryDTO, null);

            verify(awsS3Service).getImagePath(eq("category-images/1"), eq("electronics.png"));
        }
    }

    // ============================= isNameUnique =============================

    @Nested
    @DisplayName("isNameUnique - Kiểm tra tên danh mục duy nhất")
    class IsNameUniqueTests {

        @Test
        @DisplayName("Should return true when name does not exist")
        void shouldReturnTrue_WhenNameDoesNotExist() {
            when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());

            boolean result = categoryService.isNameUnique(null, "New Category");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when name belongs to the same category (update case)")
        void shouldReturnTrue_WhenNameBelongsToSameCategory() {
            when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(testCategory));

            boolean result = categoryService.isNameUnique(1L, "Electronics");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when name belongs to a different category")
        void shouldReturnFalse_WhenNameBelongsToDifferentCategory() {
            Category anotherCategory = new Category();
            anotherCategory.setId(2L);
            anotherCategory.setName("Electronics");
            when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(anotherCategory));

            boolean result = categoryService.isNameUnique(1L, "Electronics");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when name exists and id is null (new category)")
        void shouldReturnFalse_WhenNameExistsAndIdIsNull() {
            when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(testCategory));

            boolean result = categoryService.isNameUnique(null, "Electronics");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when name does not exist for new category")
        void shouldReturnTrue_WhenNameDoesNotExistForNewCategory() {
            when(categoryRepository.findByName("Brand New")).thenReturn(Optional.empty());

            boolean result = categoryService.isNameUnique(null, "Brand New");

            assertThat(result).isTrue();
        }
    }

    // ============================= updateCategoryEnabledStatus =============================

    @Nested
    @DisplayName("updateCategoryEnabledStatus - Cập nhật trạng thái kích hoạt")
    class UpdateCategoryEnabledStatusTests {

        @Test
        @DisplayName("Should enable category successfully")
        void shouldEnableCategory_Successfully() throws NotFoundException {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            categoryService.updateCategoryEnabledStatus(1L, true);

            verify(categoryRepository).updateEnabledStatus(1L, true);
        }

        @Test
        @DisplayName("Should disable category successfully")
        void shouldDisableCategory_Successfully() throws NotFoundException {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            categoryService.updateCategoryEnabledStatus(1L, false);

            verify(categoryRepository).updateEnabledStatus(1L, false);
        }

        @Test
        @DisplayName("Should throw NotFoundException when category does not exist")
        void shouldThrowNotFoundException_WhenCategoryDoesNotExist() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategoryEnabledStatus(999L, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any category with ID");

            verify(categoryRepository, never()).updateEnabledStatus(anyLong(), anyBoolean());
        }
    }

    // ============================= delete =============================

    @Nested
    @DisplayName("delete - Xóa danh mục")
    class DeleteTests {

        @Test
        @DisplayName("Should delete category and remove S3 folder successfully")
        void shouldDeleteCategory_AndRemoveS3Folder_Successfully() throws NotFoundException {
            testCategory.setChildren(new HashSet<>());
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            categoryService.delete(1L);

            verify(categoryRepository).deleteById(1L);
            verify(awsS3Service).removeFolder("category-images/1/");
        }

        @Test
        @DisplayName("Should throw NotFoundException when category does not exist")
        void shouldThrowNotFoundException_WhenCategoryDoesNotExist() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.delete(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any category with ID");

            verify(categoryRepository, never()).deleteById(anyLong());
            verify(awsS3Service, never()).removeFolder(anyString());
        }

        @Test
        @DisplayName("Should detach children before deleting parent category")
        void shouldDetachChildren_BeforeDeletingParentCategory() throws NotFoundException {
            Category child1 = new Category();
            child1.setId(2L);
            child1.setName("Smartphones");
            child1.setParent(testCategory);

            Category child2 = new Category();
            child2.setId(3L);
            child2.setName("Laptops");
            child2.setParent(testCategory);

            testCategory.setChildren(new HashSet<>(List.of(child1, child2)));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            categoryService.delete(1L);

            verify(categoryRepository, times(2)).save(argThat(cat ->
                    cat.getParent() == null
            ));
            verify(categoryRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should not save any children when category has no children")
        void shouldNotSaveAnyChildren_WhenCategoryHasNoChildren() throws NotFoundException {
            testCategory.setChildren(new HashSet<>());
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

            categoryService.delete(1L);

            // Only deleteById should be called, no save for children
            verify(categoryRepository).deleteById(1L);
            verify(categoryRepository, never()).save(argThat(cat ->
                    cat.getParent() == null && !cat.getId().equals(1L)
            ));
        }
    }

    // ============================= getAllCategoriesEnabled =============================

    @Nested
    @DisplayName("getAllCategoriesEnabled - Lấy danh mục đã kích hoạt (Customer)")
    class GetAllCategoriesEnabledTests {

        @Test
        @DisplayName("Should return list of enabled CategoryDTOs with image paths")
        void shouldReturnListOfEnabledCategoryDTOs_WithImagePaths() {
            when(categoryRepository.getAllCategoriesEnabled()).thenReturn(List.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            List<CategoryDTO> result = categoryService.getAllCategoriesEnabled();

            assertThat(result).hasSize(1);
            verify(awsS3Service).getImagePath(eq("category-images/1"), eq("electronics.png"));
        }

        @Test
        @DisplayName("Should return empty list when no enabled categories exist")
        void shouldReturnEmptyList_WhenNoEnabledCategoriesExist() {
            when(categoryRepository.getAllCategoriesEnabled()).thenReturn(Collections.emptyList());

            List<CategoryDTO> result = categoryService.getAllCategoriesEnabled();

            assertThat(result).isEmpty();
            verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
        }
    }

    // ============================= getCategoryByName =============================

    @Nested
    @DisplayName("getCategoryByName - Lấy danh mục theo tên (Customer)")
    class GetCategoryByNameTests {

        @Test
        @DisplayName("Should return CategoryDTO with flattened children when category exists")
        void shouldReturnCategoryDTO_WithFlattenedChildren_WhenCategoryExists() throws NotFoundException {
            // Setup: parent with nested children
            Category grandChild = new Category();
            grandChild.setId(3L);
            grandChild.setName("iPhone");
            grandChild.setImage("iphone.png");

            childCategory.setChildren(new HashSet<>(List.of(grandChild)));
            testCategory.setChildren(new HashSet<>(List.of(childCategory)));

            CategoryDTO grandChildDTO = new CategoryDTO();
            grandChildDTO.setId(3L);
            grandChildDTO.setName("iPhone");
            grandChildDTO.setImage("iphone.png");
            grandChildDTO.setChildren(new HashSet<>());

            childCategoryDTO.setChildren(new HashSet<>(List.of(grandChildDTO)));
            testCategoryDTO.setChildren(new HashSet<>(List.of(childCategoryDTO)));

            when(categoryRepository.getCategoryByName("Electronics")).thenReturn(Optional.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            CategoryDTO result = categoryService.getCategoryByName("Electronics");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Electronics");
            // Children should be flattened (both direct and nested children)
            assertThat(result.getChildren()).isNotNull();
            verify(categoryRepository).getCategoryByName("Electronics");
        }

        @Test
        @DisplayName("Should throw NotFoundException when category name does not exist")
        void shouldThrowNotFoundException_WhenCategoryNameDoesNotExist() {
            when(categoryRepository.getCategoryByName("Nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryByName("Nonexistent"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Category isn't existing");

            verify(categoryMapper, never()).toCategoryDTO(any(Category.class));
        }

        @Test
        @DisplayName("Should set image path for category and all children")
        void shouldSetImagePath_ForCategoryAndAllChildren() throws NotFoundException {
            testCategory.setChildren(new HashSet<>(List.of(childCategory)));

            childCategoryDTO.setChildren(new HashSet<>());
            testCategoryDTO.setChildren(new HashSet<>(List.of(childCategoryDTO)));

            when(categoryRepository.getCategoryByName("Electronics")).thenReturn(Optional.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            categoryService.getCategoryByName("Electronics");

            verify(awsS3Service).getImagePath(eq("category-images/1"), eq("electronics.png"));
            verify(awsS3Service).getImagePath(eq("category-images/2"), eq("smartphones.png"));
        }

        @Test
        @DisplayName("Should set children to null after flattening for each child")
        void shouldSetChildrenToNull_AfterFlattening_ForEachChild() throws NotFoundException {
            testCategory.setChildren(new HashSet<>(List.of(childCategory)));

            childCategoryDTO.setChildren(new HashSet<>());
            testCategoryDTO.setChildren(new HashSet<>(List.of(childCategoryDTO)));

            when(categoryRepository.getCategoryByName("Electronics")).thenReturn(Optional.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            CategoryDTO result = categoryService.getCategoryByName("Electronics");

            // Each child in the flattened set should have children = null
            for (CategoryDTO child : result.getChildren()) {
                assertThat(child.getChildren()).isNull();
            }
        }

        @Test
        @DisplayName("Should return category without children when category has no children")
        void shouldReturnCategory_WithoutChildren_WhenCategoryHasNoChildren() throws NotFoundException {
            testCategory.setChildren(new HashSet<>());
            testCategoryDTO.setChildren(new HashSet<>());

            when(categoryRepository.getCategoryByName("Electronics")).thenReturn(Optional.of(testCategory));
            when(categoryMapper.toCategoryDTO(testCategory)).thenReturn(testCategoryDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/image.png");

            CategoryDTO result = categoryService.getCategoryByName("Electronics");

            assertThat(result).isNotNull();
            assertThat(result.getChildren()).isEmpty();
        }
    }
}