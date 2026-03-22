package com.store.ecommerce.service.impl;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import com.store.ecommerce.entity.Product;
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

import java.util.*;

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
        List<BrandDTO> listBrandDTOs = brandRepository.findAll().stream().map(brandMapper::toBrandDTO).toList();
        listBrandDTOs.forEach(this::setLogoImagePath);
        return listBrandDTOs;
    }

    @Override
    public List<BrandDTO> getAllBrands(String keyword, String sortField, String sortDir) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        List<BrandDTO> listBrandDTOs = brandRepository.findAll(keyword, sort)
                .stream().map(brandMapper::toBrandDTO).toList();
        listBrandDTOs.forEach(this::setLogoImagePath);
        return listBrandDTOs;
    }

    @Override
    public Page<BrandDTO> getBrandByPage(PagingAndSortingHelper helper) {
        Pageable pageable = PageRequest.of(helper.getPageNum() - 1, helper.getPageSize(),
                helper.getSortDir().equalsIgnoreCase("asc") ? Sort.by(helper.getSortField()).ascending() : Sort.by(helper.getSortField()).descending());

        Page<Brand> pageBrands;
        if (helper.getKeyword() != null && !helper.getKeyword().isBlank()) {
            pageBrands = brandRepository.findAll(helper.getKeyword(), pageable);
        } else {
            pageBrands = brandRepository.findAll(pageable);
        }

        Page<BrandDTO> pageBrandDTOs = pageBrands.map(brandMapper::toBrandDTO);
        pageBrandDTOs.map(this::setLogoImagePath);
        return pageBrandDTOs;
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

        if (brand.isEmpty()) return true;

        return id != null && Objects.equals(brand.get().getId(), id);
    }

    @Override
    public BrandDTO saveBrand(BrandDTO brandDTO) throws IllegalArgumentException, NotFoundException {
        boolean isUpdating = (brandDTO.getId() != null);
        if (!isNameUnique(brandDTO.getId(), brandDTO.getName())) {
            throw new IllegalArgumentException("Brand name already exists!");
        }

        if (isUpdating) {
            if (brandDTO.getLogo() == null) {
                Brand saved = brandRepository.findById(brandDTO.getId()).orElseThrow(
                        () -> new NotFoundException("Brand not found")
                );
                brandDTO.setLogo(saved.getLogo());
            }
        }

        Brand brand = brandMapper.toBrand(brandDTO);
        Set<Category> categories = new HashSet<>();
        brandDTO.getListCategoryIDs().forEach(catID -> categories.add(new Category(catID)));
        brand.setCategories(categories);

        return brandMapper.toBrandDTO(brandRepository.save(brand));
    }

    @Override
    public void deleteBrand(Long id) throws NotFoundException {
        if (brandRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Could not find any brand with ID: " + id);
        }

        brandRepository.deleteById(id);
    }

    @Override
    public List<BrandDTO> getBrandByCategory(Long categoryID) {
        Category category = categoryRepository.findById(categoryID).orElseThrow();
        return brandRepository.findAllByCategories(category).stream().map(brandMapper::toBrandDTO).toList();
    }

    // For Customer - Search Controller
    @Override
    @Transactional
    public List<BrandDTO> getRecommendedBrands(String keyword) {
        return productRepository.searchProduct(keyword).stream()
                .map(Product::getBrand)
                .filter(Objects::nonNull)
                .distinct()
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
