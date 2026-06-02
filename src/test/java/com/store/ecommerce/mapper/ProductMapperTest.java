package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.*;
import com.store.ecommerce.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

    private ProductMapper productMapper;
    private CategoryMapper categoryMapper;
    private BrandMapper brandMapper;

    @BeforeEach
    void setUp() {
        this.categoryMapper = new CategoryMapperImpl();
        this.brandMapper = new BrandMapperImpl();

        ProductMapperImpl productMapperImpl = new ProductMapperImpl();

        ReflectionTestUtils.setField(productMapperImpl, "categoryMapper", this.categoryMapper);
        ReflectionTestUtils.setField(productMapperImpl, "brandMapper", this.brandMapper);

        this.productMapper = productMapperImpl;
    }

    @Test
    void testToProductDTO_AllFieldsMapped() {
        // Given
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setAlias("test-product-id.1");
        product.setDescription("Product description");
        product.setSummary("Short summary");
        product.setEnabled(true);
        product.setInStock(true);
        product.setReviewCount(10);
        product.setAverageRating(4.5f);
        product.setDiscountPercent(20.0f);
        product.setPrice(100.0f);
        product.setCost(60.0f);
        product.setLength(10.0f);
        product.setWidth(5.0f);
        product.setHeight(3.0f);
        product.setWeight(1.5f);
        product.setMainImage("main.jpg");
        product.setCreatedTime(new Date());

        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        product.setCategory(category);

        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Samsung");
        product.setBrand(brand);

        Set<ProductImage> images = new HashSet<>();
        ProductImage image = new ProductImage();
        image.setId(1L);
        image.setName("image1.jpg");
        images.add(image);
        product.setImages(images);

        List<ProductDetail> details = new ArrayList<>();
        ProductDetail detail = new ProductDetail();
        detail.setId(1L);
        detail.setName("Color");
        detail.setValue("Black");
        details.add(detail);
        product.setDetails(details);

        // When
        ProductDTO productDTO = productMapper.toProductDTO(product);

        // Then
        assertNotNull(productDTO);
        assertEquals(1L, productDTO.getId());
        assertEquals("Test Product", productDTO.getName());
        assertEquals("Electronics", productDTO.getCategory().getName());
        assertEquals("Samsung", productDTO.getBrand().getName());
        assertNotNull(productDTO.getImages());
        assertEquals(1, productDTO.getImages().size());
        assertNotNull(productDTO.getDetails());
        assertEquals(1, productDTO.getDetails().size());
    }

    @Test
    void testToProductDTO_NullInput() {
        ProductDTO productDTO = productMapper.toProductDTO(null);
        assertNull(productDTO);
    }

    @Test
    void testToProductDTO_NullNestedObjects() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Simple Product");
        product.setCategory(null);
        product.setBrand(null);
        product.setImages(null);
        product.setDetails(null);

        ProductDTO productDTO = productMapper.toProductDTO(product);

        assertNotNull(productDTO);
        assertEquals(1L, productDTO.getId());
        assertNull(productDTO.getCategory());
        assertNull(productDTO.getBrand());
        assertNull(productDTO.getImages());
        assertNull(productDTO.getDetails());
    }

    @Test
    void testToProductImageDTO() {
        ProductImage image = new ProductImage();
        image.setId(1L);
        image.setName("test-image.jpg");

        ProductImageDTO imageDTO = productMapper.toProductImageDTO(image);
        assertNotNull(imageDTO);
        assertEquals(1L, imageDTO.getId());
        assertEquals("test-image.jpg", imageDTO.getName());
    }

    @Test
    void testToProductDetailDTO() {
        ProductDetail detail = new ProductDetail();
        detail.setId(1L);
        detail.setName("Specification");
        detail.setValue("Value");

        ProductDetailDTO detailDTO = productMapper.toProductDetailDTO(detail);
        assertNotNull(detailDTO);
        assertEquals(1L, detailDTO.getId());
        assertEquals("Specification", detailDTO.getName());
        assertEquals("Value", detailDTO.getValue());
    }

    @Test
    void testImagesToImagesDTO_NullInput() {
        Set<ProductImageDTO> result = productMapper.imagesToImagesDTO(null);
        assertNull(result);
    }

    @Test
    void testImagesToImagesDTO_EmptySet() {
        Set<ProductImageDTO> result = productMapper.imagesToImagesDTO(new HashSet<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDetailsToDetailsDTO_NullInput() {
        List<ProductDetailDTO> result = productMapper.detailsToDetailsDTO(null);
        assertNull(result);
    }

    @Test
    void testDetailsToDetailsDTO_EmptyList() {
        List<ProductDetailDTO> result = productMapper.detailsToDetailsDTO(new ArrayList<>());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testToProduct_FromProductDTO() {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(1L);

        BrandDTO brandDTO = new BrandDTO();
        brandDTO.setId(1L);

        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setName("Test Product");
        productDTO.setDescription("Description");
        productDTO.setSummary("Summary");
        productDTO.setEnabled(true);
        productDTO.setInStock(true);
        productDTO.setReviewCount(5);
        productDTO.setAverageRating(4.0f);
        productDTO.setDiscountPercent(10.0f);
        productDTO.setPrice(50.0f);
        productDTO.setCost(30.0f);
        productDTO.setLength(8.0f);
        productDTO.setWidth(4.0f);
        productDTO.setHeight(2.0f);
        productDTO.setWeight(1.0f);
        productDTO.setMainImage("product.jpg");
        productDTO.setCategory(categoryDTO);
        productDTO.setBrand(brandDTO);

        Product product = productMapper.toProduct(productDTO);

        assertNotNull(product);
        assertEquals(1L, product.getId());
        assertEquals("Test Product", product.getName());
        assertNotNull(product.getCategory());
        assertEquals(1L, product.getCategory().getId());
        assertNotNull(product.getBrand());
        assertEquals(1L, product.getBrand().getId());
    }

    @Test
    void testToProduct_NullDTO() {
        Product product = productMapper.toProduct(null);
        assertNull(product);
    }

    @Test
    void testToProductDTO_ListMapping() {
        // Given
        List<Product> products = new ArrayList<>();
        Product product1 = new Product(); product1.setId(1L); product1.setName("Product 1"); products.add(product1);
        Product product2 = new Product(); product2.setId(2L); product2.setName("Product 2"); products.add(product2);

        // When: ✅ Gọi trực tiếp hàm MapStruct tự sinh, không dùng vòng lặp for thủ công
        List<ProductDTO> productDTOs = productMapper.toProductDTOList(products);

        // Then
        assertNotNull(productDTOs);
        assertEquals(2, productDTOs.size());
        assertEquals("Product 1", productDTOs.get(0).getName());
        assertEquals("Product 2", productDTOs.get(1).getName());
    }
}