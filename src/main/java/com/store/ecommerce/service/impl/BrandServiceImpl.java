package com.store.ecommerce.service.impl;

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
import com.store.ecommerce.service.BrandService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandMapper brandMapper;
    private final AWSS3Service awsS3Service;

    @Override
    public List<BrandDTO> getAllBrands() {
        List<BrandDTO> listBrandDTOs = brandRepository.findAll().stream()
                .map(brandMapper::toBrandDTO)
                .toList();
        listBrandDTOs.forEach(this::setLogoImagePath);
        return listBrandDTOs;
    }

    @Override
    public List<BrandDTO> getAllBrands(String keyword, String sortField, String sortDir) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        List<BrandDTO> listBrandDTOs = brandRepository.searchByKeyword(keyword, sort)
                .stream().map(brandMapper::toBrandDTO).toList();
        listBrandDTOs.forEach(this::setLogoImagePath);
        return listBrandDTOs;
    }

    @Override
    public Page<BrandDTO> getBrandByPage(PagingAndSortingHelper helper) {
        Sort sort = helper.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(helper.getSortField()).ascending()
                : Sort.by(helper.getSortField()).descending();

        Pageable pageable = PageRequest.of(helper.getPageNum() - 1, helper.getPageSize(), sort);

        Page<Brand> pageBrands;
        if (StringUtils.hasText(helper.getKeyword())) {
            pageBrands = brandRepository.searchByKeyword(helper.getKeyword(), pageable);
        } else {
            pageBrands = brandRepository.findAll(pageable);
        }

        return pageBrands.map(brandMapper::toBrandDTO)
                .map(this::setLogoImagePath);
    }

    @Override
    @Transactional
    public BrandDTO getBrandById(Long id) throws NotFoundException {
        Brand brand = brandRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Could not find any brand with ID: " + id));

        BrandDTO brandDTO = brandMapper.toBrandDTO(brand);
        setLogoImagePath(brandDTO);
        return brandDTO;
    }

    @Override
    public boolean isNameUnique(Long id, String name) {
        Optional<Brand> brand = brandRepository.findByName(name);

        return brand.isEmpty() || (id != null && Objects.equals(brand.get().getId(), id));
    }

    @Override
    public BrandDTO saveBrand(BrandDTO brandDTO, MultipartFile logo) throws ConflictException, NotFoundException, IOException {
        if (!isNameUnique(brandDTO.getId(), brandDTO.getName())) {
            throw new ConflictException("Brand name already exists!");
        }

        boolean isUpdating = (brandDTO.getId() != null);
        String fileName = null;

        if (!isFileNullOrEmpty(logo)) {
            fileName = UUID.randomUUID() + "_" + Objects.requireNonNull(logo.getOriginalFilename());
            brandDTO.setLogo(fileName);
        } else if (isUpdating) {
            Brand existingBrand = brandRepository.findById(brandDTO.getId())
                    .orElseThrow(() -> new NotFoundException("Brand not found"));
            fileName = existingBrand.getLogo();
            brandDTO.setLogo(existingBrand.getLogo());
        } else {
            brandDTO.setLogo(null);
        }

        Brand brand = brandMapper.toBrand(brandDTO);

        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(brandDTO.getListCategoryIDs()));
        brand.setCategories(categories);

        Brand savedBrandEntity = brandRepository.save(brand);
        BrandDTO savedBrandDTO = brandMapper.toBrandDTO(savedBrandEntity);

        // Upload logo lên S3 nếu có file mới
        if (!isFileNullOrEmpty(logo)) {
            String uploadDir = "brand-logos/" + savedBrandDTO.getId();
            awsS3Service.removeFolder(uploadDir + "/");

            awsS3Service.uploadFile(uploadDir, fileName,
                    logo.getInputStream(), logo.getSize(), logo.getContentType());
        }

        return setLogoImagePath(savedBrandDTO);
    }

    @Override
    public void deleteBrand(Long id) throws NotFoundException {
        if (!brandRepository.existsById(id)) {
            throw new NotFoundException("Could not find any brand with ID: " + id);
        }

        brandRepository.deleteById(id);

        String dir = "brand-logos/" + id;
        awsS3Service.removeFolder(dir + "/");
    }

    @Override
    @Transactional
    public List<BrandDTO> getBrandByCategory(Long categoryID) {
        Category category = categoryRepository.findById(categoryID).orElseThrow();
        return brandRepository.findAllByCategories(category).stream()
                .map(brandMapper::toBrandDTO)
                .toList();
    }

    // For Customer - Search Controller
    @Override
    @Transactional
    public List<BrandDTO> getRecommendedBrands(String keyword) {
        List<Product> products = productRepository.searchProduct(keyword);
        return products.stream()
                .map(Product::getBrand)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Brand::getId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values().stream()
                .limit(7)
                .map(brandMapper::toBrandDTO)
                .toList();
    }

    private BrandDTO setLogoImagePath(BrandDTO brandDTO) {
        brandDTO.setLogoImagePath(awsS3Service.getImagePath("brand-logos/" + brandDTO.getId(),
                brandDTO.getLogo()));
        return brandDTO;
    }
}