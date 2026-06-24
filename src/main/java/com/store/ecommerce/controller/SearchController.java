package com.store.ecommerce.controller;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.response.ApiErrorResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.PageResponse;
import com.store.ecommerce.dto.wrapper.BrandListWrapper;
import com.store.ecommerce.dto.wrapper.PagedProductWrapper;
import com.store.ecommerce.service.BrandService;
import com.store.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "APIs for searching products and related data")
public class SearchController {
    private final ProductService productService;
    private final BrandService brandService;

    @Operation(
            summary = "Search products",
            description = "Search products by keyword with optional filters (rating, brands, sorting)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedProductWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/products")
    public ResponseEntity<ApiSuccessResponse<PageResponse<ProductDTO>>> searchProduct(
            @Parameter(description = "Search keyword", example = "laptop")
            @RequestParam String keyword,

            @Parameter(description = "Page number (starting from 1)", example = "1")
            @RequestParam(defaultValue = "1") int pageNum,

            @Parameter(description = "Page size (starting from 1)", example = "1")
            @RequestParam(defaultValue = "24") int pageSize,

            @Parameter(description = "Sort field (e.g., price, averageRating)", example = "averageRating")
            @RequestParam(defaultValue = "averageRating") String sortField,

            @Parameter(description = "Minimum rating filter", example = "4")
            @RequestParam(defaultValue = "0") Float rating,

            @Parameter(description = "Filter by brand IDs")
            @RequestParam(required = false) Long[] brandIDs
    ) {

        Page<ProductDTO> page = productService.searchProduct(
                keyword, pageNum, pageSize, sortField, rating, brandIDs);

        PageResponse<ProductDTO> data = PageResponse.<ProductDTO>builder()
                .content(page.getContent())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements())
                .build();

        return ResponseEntity.ok(
                ApiSuccessResponse.<PageResponse<ProductDTO>>builder()
                        .success(true)
                        .message("Search results retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get recommended brands",
            description = "Suggest brands based on search keyword"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Recommended brands retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BrandListWrapper.class))
            )
    })
    @GetMapping("/recommended-brands")
    public ResponseEntity<ApiSuccessResponse<List<BrandDTO>>> getRecommendedBrands(
            @Parameter(description = "Search keyword", example = "nike")
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<BrandDTO>>builder()
                        .success(true)
                        .message("Recommended brands retrieved successfully")
                        .data(brandService.getRecommendedBrands(keyword))
                        .build()
        );
    }
}
