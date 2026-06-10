package com.store.ecommerce.controller;

import com.store.ecommerce.dto.request.CodSupportRequest;
import com.store.ecommerce.dto.response.ApiErrorResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.response.PageResponse;
import com.store.ecommerce.dto.wrapper.MessageResponseWrapper;
import com.store.ecommerce.dto.wrapper.PagedShippingRateWrapper;
import com.store.ecommerce.dto.wrapper.ShippingRateWrapper;
import com.store.ecommerce.entity.ShippingRate;
import com.store.ecommerce.service.ShippingRateService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping-rates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Shipping Rates", description = "APIs for managing shipping rates")
public class ShippingRateController {
    private final ShippingRateService shippingRateService;

    @Operation(
            summary = "Get shipping rates (paginated)",
            description = "Retrieve shipping rates with pagination, sorting and search"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "Shipping rates retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedShippingRateWrapper.class))
    )
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<PageResponse<ShippingRate>>> getShippingRatesByPage(
            PagingAndSortingHelper helper) {
        PageResponse<ShippingRate> data;

        if (helper.getPageSize() < 1) {
            List<ShippingRate> shippingRates =
                    shippingRateService.getAllShippingRates(
                            helper.getKeyword(),
                            helper.getSortField(),
                            helper.getSortDir()
                    );

            data = PageResponse.<ShippingRate>builder()
                    .content(shippingRates)
                    .totalPages(1)
                    .totalItems((long) shippingRates.size())
                    .build();
        } else {
            Page<ShippingRate> page = shippingRateService.getShippingRatesByPage(helper);

            data = PageResponse.<ShippingRate>builder()
                    .content(page.getContent())
                    .totalPages(page.getTotalPages())
                    .totalItems(page.getTotalElements())
                    .build();
        }

        return ResponseEntity.ok(
                ApiSuccessResponse.<PageResponse<ShippingRate>>builder()
                        .success(true)
                        .message("Shipping rates retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get shipping rate by ID",
            description = "Retrieve shipping rate details by ID"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Shipping rate retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ShippingRateWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Shipping rate not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ShippingRate>> getShippingRateById(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<ShippingRate>builder()
                        .success(true)
                        .message("Shipping rate retrieved successfully")
                        .data(shippingRateService.getShippingRateById(id))
                        .build()
        );
    }

    @Operation(
            summary = "Create or update shipping rate",
            description = "Save a shipping rate (create or update)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Shipping rate saved successfully",
                    content = @Content(schema = @Schema(implementation = ShippingRateWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Related resource not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping("")
    public ResponseEntity<ApiSuccessResponse<ShippingRate>> saveShippingRate(
            @RequestBody ShippingRate shippingRate) {

        ShippingRate saved = shippingRateService.saveShippingRate(shippingRate);

        return ResponseEntity.ok(
                ApiSuccessResponse.<ShippingRate>builder()
                        .success(true)
                        .message("Shipping rate saved successfully")
                        .data(saved)
                        .build()
        );
    }

    @Operation(
            summary = "Update COD support",
            description = "Enable or disable Cash On Delivery (COD) support for a shipping rate"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "COD support updated successfully",
            content = @Content(schema = @Schema(implementation = ShippingRateWrapper.class))
    )
    @PatchMapping("/{id}/cod")
    public ResponseEntity<ApiSuccessResponse<ShippingRate>> updateCodSupport(
            @PathVariable("id") Long id,
            @RequestBody @Valid CodSupportRequest request) {

            shippingRateService.updateCodSupported(id, request.isSupported());

        return ResponseEntity.ok(
                ApiSuccessResponse.<ShippingRate>builder()
                        .success(true)
                        .message("COD support updated successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Delete shipping rate",
            description = "Delete shipping rate by ID"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "Shipping rate deleted successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteShippingRate(
            @PathVariable(name = "id") Long id) {

        shippingRateService.deleteShippingRate(id);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Shipping rate deleted successfully")
                        .data(null)
                        .build()
        );
    }
}
