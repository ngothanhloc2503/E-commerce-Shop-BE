package com.store.ecommerce.controller;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.dto.request.CategoryStatusRequest;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.response.PagedResponse;
import com.store.ecommerce.dto.wrapper.*;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.CategoryService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import com.store.ecommerce.util.exporter.category.CategoryCsvExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "APIs for managing product categories")
public class CategoryController {

    private final CategoryService categoryService;

    private final AWSS3Service awsS3Service;

    @Operation(
            summary = "Get all categories",
            description = "Retrieve all categories without pagination"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Categories retrieved successfully",
            content = @Content(schema = @Schema(implementation = CategoryListWrapper.class))
    )
    @GetMapping("/all")
    public ResponseEntity<ApiSuccessResponse<List<CategoryDTO>>> getAllCategories() {

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<CategoryDTO>>builder()
                        .success(true)
                        .message("Categories retrieved successfully")
                        .data(categoryService.getAllCategories())
                        .build()
        );
    }

    @Operation(
            summary = "Get categories with pagination",
            description = "Retrieve categories with pagination, filtering and sorting"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Categories retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedCategoryWrapper.class))
    )
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<PagedResponse<CategoryDTO>>> getCategoryByPage(
            PagingAndSortingHelper helper) {
        PagedResponse<CategoryDTO> data;

        if (helper.getPageSize() < 1) {
            List<CategoryDTO> categories = categoryService.getAllCategories(
                    helper.getKeyword(),
                    helper.getSortField(),
                    helper.getSortDir()
            );

            data = PagedResponse.<CategoryDTO>builder()
                    .content(categories)
                    .totalPages(1)
                    .totalItems((long) categories.size())
                    .build();

        } else {
            Page<CategoryDTO> page = categoryService.getCategoriesByPage(helper);

            data = PagedResponse.<CategoryDTO>builder()
                    .content(page.getContent())
                    .totalPages(page.getTotalPages())
                    .totalItems(page.getTotalElements())
                    .build();
        }

        return ResponseEntity.ok(
                ApiSuccessResponse.<PagedResponse<CategoryDTO>>builder()
                        .success(true)
                        .message("Categories retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get category by name",
            description = "Retrieve category using its name"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Category retrieved successfully",
            content = @Content(schema = @Schema(implementation = CategoryWrapper.class))
    )
    @GetMapping("/by-name/{name}")
    public ResponseEntity<ApiSuccessResponse<CategoryDTO>> getCategoryByName(
            @PathVariable("name") String name) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<CategoryDTO>builder()
                        .success(true)
                        .message("Category retrieved successfully")
                        .data(categoryService.getCategoryByName(name))
                        .build()
        );
    }

    @Operation(
            summary = "Get category by ID",
            description = "Retrieve a category by ID (Admin only)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Category retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CategoryWrapper.class))
            ),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<CategoryDTO>> getCategoryById(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<CategoryDTO>builder()
                        .success(true)
                        .message("Category retrieved successfully")
                        .data(categoryService.getCategoryById(id))
                        .build()
        );
    }

    @Operation(
            summary = "Check category name uniqueness",
            description = "Check if category name is unique (Admin only)"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Check result",
            content = @Content(schema = @Schema(implementation = BooleanWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/name-unique")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Boolean>> checkUniqueName(
            @RequestParam("id") Long id,
            @RequestParam("name") String name) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<Boolean>builder()
                        .success(true)
                        .message("Check completed")
                        .data(categoryService.isNameUnique(id, name))
                        .build()
        );
    }

    @Operation(
            summary = "Create or update category",
            description = "Create or update category with optional image upload"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Category saved successfully",
            content = @Content(schema = @Schema(implementation = CategoryWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<CategoryDTO>> saveCategory(
            @Parameter(
                    description = "Category data (JSON)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CategoryDTO.class))
            )
            @RequestPart("category") CategoryDTO categoryDTO,

            @Parameter(description = "Category image")
            @RequestPart(name = "image", required = false) MultipartFile image

    ) throws IOException {

        CategoryDTO savedCategory = categoryService.save(categoryDTO, image);

        return ResponseEntity.ok(
                ApiSuccessResponse.<CategoryDTO>builder()
                        .success(true)
                        .message("Category saved successfully")
                        .data(savedCategory)
                        .build()
        );
    }

    @Operation(
            summary = "Update category status",
            description = "Enable or disable a category"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status updated successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> updateCategoryEnabledStatus(
            @PathVariable("id") Long id,
            @RequestBody @Valid CategoryStatusRequest request) {

        categoryService.updateCategoryEnabledStatus(id, request.getStatus());

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Category status updated successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Delete category",
            description = "Delete a category by ID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Category deleted successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteCategoryById(
            @PathVariable("id") Long id) {

        categoryService.delete(id);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Category deleted successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Export categories to CSV",
            description = "Export all categories to CSV file"
    )
    @ApiResponse(responseCode = "200", description = "CSV exported successfully")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportToCsv(HttpServletResponse response) throws IOException {

        List<CategoryDTO> listCategories = categoryService.getAllCategories();
        CategoryCsvExporter exporter = new CategoryCsvExporter();

        exporter.export(response, listCategories);
    }
}
