package com.store.ecommerce.service;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.dto.response.CategoryListData;
import com.store.ecommerce.dto.response.PageResponse;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CategoryService {
    //For Staff
    CategoryListData getAllCategories();

    CategoryListData getAllCategories(String keyword, String sortField, String sortDir);

    PageResponse<CategoryDTO> getCategoriesByPage(PagingAndSortingHelper helper);

    CategoryDTO getCategoryById(Long id) throws NotFoundException;

    CategoryDTO save(CategoryDTO categoryDTO, MultipartFile image) throws ConflictException, NotFoundException, IOException;

    boolean isNameUnique(Long id, String name);

    void updateCategoryEnabledStatus(Long id, boolean status) throws NotFoundException;

    void delete(Long id) throws NotFoundException;

    //For Customer
    CategoryListData getAllCategoriesEnabled();

    CategoryDTO getCategoryByName(String name) throws NotFoundException;
}
