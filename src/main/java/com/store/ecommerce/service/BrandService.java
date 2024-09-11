package com.store.ecommerce.service;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

public interface BrandService {
    List<BrandDTO> getAllBrands();

    List<BrandDTO> getAllBrands(String keyword, String sortField, String sortDir);

    Page<BrandDTO> getBrandByPage(PagingAndSortingHelper helper);

    BrandDTO getBrandById(Long id) throws NotFoundException;

    boolean isNameUnique(Long id, String name);

    BrandDTO saveBrand(BrandDTO brandDTO) throws Exception;

    void deleteBrand(Long id) throws NotFoundException;

    List<BrandDTO> getBrandByCategory(Long categoryID);

    List<BrandDTO> getRecommendedBrands(String keyword);
}
