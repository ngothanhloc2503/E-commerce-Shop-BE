package com.store.ecommerce.controller;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.response.PagedResponse;
import com.store.ecommerce.dto.wrapper.*;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.BrandService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import com.store.ecommerce.util.exporter.brand.BrandCsvExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/api/brands")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Brand", description = "APIs for managing product brands")
public class BrandController {
    private final BrandService brandService;

    private final AWSS3Service awsS3Service;

    @Operation(
            summary = "Get brands with pagination",
            description = "Retrieve brands with pagination, sorting and filtering support"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Brands retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedBrandWrapper.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<PagedResponse<BrandDTO>>> getBrandByPage(PagingAndSortingHelper helper) {
        PagedResponse<BrandDTO> data;

        if (helper.getPageSize() < 1) {
            List<BrandDTO> allBrands = brandService.getAllBrands(
                    helper.getKeyword(),
                    helper.getSortField(),
                    helper.getSortDir()
            );

            data = PagedResponse.<BrandDTO>builder()
                    .content(allBrands)
                    .totalPages(1)
                    .totalItems((long) allBrands.size())
                    .build();

        } else {
            Page<BrandDTO> page = brandService.getBrandByPage(helper);

            data = PagedResponse.<BrandDTO>builder()
                    .content(page.getContent())
                    .totalPages(page.getTotalPages())
                    .totalItems(page.getTotalElements())
                    .build();
        }

        return ResponseEntity.ok(
                ApiSuccessResponse.<PagedResponse<BrandDTO>>builder()
                        .success(true)
                        .message("Brands retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get all brands",
            description = "Retrieve all brands without pagination"
    )
    @ApiResponse(
            responseCode = "200",
            description = "All brands retrieved successfully",
            content = @Content(schema = @Schema(implementation = BrandListWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all")
    public ResponseEntity<ApiSuccessResponse<List<BrandDTO>>> getAllBrands() {

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<BrandDTO>>builder()
                        .success(true)
                        .message("All brands retrieved successfully")
                        .data(brandService.getAllBrands())
                        .build()
        );
    }

    @Operation(
            summary = "Get brand by ID",
            description = "Retrieve a specific brand by its ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Brand retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BrandWrapper.class))
            ),
            @ApiResponse(responseCode = "404", description = "Brand not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<BrandDTO>> getBrandById(@PathVariable("id") Long id) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<BrandDTO>builder()
                        .success(true)
                        .message("Brand retrieved successfully")
                        .data(brandService.getBrandById(id))
                        .build()
        );
    }

    @Operation(
            summary = "Get brands by category",
            description = "Retrieve brands associated with a specific category"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Brands retrieved successfully",
            content = @Content(schema = @Schema(implementation = BrandListWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/category/{categoryID}")
    public ResponseEntity<ApiSuccessResponse<List<BrandDTO>>> getBrandByCategory(
            @PathVariable("categoryID") Long categoryID) {
        return ResponseEntity.ok(
                ApiSuccessResponse.<List<BrandDTO>>builder()
                        .success(true)
                        .message("Brands retrieved successfully")
                        .data(brandService.getBrandByCategory(categoryID))
                        .build()
        );
    }

    @Operation(
            summary = "Create or update brand",
            description = "Create or update a brand with optional logo upload (multipart/form-data)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Brand saved successfully",
                    content = @Content(schema = @Schema(implementation = BrandWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<BrandDTO>> saveBrand(
            @Parameter(
                    description = "Brand data (JSON)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BrandDTO.class))
            )
            @RequestPart("brand") BrandDTO brandDTO,

            @Parameter(
                    description = "Brand logo image",
                    required = false
            )
            @RequestPart(name = "logo", required = false) MultipartFile logo
    ) throws IOException {

        BrandDTO savedBrand = brandService.saveBrand(brandDTO, logo);

        return ResponseEntity.ok(
                ApiSuccessResponse.<BrandDTO>builder()
                        .success(true)
                        .message("Brand saved successfully")
                        .data(savedBrand)
                        .build()
        );
    }

    @Operation(
            summary = "Check brand name uniqueness",
            description = "Check if brand name is unique"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Check result",
            content = @Content(schema = @Schema(implementation = BooleanWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/name-unique")
    public ResponseEntity<ApiSuccessResponse<Boolean>> checkNameUnique(
            @RequestParam("id") Long id,
            @RequestParam("name") String name) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<Boolean>builder()
                        .success(true)
                        .message("Check completed")
                        .data(brandService.isNameUnique(id, name))
                        .build()
        );
    }

    @Operation(
            summary = "Delete brand",
            description = "Delete a brand by ID"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Brand deleted successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteBrand(@PathVariable("id") Long id) {

        brandService.deleteBrand(id);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Brand deleted successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Export brands to CSV",
            description = "Export all brands to CSV file"
    )
    @ApiResponse(responseCode = "200", description = "CSV exported successfully")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/export/csv")
    public void exportToCsv(HttpServletResponse response) throws IOException {
        List<BrandDTO> listBrands = brandService.getAllBrands();
        BrandCsvExporter exporter = new BrandCsvExporter();

        exporter.export(response, listBrands);
    }
}
