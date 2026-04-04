package com.store.ecommerce.service;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.util.PagingAndSortingHelper;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {
    // For Staff
    ProductDTO getProductByID(Long id) throws NotFoundException;

    List<ProductDTO> getAllProducts(Long categoryID);

    List<ProductDTO> getAllProducts(Long categoryID, String keyword, String sortField, String sortDir);

    Page<ProductDTO> getProductByPage(PagingAndSortingHelper helper, Long categoryID);

    void changeEnabledStatus(Long id, boolean status) throws NotFoundException;

    ProductDTO saveProduct(ProductDTO productDTO,
                           MultipartFile mainImageFile,
                           MultipartFile[] extrasImagesFile) throws ConflictException, IOException;

    boolean isNameUnique(Long id, String name);

    void deleteProduct(Long id) throws NotFoundException;

    // For Customer
    List<ProductDTO> getProductForHomePage();

    ProductDTO getProductByAlias(String alias) throws NotFoundException;

    Page<ProductDTO> getProductByCategoryName(String categoryName, int pageNum);

    Page<ProductDTO> searchProduct(String keyword, int pageNum, String sortField, Float rating, Long[] brandIDs);
}
