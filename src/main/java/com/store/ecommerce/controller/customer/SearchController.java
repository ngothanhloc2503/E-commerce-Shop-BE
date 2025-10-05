package com.store.ecommerce.controller.customer;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.response.PagedResponseDTO;
import com.store.ecommerce.service.BrandService;
import com.store.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("SearchController")
@RequestMapping("/api/customer/search")
@RequiredArgsConstructor
public class SearchController {
    private final ProductService productService;
    private final BrandService brandService;

    @GetMapping("/products")
    public ResponseEntity<?> searchProduct(
            @RequestParam(name = "keyword") String keyword,
            @RequestParam(name = "pageNum") int pageNum,
            @RequestParam(name = "sortField", defaultValue = "averageRating") String sortField,
            @RequestParam(name = "rating", defaultValue = "0") float rating,
            @RequestParam(name = "brandIDs", required = false) long[] brandIDs) {
        Page<ProductDTO> page = productService.searchProduct(keyword, pageNum, sortField, rating, brandIDs);

        return ResponseEntity.ok(PagedResponseDTO.builder()
                .content(page.getContent())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements()).build());
    }

    @GetMapping("/recommended-brands")
    public ResponseEntity<?> getRecommendedBrands(
            @RequestParam(name = "keyword") String keyword) {
        return ResponseEntity.ok(brandService.getRecommendedBrands(keyword));
    }
}
