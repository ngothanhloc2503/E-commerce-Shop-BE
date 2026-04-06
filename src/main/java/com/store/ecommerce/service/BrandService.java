package com.store.ecommerce.service;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BrandService {
    List<BrandDTO> getAllBrands();

    List<BrandDTO> getAllBrands(String keyword, String sortField, String sortDir);

    Page<BrandDTO> getBrandByPage(PagingAndSortingHelper helper);

    BrandDTO getBrandById(Long id) throws NotFoundException;

    boolean isNameUnique(Long id, String name);

    BrandDTO saveBrand(BrandDTO brandDTO, MultipartFile logo) throws ConflictException, NotFoundException, IOException;

    void deleteBrand(Long id) throws NotFoundException;

    List<BrandDTO> getBrandByCategory(Long categoryID);

    List<BrandDTO> getRecommendedBrands(String keyword);
}
