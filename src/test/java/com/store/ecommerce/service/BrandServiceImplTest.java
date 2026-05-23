package com.store.ecommerce.service;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import com.store.ecommerce.entity.Product;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.BrandMapper;
import com.store.ecommerce.repository.BrandRepository;
import com.store.ecommerce.repository.CategoryRepository;
import com.store.ecommerce.repository.ProductRepository;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.impl.BrandServiceImpl;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BrandServiceImpl Unit Tests")
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandMapper brandMapper;

    @Mock
    private AWSS3Service awsS3Service;

    @Mock
    private PagingAndSortingHelper pagingAndSortingHelper;

    @InjectMocks
    private BrandServiceImpl brandService;

    private Brand testBrand;
    private BrandDTO testBrandDTO;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");

        testBrand = new Brand();
        testBrand.setId(1L);
        testBrand.setName("Samsung");
        testBrand.setLogo("samsung-logo.png");
        testBrand.setCategories(Set.of(testCategory));

        testBrandDTO = new BrandDTO();
        testBrandDTO.setId(1L);
        testBrandDTO.setName("Samsung");
        testBrandDTO.setLogo("samsung-logo.png");
        testBrandDTO.setListCategoryIDs(List.of(1L));
    }

    // ============================= getAllBrands =============================

    @Nested
    @DisplayName("getAllBrands - Lấy tất cả thương hiệu")
    class GetAllBrandsTests {

        @Test
        @DisplayName("Should return list of BrandDTOs with logo image paths")
        void shouldReturnListOfBrandDTOs_WithLogoImagePaths() {
            when(brandRepository.findAll()).thenReturn(List.of(testBrand));
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            List<BrandDTO> result = brandService.getAllBrands();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Samsung");
            verify(awsS3Service).getImagePath(eq("brand-logos/1"), eq("samsung-logo.png"));
        }

        @Test
        @DisplayName("Should return empty list when no brands exist")
        void shouldReturnEmptyList_WhenNoBrandsExist() {
            when(brandRepository.findAll()).thenReturn(Collections.emptyList());

            List<BrandDTO> result = brandService.getAllBrands();

            assertThat(result).isEmpty();
            verify(brandMapper, never()).toBrandDTO(any(Brand.class));
        }

        @Test
        @DisplayName("Should set logo image path for each brand")
        void shouldSetLogoImagePath_ForEachBrand() {
            Brand brand2 = new Brand();
            brand2.setId(2L);
            brand2.setName("Apple");
            brand2.setLogo("apple-logo.png");

            BrandDTO dto2 = new BrandDTO();
            dto2.setId(2L);
            dto2.setName("Apple");
            dto2.setLogo("apple-logo.png");

            when(brandRepository.findAll()).thenReturn(List.of(testBrand, brand2));
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(brandMapper.toBrandDTO(brand2)).thenReturn(dto2);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            List<BrandDTO> result = brandService.getAllBrands();

            assertThat(result).hasSize(2);
            verify(awsS3Service, times(2)).getImagePath(anyString(), anyString());
        }
    }

    // ============================= getAllBrands (keyword, sort) =============================

    @Nested
    @DisplayName("getAllBrands - Lấy thương hiệu với từ khóa và sắp xếp")
    class GetAllBrandsWithKeywordAndSortTests {

        @Test
        @DisplayName("Should return filtered and sorted brands ascending")
        void shouldReturnFilteredAndSortedBrands_Ascending() {
            when(brandRepository.findAll(eq("samsung"), any(Sort.class))).thenReturn(List.of(testBrand));
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            List<BrandDTO> result = brandService.getAllBrands("samsung", "name", "asc");

            assertThat(result).hasSize(1);
            verify(brandRepository).findAll(eq("samsung"), any(Sort.class));
        }

        @Test
        @DisplayName("Should return filtered and sorted brands descending")
        void shouldReturnFilteredAndSortedBrands_Descending() {
            when(brandRepository.findAll(eq("samsung"), any(Sort.class))).thenReturn(List.of(testBrand));
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            List<BrandDTO> result = brandService.getAllBrands("samsung", "name", "desc");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should default to descending when sortDir is not 'asc'")
        void shouldDefaultToDescending_WhenSortDirIsNotAsc() {
            when(brandRepository.findAll(anyString(), any(Sort.class))).thenReturn(List.of(testBrand));
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.getAllBrands("test", "name", "invalid");

            verify(brandRepository).findAll(eq("test"), any(Sort.class));
        }

        @Test
        @DisplayName("Should return empty list when keyword matches no brands")
        void shouldReturnEmptyList_WhenKeywordMatchesNoBrands() {
            when(brandRepository.findAll(eq("nonexistent"), any(Sort.class))).thenReturn(Collections.emptyList());

            List<BrandDTO> result = brandService.getAllBrands("nonexistent", "name", "asc");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should set logo image path for filtered brands")
        void shouldSetLogoImagePath_ForFilteredBrands() {
            when(brandRepository.findAll(eq("samsung"), any(Sort.class))).thenReturn(List.of(testBrand));
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.getAllBrands("samsung", "name", "asc");

            verify(awsS3Service).getImagePath(eq("brand-logos/1"), eq("samsung-logo.png"));
        }
    }

    // ============================= getBrandByPage =============================

    @Nested
    @DisplayName("getBrandByPage - Lấy thương hiệu phân trang")
    class GetBrandByPageTests {

        @Test
        @DisplayName("Should return paginated brands without keyword")
        void shouldReturnPaginatedBrands_WithoutKeyword() {
            Page<Brand> brandPage = new PageImpl<>(List.of(testBrand));

            when(pagingAndSortingHelper.getPageNum()).thenReturn(1);
            when(pagingAndSortingHelper.getPageSize()).thenReturn(10);
            when(pagingAndSortingHelper.getSortField()).thenReturn("name");
            when(pagingAndSortingHelper.getSortDir()).thenReturn("asc");
            when(pagingAndSortingHelper.getKeyword()).thenReturn(null);
            when(brandRepository.findAll(any(Pageable.class))).thenReturn(brandPage);
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            Page<BrandDTO> result = brandService.getBrandByPage(pagingAndSortingHelper);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(brandRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return paginated brands with keyword")
        void shouldReturnPaginatedBrands_WithKeyword() {
            Page<Brand> brandPage = new PageImpl<>(List.of(testBrand));

            when(pagingAndSortingHelper.getPageNum()).thenReturn(1);
            when(pagingAndSortingHelper.getPageSize()).thenReturn(10);
            when(pagingAndSortingHelper.getSortField()).thenReturn("name");
            when(pagingAndSortingHelper.getSortDir()).thenReturn("asc");
            when(pagingAndSortingHelper.getKeyword()).thenReturn("samsung");
            when(brandRepository.findAll(eq("samsung"), any(Pageable.class))).thenReturn(brandPage);
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            Page<BrandDTO> result = brandService.getBrandByPage(pagingAndSortingHelper);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(brandRepository).findAll(eq("samsung"), any(Pageable.class));
            verify(brandRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return paginated brands with blank keyword (treated as no keyword)")
        void shouldReturnPaginatedBrands_WithBlankKeyword() {
            Page<Brand> brandPage = new PageImpl<>(List.of(testBrand));

            when(pagingAndSortingHelper.getPageNum()).thenReturn(1);
            when(pagingAndSortingHelper.getPageSize()).thenReturn(10);
            when(pagingAndSortingHelper.getSortField()).thenReturn("name");
            when(pagingAndSortingHelper.getSortDir()).thenReturn("asc");
            when(pagingAndSortingHelper.getKeyword()).thenReturn("   ");
            when(brandRepository.findAll(any(Pageable.class))).thenReturn(brandPage);
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            Page<BrandDTO> result = brandService.getBrandByPage(pagingAndSortingHelper);

            assertThat(result).isNotNull();
            verify(brandRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no brands exist")
        void shouldReturnEmptyPage_WhenNoBrandsExist() {
            Page<Brand> emptyPage = new PageImpl<>(Collections.emptyList());

            when(pagingAndSortingHelper.getPageNum()).thenReturn(1);
            when(pagingAndSortingHelper.getPageSize()).thenReturn(10);
            when(pagingAndSortingHelper.getSortField()).thenReturn("name");
            when(pagingAndSortingHelper.getSortDir()).thenReturn("asc");
            when(pagingAndSortingHelper.getKeyword()).thenReturn(null);
            when(brandRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            Page<BrandDTO> result = brandService.getBrandByPage(pagingAndSortingHelper);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should set logo image path for each brand in page")
        void shouldSetLogoImagePath_ForEachBrandInPage() {
            Page<Brand> brandPage = new PageImpl<>(List.of(testBrand));

            when(pagingAndSortingHelper.getPageNum()).thenReturn(1);
            when(pagingAndSortingHelper.getPageSize()).thenReturn(10);
            when(pagingAndSortingHelper.getSortField()).thenReturn("name");
            when(pagingAndSortingHelper.getSortDir()).thenReturn("asc");
            when(pagingAndSortingHelper.getKeyword()).thenReturn(null);
            when(brandRepository.findAll(any(Pageable.class))).thenReturn(brandPage);
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.getBrandByPage(pagingAndSortingHelper);

            verify(awsS3Service).getImagePath(eq("brand-logos/1"), eq("samsung-logo.png"));
        }
    }

    // ============================= getBrandById =============================

    @Nested
    @DisplayName("getBrandById - Lấy thương hiệu theo ID")
    class GetBrandByIdTests {

        @Test
        @DisplayName("Should return BrandDTO with logo image path when brand exists")
        void shouldReturnBrandDTO_WithLogoImagePath_WhenBrandExists() throws NotFoundException {
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            BrandDTO result = brandService.getBrandById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Samsung");
            verify(awsS3Service).getImagePath(eq("brand-logos/1"), eq("samsung-logo.png"));
        }

        @Test
        @DisplayName("Should throw NotFoundException when brand does not exist")
        void shouldThrowNotFoundException_WhenBrandDoesNotExist() {
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.getBrandById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any brand with ID");

            verify(brandMapper, never()).toBrandDTO(any(Brand.class));
        }
    }

    // ============================= isNameUnique =============================

    @Nested
    @DisplayName("isNameUnique - Kiểm tra tên thương hiệu duy nhất")
    class IsNameUniqueTests {

        @Test
        @DisplayName("Should return true when name does not exist")
        void shouldReturnTrue_WhenNameDoesNotExist() {
            when(brandRepository.findByName("New Brand")).thenReturn(Optional.empty());

            boolean result = brandService.isNameUnique(null, "New Brand");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when name belongs to the same brand (update case)")
        void shouldReturnTrue_WhenNameBelongsToSameBrand() {
            when(brandRepository.findByName("Samsung")).thenReturn(Optional.of(testBrand));

            boolean result = brandService.isNameUnique(1L, "Samsung");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when name belongs to a different brand")
        void shouldReturnFalse_WhenNameBelongsToDifferentBrand() {
            Brand anotherBrand = new Brand();
            anotherBrand.setId(2L);
            anotherBrand.setName("Samsung");
            when(brandRepository.findByName("Samsung")).thenReturn(Optional.of(anotherBrand));

            boolean result = brandService.isNameUnique(1L, "Samsung");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when name exists and id is null (new brand)")
        void shouldReturnFalse_WhenNameExistsAndIdIsNull() {
            when(brandRepository.findByName("Samsung")).thenReturn(Optional.of(testBrand));

            boolean result = brandService.isNameUnique(null, "Samsung");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when name does not exist for new brand")
        void shouldReturnTrue_WhenNameDoesNotExistForNewBrand() {
            when(brandRepository.findByName("Brand New")).thenReturn(Optional.empty());

            boolean result = brandService.isNameUnique(null, "Brand New");

            assertThat(result).isTrue();
        }
    }

    // ============================= saveBrand =============================

    @Nested
    @DisplayName("saveBrand - Tạo thương hiệu mới")
    class SaveNewBrandTests {

        @Test
        @DisplayName("Should create new brand successfully when name is unique")
        void shouldCreateNewBrand_Successfully_WhenNameIsUnique() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("New Brand")).thenReturn(Optional.empty());

            BrandDTO newBrandDTO = new BrandDTO();
            newBrandDTO.setId(null);
            newBrandDTO.setName("New Brand");
            newBrandDTO.setListCategoryIDs(List.of(1L));

            Brand brandEntity = new Brand();
            brandEntity.setName("New Brand");
            when(brandMapper.toBrand(newBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("New Brand");
            savedBrand.setLogo(null);
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Brand");
            savedDTO.setLogo(null);
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            BrandDTO result = brandService.saveBrand(newBrandDTO, null);

            assertThat(result).isNotNull();
            verify(brandRepository).save(any(Brand.class));
        }

        @Test
        @DisplayName("Should throw ConflictException when brand name already exists")
        void shouldThrowConflictException_WhenBrandNameAlreadyExists() {
            Brand existingBrand = new Brand();
            existingBrand.setId(2L);
            existingBrand.setName("Samsung");
            when(brandRepository.findByName("Samsung")).thenReturn(Optional.of(existingBrand));

            assertThatThrownBy(() -> brandService.saveBrand(testBrandDTO, null))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Brand name already exists!");

            verify(brandRepository, never()).save(any(Brand.class));
        }

        @Test
        @DisplayName("Should set logo filename with UUID prefix when logo file is provided")
        void shouldSetLogoFilename_WithUUIDPrefix_WhenLogoFileIsProvided() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("New Brand")).thenReturn(Optional.empty());

            BrandDTO newBrandDTO = new BrandDTO();
            newBrandDTO.setId(null);
            newBrandDTO.setName("New Brand");
            newBrandDTO.setListCategoryIDs(List.of(1L));

            MockMultipartFile logo = new MockMultipartFile(
                    "logo", "brand-logo.png", "image/png", "logo-data".getBytes());

            Brand brandEntity = new Brand();
            brandEntity.setName("New Brand");
            when(brandMapper.toBrand(newBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("New Brand");
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Brand");
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.saveBrand(newBrandDTO, logo);

            assertThat(newBrandDTO.getLogo()).contains("brand-logo.png");
            assertThat(newBrandDTO.getLogo()).isNotEqualTo("brand-logo.png"); // has UUID prefix
        }

        @Test
        @DisplayName("Should upload logo to S3 when logo file is provided")
        void shouldUploadLogoToS3_WhenLogoFileIsProvided() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("New Brand")).thenReturn(Optional.empty());

            BrandDTO newBrandDTO = new BrandDTO();
            newBrandDTO.setId(null);
            newBrandDTO.setName("New Brand");
            newBrandDTO.setListCategoryIDs(List.of(1L));

            MockMultipartFile logo = new MockMultipartFile(
                    "logo", "brand-logo.png", "image/png", "logo-data".getBytes());

            Brand brandEntity = new Brand();
            brandEntity.setName("New Brand");
            when(brandMapper.toBrand(newBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("New Brand");
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Brand");
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.saveBrand(newBrandDTO, logo);

            verify(awsS3Service).removeFolder(eq("brand-logos/1/"));
            verify(awsS3Service).uploadFile(eq("brand-logos/1"), anyString(), any(), anyLong(), eq("image/png"));
        }

        @Test
        @DisplayName("Should not upload logo when no logo file is provided")
        void shouldNotUploadLogo_WhenNoLogoFileIsProvided() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("New Brand")).thenReturn(Optional.empty());

            BrandDTO newBrandDTO = new BrandDTO();
            newBrandDTO.setId(null);
            newBrandDTO.setName("New Brand");
            newBrandDTO.setListCategoryIDs(List.of(1L));

            Brand brandEntity = new Brand();
            brandEntity.setName("New Brand");
            when(brandMapper.toBrand(newBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("New Brand");
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Brand");
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.saveBrand(newBrandDTO, null);

            verify(awsS3Service, never()).removeFolder(anyString());
            verify(awsS3Service, never()).uploadFile(anyString(), anyString(), any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("Should set logo to null when no file and logo has text")
        void shouldSetLogoToNull_WhenNoFileAndLogoHasText() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("New Brand")).thenReturn(Optional.empty());

            BrandDTO newBrandDTO = new BrandDTO();
            newBrandDTO.setId(null);
            newBrandDTO.setName("New Brand");
            newBrandDTO.setLogo("old-logo.png");
            newBrandDTO.setListCategoryIDs(List.of(1L));

            Brand brandEntity = new Brand();
            brandEntity.setName("New Brand");
            when(brandMapper.toBrand(newBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("New Brand");
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Brand");
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.saveBrand(newBrandDTO, null);

            // When no file and logo has text, it should be set to null
            assertThat(newBrandDTO.getLogo()).isNull();
        }

        @Test
        @DisplayName("Should set categories from listCategoryIDs")
        void shouldSetCategories_FromListCategoryIDs() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("New Brand")).thenReturn(Optional.empty());

            BrandDTO newBrandDTO = new BrandDTO();
            newBrandDTO.setId(null);
            newBrandDTO.setName("New Brand");
            newBrandDTO.setListCategoryIDs(List.of(1L, 2L));

            Brand brandEntity = new Brand();
            brandEntity.setName("New Brand");
            when(brandMapper.toBrand(newBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("New Brand");
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Brand");
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.saveBrand(newBrandDTO, null);

            verify(brandRepository).save(argThat(brand ->
                    brand.getCategories().size() == 2
            ));
        }

        @Test
        @DisplayName("Should set logo image path on saved brand DTO")
        void shouldSetLogoImagePath_OnSavedBrandDTO() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("New Brand")).thenReturn(Optional.empty());

            BrandDTO newBrandDTO = new BrandDTO();
            newBrandDTO.setId(null);
            newBrandDTO.setName("New Brand");
            newBrandDTO.setListCategoryIDs(List.of(1L));

            Brand brandEntity = new Brand();
            brandEntity.setName("New Brand");
            when(brandMapper.toBrand(newBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("New Brand");
            savedBrand.setLogo("brand.png");
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("New Brand");
            savedDTO.setLogo("brand.png");
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), anyString())).thenReturn("http://s3.url/brand.png");

            BrandDTO result = brandService.saveBrand(newBrandDTO, null);

            verify(awsS3Service).getImagePath(eq("brand-logos/1"), eq("brand.png"));
        }
    }

    @Nested
    @DisplayName("saveBrand - Cập nhật thương hiệu")
    class SaveUpdateBrandTests {

        @Test
        @DisplayName("Should update existing brand when name belongs to same brand")
        void shouldUpdateExistingBrand_WhenNameBelongsToSameBrand() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("Samsung")).thenReturn(Optional.of(testBrand));

            testBrandDTO.setLogo(null);

            Brand existingBrand = new Brand();
            existingBrand.setId(1L);
            existingBrand.setLogo("old-logo.png");
            when(brandRepository.findById(1L)).thenReturn(Optional.of(existingBrand));

            Brand brandEntity = new Brand();
            brandEntity.setId(1L);
            brandEntity.setName("Samsung");
            when(brandMapper.toBrand(testBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("Samsung");
            savedBrand.setLogo("old-logo.png");
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Samsung");
            savedDTO.setLogo("old-logo.png");
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            BrandDTO result = brandService.saveBrand(testBrandDTO, null);

            assertThat(result).isNotNull();
            verify(brandRepository).save(any(Brand.class));
        }

        @Test
        @DisplayName("Should keep old logo when updating without new logo file")
        void shouldKeepOldLogo_WhenUpdatingWithoutNewLogoFile() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("Samsung")).thenReturn(Optional.of(testBrand));

            testBrandDTO.setLogo("samsung-logo.png");

            Brand existingBrand = new Brand();
            existingBrand.setId(1L);
            existingBrand.setLogo("samsung-logo.png");
            when(brandRepository.findById(1L)).thenReturn(Optional.of(existingBrand));

            Brand brandEntity = new Brand();
            brandEntity.setId(1L);
            brandEntity.setName("Samsung");
            when(brandMapper.toBrand(testBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("Samsung");
            savedBrand.setLogo("samsung-logo.png");
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Samsung");
            savedDTO.setLogo("samsung-logo.png");
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.saveBrand(testBrandDTO, null);

            verify(brandRepository).findById(1L);
        }

        @Test
        @DisplayName("Should fetch existing brand logo when logo is null during update")
        void shouldFetchExistingBrandLogo_WhenLogoIsNullDuringUpdate() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("Samsung")).thenReturn(Optional.of(testBrand));

            testBrandDTO.setLogo(null);

            Brand existingBrand = new Brand();
            existingBrand.setId(1L);
            existingBrand.setLogo("existing-logo.png");
            when(brandRepository.findById(1L)).thenReturn(Optional.of(existingBrand));

            Brand brandEntity = new Brand();
            brandEntity.setId(1L);
            brandEntity.setName("Samsung");
            when(brandMapper.toBrand(testBrandDTO)).thenReturn(brandEntity);

            Brand savedBrand = new Brand();
            savedBrand.setId(1L);
            savedBrand.setName("Samsung");
            savedBrand.setLogo("existing-logo.png");
            when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);

            BrandDTO savedDTO = new BrandDTO();
            savedDTO.setId(1L);
            savedDTO.setName("Samsung");
            savedDTO.setLogo("existing-logo.png");
            when(brandMapper.toBrandDTO(savedBrand)).thenReturn(savedDTO);
            when(awsS3Service.getImagePath(anyString(), any())).thenReturn("http://s3.url/logo.png");

            brandService.saveBrand(testBrandDTO, null);

            verify(brandRepository).findById(1L);
            assertThat(testBrandDTO.getLogo()).isEqualTo("existing-logo.png");
        }

        @Test
        @DisplayName("Should throw NotFoundException when updating brand not found for logo fetch")
        void shouldThrowNotFoundException_WhenUpdatingBrandNotFoundForLogoFetch() throws ConflictException, NotFoundException, IOException {
            when(brandRepository.findByName("Samsung")).thenReturn(Optional.of(testBrand));

            testBrandDTO.setLogo(null);
            when(brandRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.saveBrand(testBrandDTO, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Brand not found");

            verify(brandRepository, never()).save(any(Brand.class));
        }
    }

    // ============================= deleteBrand =============================

    @Nested
    @DisplayName("deleteBrand - Xóa thương hiệu")
    class DeleteBrandTests {

        @Test
        @DisplayName("Should delete brand and remove S3 folder successfully")
        void shouldDeleteBrand_AndRemoveS3Folder_Successfully() throws NotFoundException {
            when(brandRepository.findById(1L)).thenReturn(Optional.of(testBrand));

            brandService.deleteBrand(1L);

            verify(brandRepository).deleteById(1L);
            verify(awsS3Service).removeFolder("brand-logos/1/");
        }

        @Test
        @DisplayName("Should throw NotFoundException when brand does not exist")
        void shouldThrowNotFoundException_WhenBrandDoesNotExist() {
            when(brandRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.deleteBrand(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Could not find any brand with ID");

            verify(brandRepository, never()).deleteById(anyLong());
            verify(awsS3Service, never()).removeFolder(anyString());
        }
    }

    // ============================= getBrandByCategory =============================

    @Nested
    @DisplayName("getBrandByCategory - Lấy thương hiệu theo danh mục")
    class GetBrandByCategoryTests {

        @Test
        @DisplayName("Should return list of BrandDTOs for the given category")
        void shouldReturnListOfBrandDTOs_ForGivenCategory() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findAllByCategories(testCategory)).thenReturn(List.of(testBrand));
            when(brandMapper.toBrandDTO(testBrand)).thenReturn(testBrandDTO);

            List<BrandDTO> result = brandService.getBrandByCategory(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Samsung");
            verify(categoryRepository).findById(1L);
            verify(brandRepository).findAllByCategories(testCategory);
        }

        @Test
        @DisplayName("Should return empty list when category has no brands")
        void shouldReturnEmptyList_WhenCategoryHasNoBrands() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findAllByCategories(testCategory)).thenReturn(Collections.emptyList());

            List<BrandDTO> result = brandService.getBrandByCategory(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw NoSuchElementException when category not found")
        void shouldThrowNoSuchElementException_WhenCategoryNotFound() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.getBrandByCategory(999L))
                    .isInstanceOf(NoSuchElementException.class);

            verify(brandRepository, never()).findAllByCategories(any(Category.class));
        }
    }

    // ============================= getRecommendedBrands =============================

    @Nested
    @DisplayName("getRecommendedBrands - Lấy thương hiệu đề xuất cho khách hàng")
    class GetRecommendedBrandsTests {

        @Test
        @DisplayName("Should return distinct brands from search results limited to 7")
        void shouldReturnDistinctBrands_FromSearchResults_LimitedTo7() {
            List<Product> products = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Product p = new Product();
                Brand b = new Brand();
                b.setId((long) (i % 5) + 1); // 5 distinct brands
                b.setName("Brand " + ((i % 5) + 1));
                p.setBrand(b);
                products.add(p);
            }

            when(productRepository.searchProduct("phone")).thenReturn(products);
            when(brandMapper.toBrandDTO(any(Brand.class))).thenAnswer(invocation -> {
                Brand b = invocation.getArgument(0);
                BrandDTO dto = new BrandDTO();
                dto.setId(b.getId());
                dto.setName(b.getName());
                return dto;
            });

            List<BrandDTO> result = brandService.getRecommendedBrands("phone");

            assertThat(result).hasSize(5);
            verify(productRepository).searchProduct("phone");
        }

        @Test
        @DisplayName("Should limit recommended brands to maximum 7")
        void shouldLimitRecommendedBrands_ToMaximum7() {
            List<Product> products = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Product p = new Product();
                Brand b = new Brand();
                b.setId((long) i + 1);
                b.setName("Brand " + (i + 1));
                p.setBrand(b);
                products.add(p);
            }

            when(productRepository.searchProduct("phone")).thenReturn(products);
            when(brandMapper.toBrandDTO(any(Brand.class))).thenAnswer(invocation -> {
                Brand b = invocation.getArgument(0);
                BrandDTO dto = new BrandDTO();
                dto.setId(b.getId());
                dto.setName(b.getName());
                return dto;
            });

            List<BrandDTO> result = brandService.getRecommendedBrands("phone");

            assertThat(result).hasSize(7);
        }

        @Test
        @DisplayName("Should filter out products with null brand")
        void shouldFilterOutProducts_WithNullBrand() {
            Product productWithBrand = new Product();
            Brand brand = new Brand();
            brand.setId(1L);
            brand.setName("Samsung");
            productWithBrand.setBrand(brand);

            Product productWithoutBrand = new Product();
            productWithoutBrand.setBrand(null);

            when(productRepository.searchProduct("phone")).thenReturn(List.of(productWithBrand, productWithoutBrand));
            when(brandMapper.toBrandDTO(any(Brand.class))).thenAnswer(invocation -> {
                Brand b = invocation.getArgument(0);
                BrandDTO dto = new BrandDTO();
                dto.setId(b.getId());
                dto.setName(b.getName());
                return dto;
            });

            List<BrandDTO> result = brandService.getRecommendedBrands("phone");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Samsung");
        }

        @Test
        @DisplayName("Should return empty list when no products found")
        void shouldReturnEmptyList_WhenNoProductsFound() {
            when(productRepository.searchProduct("nonexistent")).thenReturn(Collections.emptyList());

            List<BrandDTO> result = brandService.getRecommendedBrands("nonexistent");

            assertThat(result).isEmpty();
            verify(brandMapper, never()).toBrandDTO(any(Brand.class));
        }

        @Test
        @DisplayName("Should return distinct brands when same brand appears multiple times")
        void shouldReturnDistinctBrands_WhenSameBrandAppearsMultipleTimes() {
            Brand samsung = new Brand();
            samsung.setId(1L);
            samsung.setName("Samsung");

            Product p1 = new Product();
            p1.setBrand(samsung);
            Product p2 = new Product();
            p2.setBrand(samsung);
            Product p3 = new Product();
            p3.setBrand(samsung);

            when(productRepository.searchProduct("samsung phone")).thenReturn(List.of(p1, p2, p3));
            when(brandMapper.toBrandDTO(any(Brand.class))).thenAnswer(invocation -> {
                Brand b = invocation.getArgument(0);
                BrandDTO dto = new BrandDTO();
                dto.setId(b.getId());
                dto.setName(b.getName());
                return dto;
            });

            List<BrandDTO> result = brandService.getRecommendedBrands("samsung phone");

            assertThat(result).hasSize(1);
        }
    }
}