package com.store.ecommerce.mapper;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BrandMapperTest {

    private BrandMapper brandMapper;

    @BeforeEach
    void setUp() {
        // Mappers.getMapper() hoạt động tốt cho Unit Test.
        // Lưu ý: Vì dùng componentModel = "spring", MapStruct sẽ sinh ra class BrandMapperImpl.
        // Bạn cũng có thể dùng: brandMapper = new BrandMapperImpl(); để test trực tiếp không cần reflection.
        brandMapper = Mappers.getMapper(BrandMapper.class);
    }

    @Test
    void testToBrandDTO_AllFieldsMapped() {
        // Given
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Samsung");
        brand.setLogo("samsung-logo.png");

        Set<Category> categories = new HashSet<>();

        Category cat1 = new Category();
        cat1.setId(1L);
        cat1.setName("Electronics");
        categories.add(cat1);

        Category cat2 = new Category();
        cat2.setId(2L);
        cat2.setName("Smartphones");
        categories.add(cat2);

        Category cat3 = new Category();
        cat3.setId(3L);
        cat3.setName("Tablets");
        categories.add(cat3);

        brand.setCategories(categories);

        // When
        BrandDTO brandDTO = brandMapper.toBrandDTO(brand);

        // Then
        assertNotNull(brandDTO);
        assertEquals(1L, brandDTO.getId());
        assertEquals("Samsung", brandDTO.getName());
        assertEquals("samsung-logo.png", brandDTO.getLogo());

        // Kiểm tra List IDs
        assertNotNull(brandDTO.getListCategoryIDs());
        assertEquals(3, brandDTO.getListCategoryIDs().size());
        assertTrue(brandDTO.getListCategoryIDs().containsAll(List.of(1L, 2L, 3L)));

        // Kiểm tra String Names
        // Lưu ý: HashSet không đảm bảo thứ tự, nên chuỗi nối ra có thể lộn xộn.
        // Ta chỉ assert chuỗi có chứa các từ khóa thay vì assertEquals toàn bộ chuỗi.
        assertNotNull(brandDTO.getNameOfCategories());
        assertTrue(brandDTO.getNameOfCategories().contains("Electronics"));
        assertTrue(brandDTO.getNameOfCategories().contains("Smartphones"));
        assertTrue(brandDTO.getNameOfCategories().contains("Tablets"));
    }

    @Test
    void testToBrandDTO_NullInput() {
        // When
        BrandDTO brandDTO = brandMapper.toBrandDTO(null);

        // Then
        assertNull(brandDTO);
    }

    @Test
    void testToBrandDTO_NullFields() {
        // Given
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Simple Brand");
        // Không set categories -> categories = null

        // When
        BrandDTO brandDTO = brandMapper.toBrandDTO(brand);

        // Then
        assertNotNull(brandDTO);
        assertEquals(1L, brandDTO.getId());
        assertEquals("Simple Brand", brandDTO.getName());
        assertNull(brandDTO.getLogo());

        // Assert logic Collections.emptyList() và "" từ @Named methods
        assertNotNull(brandDTO.getListCategoryIDs(), "ListCategoryIDs không được null");
        assertTrue(brandDTO.getListCategoryIDs().isEmpty(), "ListCategoryIDs phải rỗng");

        assertNotNull(brandDTO.getNameOfCategories(), "NameOfCategories không được null");
        assertEquals("", brandDTO.getNameOfCategories(), "NameOfCategories phải là chuỗi rỗng");
    }

    @Test
    void testToBrand_FromBrandDTO() {
        // Given
        BrandDTO brandDTO = new BrandDTO();
        brandDTO.setId(1L);
        brandDTO.setName("Apple");
        brandDTO.setLogo("apple-logo.png");
        brandDTO.setListCategoryIDs(List.of(1L, 2L)); // Có set nhưng sẽ bị ignore

        // When
        Brand brand = brandMapper.toBrand(brandDTO);

        // Then
        assertNotNull(brand);
        assertEquals(1L, brand.getId());
        assertEquals("Apple", brand.getName());
        assertEquals("apple-logo.png", brand.getLogo());

        // Categories bị ignore trong @Mapping(target = "categories", ignore = true)
        // Tuy nhiên Entity Brand có khởi tạo Set<Category> categories = new HashSet<>();
        // Nên nó sẽ là Empty Set chứ không phải null (tùy thuộc vào Entity của bạn có new HashSet() ở field không).
        assertNotNull(brand.getCategories());
        assertTrue(brand.getCategories().isEmpty());
    }

    @Test
    void testToBrand_NullDTO() {
        // When
        Brand brand = brandMapper.toBrand(null);

        // Then
        assertNull(brand);
    }

    @Test
    void testToBrandDTO_ListMapping() {
        // Given
        List<Brand> brands = new ArrayList<>();

        Brand brand1 = new Brand();
        brand1.setId(1L);
        brand1.setName("Brand 1");
        brand1.setLogo("logo1.png");
        brands.add(brand1);

        Brand brand2 = new Brand();
        brand2.setId(2L);
        brand2.setName("Brand 2");
        brand2.setLogo("logo2.png");
        brands.add(brand2);

        // When: Gọi hàm MapStruct tự động sinh ra (thay vì dùng vòng lặp for thủ công)
        List<BrandDTO> brandDTOs = brandMapper.toBrandDTOs(brands);

        // Then
        assertNotNull(brandDTOs);
        assertEquals(2, brandDTOs.size());
        assertEquals("Brand 1", brandDTOs.get(0).getName());
        assertEquals("Brand 2", brandDTOs.get(1).getName());
    }

    @Test
    void testToBrandDTO_EmptyList() {
        // Given
        List<Brand> brands = new ArrayList<>();

        // When
        List<BrandDTO> brandDTOs = brandMapper.toBrandDTOs(brands);

        // Then
        assertNotNull(brandDTOs);
        assertTrue(brandDTOs.isEmpty());
    }

    @Test
    void testToBrandDTO_WithEmptyCategories() {
        // Given
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Test Brand");
        brand.setLogo("test-logo.png");
        brand.setCategories(new HashSet<>()); // Set rỗng

        // When
        BrandDTO brandDTO = brandMapper.toBrandDTO(brand);

        // Then
        assertNotNull(brandDTO);
        assertEquals(1L, brandDTO.getId());
        assertEquals("Test Brand", brandDTO.getName());

        // Assert logic trả về Empty thay vì Null
        assertNotNull(brandDTO.getListCategoryIDs());
        assertTrue(brandDTO.getListCategoryIDs().isEmpty());

        assertNotNull(brandDTO.getNameOfCategories());
        assertEquals("", brandDTO.getNameOfCategories());
    }
}