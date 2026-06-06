package com.store.ecommerce.service;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.ProductImageDTO;
import com.store.ecommerce.dto.response.PageResponse;
import com.store.ecommerce.entity.Category;
import com.store.ecommerce.entity.Product;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.ProductMapper;
import com.store.ecommerce.repository.CategoryRepository;
import com.store.ecommerce.repository.ProductRepository;
import com.store.ecommerce.service.impl.ProductServiceImpl;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AWSS3Service awsS3Service;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductDTO testProductDTO;
    private MultipartFile mainImageFile;
    private MultipartFile emptyMainImageFile;
    private MultipartFile[] extrasImagesFile;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setAlias("test-product");
        testProduct.setEnabled(true);
        testProduct.setMainImage("product.jpg");
        testProduct.setCost(50.0f);
        testProduct.setPrice(100.0f);
        testProduct.setAverageRating(4.5f);

        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        testProduct.setCategory(category);

        testProductDTO = new ProductDTO();
        testProductDTO.setId(1L);
        testProductDTO.setName("Test Product");
        testProductDTO.setAlias("test-product");
        testProductDTO.setEnabled(true);
        testProductDTO.setMainImage("product.jpg");
        testProductDTO.setImages(new HashSet<>());

        mainImageFile = new MockMultipartFile("mainImage", "new-product.jpg", "image/jpeg", "test".getBytes());
        emptyMainImageFile = new MockMultipartFile("mainImage", "", "image/jpeg", new byte[0]);
        extrasImagesFile = new MockMultipartFile[]{
                new MockMultipartFile("extras", "extra1.jpg", "image/jpeg", "test1".getBytes()),
                new MockMultipartFile("extras", "extra2.jpg", "image/jpeg", "test2".getBytes())
        };
    }

    // ============================= getProductByID =============================

    @Nested
    @DisplayName("getProductByID - Lấy sản phẩm theo ID (Staff)")
    class GetProductByIDTests {

        @Test
        @DisplayName("Should return ProductDTO with image paths when product exists")
        void shouldReturnProductDTO_WithImagePaths_WhenProductExists() throws NotFoundException {
            ProductImageDTO imageDTO = new ProductImageDTO();
            imageDTO.setProductID(1L);
            imageDTO.setName("extra1.jpg");
            testProductDTO.setImages(Set.of(imageDTO));

            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            ProductDTO result = productService.getProductByID(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(productRepository).findById(1L);
            verify(awsS3Service).getImagePath(eq("product-images/1"), eq("product.jpg"));
            verify(awsS3Service).getImagePath(eq("product-images/1/extras"), eq("extra1.jpg"));
        }

        @Test
        @DisplayName("Should throw NotFoundException when product does not exist")
        void shouldThrowNotFoundException_WhenProductDoesNotExist() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductByID(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any product with ID");

            verify(productMapper, never()).toProductDTO(any(Product.class));
        }
    }

    // ============================= getAllProducts (categoryID) =============================

    @Nested
    @DisplayName("getAllProducts - Lấy tất cả sản phẩm theo danh mục")
    class GetAllProductsTests {

        @Test
        @DisplayName("Should return all products when categoryID is less than 1")
        void shouldReturnAllProducts_WhenCategoryIDIsLessThanOne() {
            when(productRepository.findAll()).thenReturn(List.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            List<ProductDTO> result = productService.getAllProducts(0L);

            assertThat(result).hasSize(1);
            verify(productRepository).findAll();
            verify(productRepository, never()).findByCategoryId(anyLong());
        }

        @Test
        @DisplayName("Should return products by category when categoryID is provided")
        void shouldReturnProductsByCategory_WhenCategoryIDIsProvided() {
            when(productRepository.findByCategoryId(1L)).thenReturn(List.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            List<ProductDTO> result = productService.getAllProducts(1L);

            assertThat(result).hasSize(1);
            verify(productRepository).findByCategoryId(1L);
            verify(productRepository, never()).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no products exist")
        void shouldReturnEmptyList_WhenNoProductsExist() {
            when(productRepository.findAll()).thenReturn(Collections.emptyList());

            List<ProductDTO> result = productService.getAllProducts(0L);

            assertThat(result).isEmpty();
            verify(productMapper, never()).toProductDTO(any(Product.class));
        }

        @Test
        @DisplayName("Should set main image path and extras image path for each product")
        void shouldSetMainAndExtrasImagePath_ForEachProduct() {
            ProductImageDTO imageDTO = new ProductImageDTO();
            imageDTO.setProductID(1L);
            imageDTO.setName("extra1.jpg");
            testProductDTO.setImages(Set.of(imageDTO));

            when(productRepository.findAll()).thenReturn(List.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            productService.getAllProducts(0L);

            verify(awsS3Service).getImagePath(eq("product-images/1"), eq("product.jpg"));
            verify(awsS3Service).getImagePath(eq("product-images/1/extras"), eq("extra1.jpg"));
        }
    }

    // ============================= getAllProducts (categoryID, keyword, sort) =============================

    @Nested
    @DisplayName("getAllProducts - Lấy sản phẩm với từ khóa và sắp xếp")
    class GetAllProductsWithKeywordAndSortTests {

        @Test
        @DisplayName("Should return all products with keyword when categoryID is less than 1 ascending")
        void shouldReturnAllProducts_WithKeyword_Ascending() {
            when(productRepository.searchByKeyword(eq("phone"), any(Sort.class))).thenReturn(List.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            List<ProductDTO> result = productService.getAllProducts(0L, "phone", "name", "asc");

            assertThat(result).hasSize(1);
            verify(productRepository).searchByKeyword(eq("phone"), any(Sort.class));
        }

        @Test
        @DisplayName("Should return all products with keyword when categoryID is less than 1 descending")
        void shouldReturnAllProducts_WithKeyword_Descending() {
            when(productRepository.searchByKeyword(eq("phone"), any(Sort.class))).thenReturn(List.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            List<ProductDTO> result = productService.getAllProducts(0L, "phone", "name", "desc");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return products by category with keyword when categoryID is provided")
        void shouldReturnProductsByCategory_WithKeyword() {
            when(productRepository.searchByCategoryIdAndKeyword(eq(1L), eq("phone"), any(Sort.class))).thenReturn(List.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            List<ProductDTO> result = productService.getAllProducts(1L, "phone", "name", "asc");

            assertThat(result).hasSize(1);
            verify(productRepository).searchByCategoryIdAndKeyword(eq(1L), eq("phone"), any(Sort.class));
            verify(productRepository, never()).searchByKeyword(anyString(), any(Sort.class));
        }

        @Test
        @DisplayName("Should default to descending when sortDir is not 'asc'")
        void shouldDefaultToDescending_WhenSortDirIsNotAsc() {
            when(productRepository.searchByKeyword(anyString(), any(Sort.class))).thenReturn(List.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            productService.getAllProducts(0L, "test", "name", "invalid");

            verify(productRepository).searchByKeyword(eq("test"), any(Sort.class));
        }

        @Test
        @DisplayName("Should return empty list when keyword matches no products")
        void shouldReturnEmptyList_WhenKeywordMatchesNoProducts() {
            when(productRepository.searchByKeyword(eq("nonexistent"), any(Sort.class))).thenReturn(Collections.emptyList());

            List<ProductDTO> result = productService.getAllProducts(0L, "nonexistent", "name", "asc");

            assertThat(result).isEmpty();
        }
    }

    // ============================= getProductByPage =============================

    @Nested
    @DisplayName("getProductByPage - Lấy sản phẩm phân trang (Staff)")
    class GetProductByPageTests {

        private PagingAndSortingHelper createRealHelper(int pageNum, int pageSize, String sortField, String sortDir, String keyword) {
            PagingAndSortingHelper helper = new PagingAndSortingHelper();
            helper.setPageNum(pageNum);
            helper.setPageSize(pageSize);
            helper.setSortField(sortField);
            helper.setSortDir(sortDir);
            helper.setKeyword(keyword);
            return helper;
        }

        @Test
        @DisplayName("Should return PageResponse with paginated products without category")
        void shouldReturnPageResponse_WithoutCategory() {
            // Arrange
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));

            // Dùng object thật thay vì Mock
            PagingAndSortingHelper helper = createRealHelper(1, 10, "name", "asc", null);

            when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            // Act
            PageResponse<ProductDTO> result = productService.getProductByPage(helper, 0L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testProductDTO);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getTotalItems()).isEqualTo(1L);

            verify(productRepository).findAll(any(Pageable.class));
            verify(productMapper).toProductDTO(testProduct);
        }

        @Test
        @DisplayName("Should return PageResponse with paginated products with category and no keyword")
        void shouldReturnPageResponse_WithCategoryNoKeyword() {
            // Arrange
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));
            PagingAndSortingHelper helper = createRealHelper(1, 10, "name", "asc", null);

            when(productRepository.findByCategoryId(eq(1L), any(Pageable.class))).thenReturn(productPage);
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            // Act
            PageResponse<ProductDTO> result = productService.getProductByPage(helper, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getTotalItems()).isEqualTo(1L);

            verify(productRepository).findByCategoryId(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return PageResponse with paginated products with category and keyword")
        void shouldReturnPageResponse_WithCategoryAndKeyword() {
            // Arrange
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));
            PagingAndSortingHelper helper = createRealHelper(1, 10, "name", "asc", "phone");

            when(productRepository.searchByCategoryIdAndKeyword(eq(1L), eq("phone"), any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            // Act
            PageResponse<ProductDTO> result = productService.getProductByPage(helper, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getTotalItems()).isEqualTo(1L);

            verify(productRepository).searchByCategoryIdAndKeyword(eq(1L), eq("phone"), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty PageResponse when no products exist")
        void shouldReturnEmptyPageResponse_WhenNoProductsExist() {
            // Arrange
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            PagingAndSortingHelper helper = createRealHelper(1, 10, "name", "asc", null);

            when(productRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            // Act
            PageResponse<ProductDTO> result = productService.getProductByPage(helper, 0L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalPages()).isEqualTo(0);
            assertThat(result.getTotalItems()).isEqualTo(0L);
        }
    }

    // ============================= changeEnabledStatus =============================

    @Nested
    @DisplayName("changeEnabledStatus - Thay đổi trạng thái kích hoạt")
    class ChangeEnabledStatusTests {

        @Test
        @DisplayName("Should disable product successfully")
        void shouldDisableProduct_Successfully() throws NotFoundException {
            when(productRepository.existsById(1L)).thenReturn(true);

            productService.changeEnabledStatus(1L, false);

            verify(productRepository).updateEnabledStatus(1L, false);
        }

        @Test
        @DisplayName("Should enable product successfully")
        void shouldEnableProduct_Successfully() throws NotFoundException {
            when(productRepository.existsById(1L)).thenReturn(true);

            productService.changeEnabledStatus(1L, true);

            verify(productRepository).updateEnabledStatus(1L, true);
        }

        @Test
        @DisplayName("Should throw NotFoundException when product does not exist")
        void shouldThrowNotFoundException_WhenProductDoesNotExist() {
            when(productRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> productService.changeEnabledStatus(999L, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any product with ID");

            verify(productRepository, never()).updateEnabledStatus(anyLong(), anyBoolean());
        }
    }

    // ============================= isNameUnique =============================

    @Nested
    @DisplayName("isNameUnique - Kiểm tra tên sản phẩm duy nhất")
    class IsNameUniqueTests {

        @Test
        @DisplayName("Should return true when name does not exist")
        void shouldReturnTrue_WhenNameDoesNotExist() {
            when(productRepository.findByName("New Product")).thenReturn(Optional.empty());

            boolean result = productService.isNameUnique(null, "New Product");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when name belongs to the same product (update case)")
        void shouldReturnTrue_WhenNameBelongsToSameProduct() {
            when(productRepository.findByName("Test Product")).thenReturn(Optional.of(testProduct));

            boolean result = productService.isNameUnique(1L, "Test Product");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when name belongs to a different product")
        void shouldReturnFalse_WhenNameBelongsToDifferentProduct() {
            Product anotherProduct = new Product();
            anotherProduct.setId(2L);
            anotherProduct.setName("Test Product");
            when(productRepository.findByName("Test Product")).thenReturn(Optional.of(anotherProduct));

            boolean result = productService.isNameUnique(1L, "Test Product");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when name exists and id is null (new product)")
        void shouldReturnFalse_WhenNameExistsAndIdIsNull() {
            when(productRepository.findByName("Test Product")).thenReturn(Optional.of(testProduct));

            boolean result = productService.isNameUnique(null, "Test Product");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when name does not exist for new product")
        void shouldReturnTrue_WhenNameDoesNotExistForNewProduct() {
            when(productRepository.findByName("Brand New Product")).thenReturn(Optional.empty());

            boolean result = productService.isNameUnique(null, "Brand New Product");

            assertThat(result).isTrue();
        }
    }

    // ============================= deleteProduct =============================

    @Nested
    @DisplayName("deleteProduct - Xóa sản phẩm")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete product successfully")
        void shouldDeleteProduct_Successfully() throws NotFoundException {
            when(productRepository.existsById(1L)).thenReturn(true);

            productService.deleteProduct(1L);

            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw NotFoundException when product does not exist")
        void shouldThrowNotFoundException_WhenProductDoesNotExist() {
            when(productRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> productService.deleteProduct(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any product with ID");

            verify(productRepository, never()).deleteById(anyLong());
        }
    }

    // ============================= saveProduct =============================

    @Nested
    @DisplayName("saveProduct - Lưu sản phẩm mới")
    class SaveNewProductTests {

        @Test
        @DisplayName("Should save new product successfully when name is unique")
        void shouldSaveNewProduct_Successfully_WhenNameIsUnique() throws ConflictException, IOException {
            when(productRepository.findByName("New Product")).thenReturn(Optional.empty());

            testProductDTO.setId(null);
            testProductDTO.setName("New Product");

            Product productToSave = new Product();
            productToSave.setName("New Product");
            productToSave.setImages(new HashSet<>());
            productToSave.setDetails(new HashSet<>());
            when(productMapper.toProduct(testProductDTO)).thenReturn(productToSave);

            Product savedProduct = new Product();
            savedProduct.setId(1L);
            savedProduct.setName("New Product");
            savedProduct.setMainImage("new-product.jpg");
            savedProduct.setImages(new HashSet<>());
            savedProduct.setDetails(new HashSet<>());
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Product");
            savedDTO.setMainImage("new-product.jpg");
            savedDTO.setImages(new HashSet<>());
            when(productMapper.toProductDTO(savedProduct)).thenReturn(savedDTO);
            when(awsS3Service.listFolder(anyString())).thenReturn(Collections.emptyList());

            ProductDTO result = productService.saveProduct(testProductDTO, mainImageFile, extrasImagesFile);

            assertThat(result).isNotNull();
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when product name already exists")
        void shouldThrowConflictException_WhenProductNameAlreadyExists() {
            Product existingProduct = new Product();
            existingProduct.setId(2L);
            existingProduct.setName("Test Product");
            when(productRepository.findByName("Test Product")).thenReturn(Optional.of(existingProduct));

            assertThatThrownBy(() -> productService.saveProduct(testProductDTO, mainImageFile, extrasImagesFile))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Product name already exists");

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should set main image name from multipart file when provided")
        void shouldSetMainImageName_FromMultipartFile_WhenProvided() throws ConflictException, IOException {
            when(productRepository.findByName("Test Product")).thenReturn(Optional.empty());

            testProductDTO.setId(null);
            testProductDTO.setMainImage(null);

            Product productToSave = new Product();
            productToSave.setName("Test Product");
            productToSave.setImages(new HashSet<>());
            productToSave.setDetails(new HashSet<>());
            when(productMapper.toProduct(testProductDTO)).thenReturn(productToSave);

            Product savedProduct = new Product();
            savedProduct.setId(1L);
            savedProduct.setName("Test Product");
            savedProduct.setMainImage("new-product.jpg");
            savedProduct.setImages(new HashSet<>());
            savedProduct.setDetails(new HashSet<>());
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Test Product");
            savedDTO.setMainImage("new-product.jpg");
            savedDTO.setImages(new HashSet<>());
            when(productMapper.toProductDTO(savedProduct)).thenReturn(savedDTO);
            when(awsS3Service.listFolder(anyString())).thenReturn(Collections.emptyList());

            productService.saveProduct(testProductDTO, mainImageFile, extrasImagesFile);

            // mainImage in DTO should be set to the filename from multipart
            assertThat(testProductDTO.getMainImage()).isEqualTo("new-product.jpg");
        }

        @Test
        @DisplayName("Should set main image to null when no file and no existing main image")
        void shouldSetMainImageToNull_WhenNoFileAndNoExistingMainImage() throws ConflictException, IOException {
            when(productRepository.findByName("Test Product")).thenReturn(Optional.empty());

            testProductDTO.setId(null);
            testProductDTO.setMainImage("");

            Product productToSave = new Product();
            productToSave.setName("Test Product");
            productToSave.setImages(new HashSet<>());
            productToSave.setDetails(new HashSet<>());
            when(productMapper.toProduct(testProductDTO)).thenReturn(productToSave);

            Product savedProduct = new Product();
            savedProduct.setId(1L);
            savedProduct.setName("Test Product");
            savedProduct.setImages(new HashSet<>());
            savedProduct.setDetails(new HashSet<>());
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Test Product");
            savedDTO.setImages(new HashSet<>());
            when(productMapper.toProductDTO(savedProduct)).thenReturn(savedDTO);
            when(awsS3Service.listFolder(anyString())).thenReturn(Collections.emptyList());

            productService.saveProduct(testProductDTO, emptyMainImageFile, new MultipartFile[]{});

            // mainImage in DTO should be null since no file and no existing image name
            assertThat(testProductDTO.getMainImage()).isNull();
        }

        @Test
        @DisplayName("Should upload main image when main image file is provided")
        void shouldUploadMainImage_WhenMainImageFileIsProvided() throws ConflictException, IOException {
            when(productRepository.findByName("New Product")).thenReturn(Optional.empty());

            testProductDTO.setId(null);
            testProductDTO.setName("New Product");

            Product productToSave = new Product();
            productToSave.setName("New Product");
            productToSave.setImages(new HashSet<>());
            productToSave.setDetails(new HashSet<>());
            when(productMapper.toProduct(testProductDTO)).thenReturn(productToSave);

            Product savedProduct = new Product();
            savedProduct.setId(1L);
            savedProduct.setName("New Product");
            savedProduct.setMainImage("new-product.jpg");
            savedProduct.setImages(new HashSet<>());
            savedProduct.setDetails(new HashSet<>());
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Product");
            savedDTO.setMainImage("new-product.jpg");
            savedDTO.setImages(new HashSet<>());
            when(productMapper.toProductDTO(savedProduct)).thenReturn(savedDTO);
            when(awsS3Service.listFolder(anyString())).thenReturn(Collections.emptyList());

            productService.saveProduct(testProductDTO, mainImageFile, new MultipartFile[]{});

            verify(awsS3Service).uploadFile(
                    eq("product-images/1"),
                    eq("new-product.jpg"),
                    any(),
                    anyLong(),
                    eq("image/jpeg"));
        }

        @Test
        @DisplayName("Should not upload main image when file is empty")
        void shouldNotUploadMainImage_WhenFileIsEmpty() throws ConflictException, IOException {
            when(productRepository.findByName("Test Product")).thenReturn(Optional.empty());

            testProductDTO.setId(null);
            testProductDTO.setMainImage("existing.jpg");

            Product productToSave = new Product();
            productToSave.setName("Test Product");
            productToSave.setImages(new HashSet<>());
            productToSave.setDetails(new HashSet<>());
            when(productMapper.toProduct(testProductDTO)).thenReturn(productToSave);

            Product savedProduct = new Product();
            savedProduct.setId(1L);
            savedProduct.setName("Test Product");
            savedProduct.setMainImage("existing.jpg");
            savedProduct.setImages(new HashSet<>());
            savedProduct.setDetails(new HashSet<>());
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Test Product");
            savedDTO.setMainImage("existing.jpg");
            savedDTO.setImages(new HashSet<>());
            when(productMapper.toProductDTO(savedProduct)).thenReturn(savedDTO);
            when(awsS3Service.listFolder(anyString())).thenReturn(Collections.emptyList());

            productService.saveProduct(testProductDTO, emptyMainImageFile, new MultipartFile[]{});

            // Should not upload main image since file is empty
            verify(awsS3Service, never()).uploadFile(
                    eq("product-images/1"),
                    eq("existing.jpg"),
                    any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("Should delete old main image before uploading new one")
        void shouldDeleteOldMainImage_BeforeUploadingNewOne() throws ConflictException, IOException {
            when(productRepository.findByName("New Product")).thenReturn(Optional.empty());

            testProductDTO.setId(null);
            testProductDTO.setName("New Product");

            Product productToSave = new Product();
            productToSave.setName("New Product");
            productToSave.setImages(new HashSet<>());
            productToSave.setDetails(new HashSet<>());
            when(productMapper.toProduct(testProductDTO)).thenReturn(productToSave);

            Product savedProduct = new Product();
            savedProduct.setId(1L);
            savedProduct.setName("New Product");
            savedProduct.setMainImage("new-product.jpg");
            savedProduct.setImages(new HashSet<>());
            savedProduct.setDetails(new HashSet<>());
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Product");
            savedDTO.setMainImage("new-product.jpg");
            savedDTO.setImages(new HashSet<>());
            when(productMapper.toProductDTO(savedProduct)).thenReturn(savedDTO);

            // Simulate old main image in S3
            when(awsS3Service.listFolder("product-images/1/")).thenReturn(
                    List.of("product-images/1/old-main.jpg"));

            productService.saveProduct(testProductDTO, mainImageFile, new MultipartFile[]{});

            verify(awsS3Service).deleteFile("product-images/1/old-main.jpg");
        }

        @Test
        @DisplayName("Should delete removed extra images after saving")
        void shouldDeleteRemovedExtraImages_AfterSaving() throws ConflictException, IOException {
            when(productRepository.findByName("New Product")).thenReturn(Optional.empty());

            testProductDTO.setId(null);
            testProductDTO.setName("New Product");

            Product productToSave = new Product();
            productToSave.setName("New Product");
            productToSave.setImages(new HashSet<>());
            productToSave.setDetails(new HashSet<>());
            when(productMapper.toProduct(testProductDTO)).thenReturn(productToSave);

            Product savedProduct = new Product();
            savedProduct.setId(1L);
            savedProduct.setName("New Product");
            savedProduct.setMainImage("new-product.jpg");
            savedProduct.setImages(new HashSet<>());
            savedProduct.setDetails(new HashSet<>());
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Product");
            savedDTO.setMainImage("new-product.jpg");
            savedDTO.setImages(new HashSet<>());
            when(productMapper.toProductDTO(savedProduct)).thenReturn(savedDTO);

            // Simulate old extra images in S3 that are not in productDTO
            when(awsS3Service.listFolder("product-images/1/extras/")).thenReturn(
                    List.of("product-images/1/extras/old-extra.jpg"));

            productService.saveProduct(testProductDTO, emptyMainImageFile, new MultipartFile[]{});

            verify(awsS3Service).deleteFile("product-images/1/extras/old-extra.jpg");
        }
    }

    @Nested
    @DisplayName("saveProduct - Cập nhật sản phẩm")
    class SaveUpdateProductTests {

        @Test
        @DisplayName("Should update existing product successfully when name belongs to same product")
        void shouldUpdateExistingProduct_Successfully() throws ConflictException, IOException {
            when(productRepository.findByName("Test Product")).thenReturn(Optional.of(testProduct));

            Product productInDB = new Product();
            productInDB.setId(1L);
            productInDB.setMainImage("old-product.jpg");

            Product productToSave = new Product();
            productToSave.setId(1L);
            productToSave.setName("Test Product");
            productToSave.setImages(new HashSet<>());
            productToSave.setDetails(new HashSet<>());
            when(productMapper.toProduct(testProductDTO)).thenReturn(productToSave);
            when(productRepository.findById(1L)).thenReturn(Optional.of(productInDB));

            Product savedProduct = new Product();
            savedProduct.setId(1L);
            savedProduct.setName("Test Product");
            savedProduct.setMainImage("product.jpg");
            savedProduct.setImages(new HashSet<>());
            savedProduct.setDetails(new HashSet<>());
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Test Product");
            savedDTO.setMainImage("product.jpg");
            savedDTO.setImages(new HashSet<>());
            when(productMapper.toProductDTO(savedProduct)).thenReturn(savedDTO);
            when(awsS3Service.listFolder(anyString())).thenReturn(Collections.emptyList());

            ProductDTO result = productService.saveProduct(testProductDTO, emptyMainImageFile, new MultipartFile[]{});

            assertThat(result).isNotNull();
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should keep old main image when no new main image provided")
        void shouldKeepOldMainImage_WhenNoNewMainImageProvided() throws ConflictException, IOException {
            when(productRepository.findByName("Test Product")).thenReturn(Optional.of(testProduct));

            testProductDTO.setMainImage("");

            Product productInDB = new Product();
            productInDB.setId(1L);
            productInDB.setMainImage("old-product.jpg");

            Product productToSave = new Product();
            productToSave.setId(1L);
            productToSave.setName("Test Product");
            productToSave.setMainImage(null);
            productToSave.setImages(new HashSet<>());
            productToSave.setDetails(new HashSet<>());
            when(productMapper.toProduct(testProductDTO)).thenReturn(productToSave);
            when(productRepository.findById(1L)).thenReturn(Optional.of(productInDB));

            Product savedProduct = new Product();
            savedProduct.setId(1L);
            savedProduct.setName("Test Product");
            savedProduct.setMainImage("old-product.jpg");
            savedProduct.setImages(new HashSet<>());
            savedProduct.setDetails(new HashSet<>());
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Test Product");
            savedDTO.setMainImage("old-product.jpg");
            savedDTO.setImages(new HashSet<>());
            when(productMapper.toProductDTO(savedProduct)).thenReturn(savedDTO);
            when(awsS3Service.listFolder(anyString())).thenReturn(Collections.emptyList());

            productService.saveProduct(testProductDTO, emptyMainImageFile, new MultipartFile[]{});

            // product.setMainImage should be set from productInDB
            verify(productRepository).save(argThat(p ->
                    "old-product.jpg".equals(p.getMainImage())
            ));
        }
    }

    // ============================= getProductForHomePage =============================

    @Nested
    @DisplayName("getProductForHomePage - Lấy sản phẩm cho trang chủ (Customer)")
    class GetProductForHomePageTests {

        @Test
        @DisplayName("Should return top 15 rated products sorted by average rating descending")
        void shouldReturnTop15RatedProducts() {
            List<Product> manyProducts = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                Product p = new Product();
                p.setId((long) i);
                p.setName("Product " + i);
                p.setAverageRating((float) i * 0.2f);
                manyProducts.add(p);
            }

            when(productRepository.findAllByEnabledTrue(any(Sort.class))).thenReturn(manyProducts);
            when(productMapper.toProductDTO(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                ProductDTO dto = new ProductDTO();
                dto.setId(p.getId());
                dto.setName(p.getName());
                dto.setMainImage("image.jpg");
                dto.setImages(new HashSet<>());
                return dto;
            });
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            List<ProductDTO> result = productService.getProductForHomePage().getProducts();

            assertThat(result).hasSize(15);
            verify(productRepository).findAllByEnabledTrue(any(Sort.class));
        }

        @Test
        @DisplayName("Should return all products when less than 15 exist")
        void shouldReturnAllProducts_WhenLessThan15Exist() {
            when(productRepository.findAllByEnabledTrue(any(Sort.class))).thenReturn(List.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            List<ProductDTO> result = productService.getProductForHomePage().getProducts();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty list when no products exist")
        void shouldReturnEmptyList_WhenNoProductsExist() {
            when(productRepository.findAllByEnabledTrue(any(Sort.class))).thenReturn(Collections.emptyList());

            List<ProductDTO> result = productService.getProductForHomePage().getProducts();

            assertThat(result).isEmpty();
            verify(productMapper, never()).toProductDTO(any(Product.class));
        }

        @Test
        @DisplayName("Should set main image path and extras image path for home page products")
        void shouldSetMainAndExtrasImagePath_ForHomePageProducts() {
            ProductImageDTO imageDTO = new ProductImageDTO();
            imageDTO.setProductID(1L);
            imageDTO.setName("extra1.jpg");
            testProductDTO.setImages(Set.of(imageDTO));

            when(productRepository.findAllByEnabledTrue(any(Sort.class))).thenReturn(List.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            productService.getProductForHomePage();

            verify(awsS3Service).getImagePath(eq("product-images/1"), eq("product.jpg"));
            verify(awsS3Service).getImagePath(eq("product-images/1/extras"), eq("extra1.jpg"));
        }
    }

    // ============================= getProductByAlias =============================

    @Nested
    @DisplayName("getProductByAlias - Lấy sản phẩm theo alias (Customer)")
    class GetProductByAliasTests {

        @Test
        @DisplayName("Should return ProductDTO when product with alias exists")
        void shouldReturnProductDTO_WhenProductWithAliasExists() throws NotFoundException {
            ProductImageDTO imageDTO = new ProductImageDTO();
            imageDTO.setProductID(1L);
            imageDTO.setName("extra1.jpg");
            testProductDTO.setImages(Set.of(imageDTO));

            when(productRepository.findByAliasAndEnabledTrue("test-product")).thenReturn(Optional.of(testProduct));
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            ProductDTO result = productService.getProductByAlias("test-product");

            assertThat(result).isNotNull();
            assertThat(result.getAlias()).isEqualTo("test-product");
            verify(productRepository).findByAliasAndEnabledTrue("test-product");
        }

        @Test
        @DisplayName("Should throw NotFoundException when product with alias does not exist")
        void shouldThrowNotFoundException_WhenProductWithAliasDoesNotExist() {
            when(productRepository.findByAliasAndEnabledTrue("non-existent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductByAlias("non-existent"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Product isn't existing");

            verify(productMapper, never()).toProductDTO(any(Product.class));
        }
    }

    // ============================= getProductByCategoryName =============================

    @Nested
    @DisplayName("getProductByCategoryName - Lấy sản phẩm theo tên danh mục (Customer)")
    class GetProductByCategoryNameTests {

        @Test
        @DisplayName("Should return paginated products filtered by category name")
        void shouldReturnPaginatedProducts_FilteredByCategoryName() {
            // Arrange
            String categoryName = "Electronics";
            Long categoryId = 1L;
            List<Long> categoryIds = List.of(1L, 2L, 3L);
            Pageable pageable = PageRequest.of(0, 15, Sort.by("averageRating").descending());

            when(categoryRepository.findIdByNameFlexible(categoryName)).thenReturn(Optional.of(categoryId));
            when(categoryRepository.findCategoryAndAllDescendantIds(categoryId)).thenReturn(categoryIds);

            Product mockProduct = new Product("Test Product");
            mockProduct.setId(1L);
            mockProduct.setMainImage("main.jpg");
            Page<Product> mockPage = new PageImpl<>(List.of(mockProduct), pageable, 1);

            when(productRepository.findByCategoryIdInAndEnabledTrue(eq(categoryIds), any(Pageable.class)))
                    .thenReturn(mockPage);

            ProductDTO mockDto = new ProductDTO();
            mockDto.setId(1L);
            mockDto.setMainImage("main.jpg");
            mockDto.setImages(new HashSet<>());
            when(productMapper.toProductDTO(any(Product.class))).thenReturn(mockDto);

            when(awsS3Service.getImagePath(any(), any())).thenReturn("http://image.url");

            // Act
            Page<ProductDTO> result = productService.getProductByCategoryName(categoryName, 1);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(categoryRepository).findIdByNameFlexible(categoryName);
            verify(categoryRepository).findCategoryAndAllDescendantIds(categoryId);
            verify(productRepository).findByCategoryIdInAndEnabledTrue(eq(categoryIds), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return all enabled products when category name is empty")
        void shouldReturnAllEnabledProducts_WhenCategoryNameIsEmpty() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 15, Sort.by("averageRating").descending());

            Product mockProduct = new Product("Test Product");
            mockProduct.setId(1L);
            mockProduct.setMainImage("main.jpg");
            Page<Product> mockPage = new PageImpl<>(List.of(mockProduct), pageable, 1);

            when(productRepository.findAllByEnabledTrue(any(Pageable.class))).thenReturn(mockPage);

            ProductDTO mockDto = new ProductDTO();
            mockDto.setId(1L);
            mockDto.setMainImage("main.jpg");
            mockDto.setImages(new HashSet<>());
            when(productMapper.toProductDTO(any(Product.class))).thenReturn(mockDto);

            when(awsS3Service.getImagePath(any(), any())).thenReturn("http://image.url");

            // Act
            Page<ProductDTO> result = productService.getProductByCategoryName(null, 1);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            verify(productRepository).findAllByEnabledTrue(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no products match category")
        void shouldReturnEmptyPage_WhenNoProductsMatchCategory() {
            // Arrange
            String categoryName = "NonExistentCategory";

            when(categoryRepository.findIdByNameFlexible(categoryName)).thenReturn(Optional.empty());

            // Act
            Page<ProductDTO> result = productService.getProductByCategoryName(categoryName, 1);

            // Assert
            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getTotalElements());
            verify(categoryRepository).findIdByNameFlexible(categoryName);
            verify(productRepository, never()).findByCategoryIdInAndEnabledTrue(any(), any());
        }

        @Test
        @DisplayName("Should return empty page when page number exceeds available products")
        void shouldReturnEmptyPage_WhenPageNumberExceedsAvailableProducts() {
            // Arrange
            Pageable pageable = PageRequest.of(1, 15, Sort.by("averageRating").descending());
            Page<Product> emptyPage = Page.empty(pageable);

            when(productRepository.findAllByEnabledTrue(any(Pageable.class))).thenReturn(emptyPage);

            // Act
            Page<ProductDTO> result = productService.getProductByCategoryName("", 2);

            // Assert
            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            verify(productRepository).findAllByEnabledTrue(any(Pageable.class));
        }
    }

    // ============================= searchProduct =============================

    @Nested
    @DisplayName("searchProduct - Tìm kiếm sản phẩm (Customer)")
    class SearchProductTests {

        @Test
        @DisplayName("Should return paginated search results ascending")
        void shouldReturnPaginatedSearchResults_Ascending() {
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));

            when(productRepository.searchProduct(eq("phone"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            Page<ProductDTO> result = productService.searchProduct("phone", 1, "name_asc", null, null);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(productRepository).searchProduct(eq("phone"), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return paginated search results descending")
        void shouldReturnPaginatedSearchResults_Descending() {
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));

            when(productRepository.searchProduct(eq("phone"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            Page<ProductDTO> result = productService.searchProduct("phone", 1, "name_desc", null, null);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should search with rating filter")
        void shouldSearchWithRatingFilter() {
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));

            when(productRepository.searchProduct(eq("phone"), eq(4.0f), isNull(), any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            Page<ProductDTO> result = productService.searchProduct("phone", 1, "name_asc", 4.0f, null);

            assertThat(result).isNotNull();
            verify(productRepository).searchProduct(eq("phone"), eq(4.0f), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should search with brand IDs filter")
        void shouldSearchWithBrandIDsFilter() {
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));
            Long[] brandIDs = {1L, 2L};

            when(productRepository.searchProduct(eq("phone"), isNull(), eq(Arrays.asList(brandIDs)), any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            Page<ProductDTO> result = productService.searchProduct("phone", 1, "name_asc", null, brandIDs);

            assertThat(result).isNotNull();
            verify(productRepository).searchProduct(eq("phone"), isNull(), eq(Arrays.asList(brandIDs)), any(Pageable.class));
        }

        @Test
        @DisplayName("Should pass null brandIdList when brandIDs is empty array")
        void shouldPassNullBrandIdList_WhenBrandIDsIsEmptyArray() {
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));

            when(productRepository.searchProduct(eq("phone"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toProductDTO(testProduct)).thenReturn(testProductDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/image.jpg");

            productService.searchProduct("phone", 1, "name_asc", null, new Long[]{});

            verify(productRepository).searchProduct(eq("phone"), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no products match search")
        void shouldReturnEmptyPage_WhenNoProductsMatchSearch() {
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList());

            when(productRepository.searchProduct(eq("nonexistent"), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<ProductDTO> result = productService.searchProduct("nonexistent", 1, "name_asc", null, null);

            assertThat(result.getContent()).isEmpty();
        }
    }
}