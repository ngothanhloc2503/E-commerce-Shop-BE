package com.store.ecommerce.controller;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.request.ProductStatusRequest;
import com.store.ecommerce.dto.response.ApiErrorResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.response.PageResponse;
import com.store.ecommerce.dto.wrapper.*;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.ProductService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import com.store.ecommerce.util.exporter.product.ProductCsvExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "APIs for managing and retrieving products")
public class ProductController {

    private final ProductService productService;

    private final AWSS3Service awsS3Service;

    @Operation(
            summary = "Get products for homepage",
            description = "Retrieve featured products for homepage display"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProductListWrapper.class))
    )
    @GetMapping("/home")
    public ResponseEntity<ApiSuccessResponse<List<ProductDTO>>> getProductForHomePage() {
        return ResponseEntity.ok(
                ApiSuccessResponse.<List<ProductDTO>>builder()
                        .success(true)
                        .message("Products retrieved successfully")
                        .data(productService.getProductForHomePage().getProducts())
                        .build()
        );
    }

    @Operation(
            summary = "Get product by alias",
            description = "Retrieve product details using SEO-friendly alias"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/alias/{alias}")
    public ResponseEntity<ApiSuccessResponse<ProductDTO>> getProductByAlias(
            @PathVariable("alias") String alias) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<ProductDTO>builder()
                        .success(true)
                        .message("Product retrieved successfully")
                        .data(productService.getProductByAlias(alias))
                        .build()
        );
    }

    @Operation(
            summary = "Get products by category",
            description = "Retrieve paginated products by category name"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedProductWrapper.class))
    )
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<ApiSuccessResponse<PageResponse<ProductDTO>>> getProductByCategoryName(
            @PathVariable("categoryName") String categoryName,
            @RequestParam("pageNum") int pageNum) {

        Page<ProductDTO> page = productService.getProductByCategoryName(categoryName, pageNum);

        PageResponse<ProductDTO> data = PageResponse.<ProductDTO>builder()
                .content(page.getContent())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements())
                .build();
        return ResponseEntity.ok(
                ApiSuccessResponse.<PageResponse<ProductDTO>>builder()
                        .success(true)
                        .message("Products retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get products (Admin)",
            description = "Retrieve paginated products with filtering, sorting, and category filter"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedProductWrapper.class))
    )
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<PageResponse<ProductDTO>>> getProductByPage(
            PagingAndSortingHelper helper,
            @RequestParam("categoryID") Long categoryID) {

        PageResponse<ProductDTO> data;

        if (helper.getPageSize() < 1) {
            List<ProductDTO> allProducts = productService.getAllProducts(
                    categoryID,
                    helper.getKeyword(),
                    helper.getSortField(),
                    helper.getSortDir()
            );

            data = PageResponse.<ProductDTO>builder()
                    .content(allProducts)
                    .totalPages(1)
                    .totalItems((long) allProducts.size())
                    .build();
        } else {
            data = productService.getProductByPage(helper, categoryID);
        }

        return ResponseEntity.ok(
                ApiSuccessResponse.<PageResponse<ProductDTO>>builder()
                        .success(true)
                        .message("Products retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get product by ID (Admin)",
            description = "Retrieve product details by ID (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Product retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProductWrapper.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<ProductDTO>> getProductByID(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<ProductDTO>builder()
                        .success(true)
                        .message("Product retrieved successfully")
                        .data(productService.getProductByID(id))
                        .build()
        );
    }

    @Operation(
            summary = "Get all products (Admin)",
            description = "Retrieve all products without pagination (Admin only)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "All products retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProductListWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<List<ProductDTO>>> getAllProducts() {
        return ResponseEntity.ok(
                ApiSuccessResponse.<List<ProductDTO>>builder()
                        .success(true)
                        .message("Products retrieved successfully")
                        .data(productService.getAllProducts(0L))
                        .build()
        );
    }

    @Operation(
            summary = "Update product enabled status",
            description = "Enable or disable a product"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "Status updated successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
    )
    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> changeEnabledStatus(
            @PathVariable("id") Long id,
            @RequestBody @Valid ProductStatusRequest request) {

        productService.changeEnabledStatus(id, request.getStatus());

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Status updated successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Check product name uniqueness",
            description = "Check if product name is unique"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Check result",
            content = @Content(schema = @Schema(implementation = BooleanWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/name-unique")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Boolean>> checkNameUnique(@RequestParam("id") Long id,
                                                                       @RequestParam("name") String name) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<Boolean>builder()
                        .success(true)
                        .message("Check completed")
                        .data(productService.isNameUnique(id, name))
                        .build()
        );
    }

    @Operation(
            summary = "Create or update product",
            description = "Create or update product with images"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "Product saved successfully",
            content = @Content(schema = @Schema(implementation = ProductWrapper.class))
    )
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<ProductDTO>> saveProduct(
            @RequestPart("product") ProductDTO productDTO,
            @RequestPart(name = "mainImageFile", required = false) MultipartFile mainImageFile,
            @RequestPart(name = "extrasImagesFile", required = false) MultipartFile[] extrasImagesFile)
            throws IOException {

        ProductDTO savedProduct = productService.saveProduct(productDTO, mainImageFile, extrasImagesFile);

        return ResponseEntity.ok(
                ApiSuccessResponse.<ProductDTO>builder()
                        .success(true)
                        .message("Product saved successfully")
                        .data(savedProduct)
                        .build()
        );
    }

    @Operation(
            summary = "Delete product",
            description = "Delete product and its images"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Product deleted successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteProduct(@PathVariable("id") Long id) {
        productService.deleteProduct(id);
        awsS3Service.removeFolder("product-images/" + id + "/");

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Product deleted successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Export products to CSV",
            description = "Export all products to CSV file"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "CSV exported successfully")
    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportToCsv(HttpServletResponse response) throws IOException {
        List<ProductDTO> listProducts = productService.getAllProducts(0L);
        ProductCsvExporter exporter = new ProductCsvExporter();

        exporter.export(response, listProducts);
    }
}
