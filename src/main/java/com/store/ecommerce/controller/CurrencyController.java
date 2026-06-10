package com.store.ecommerce.controller;

import com.store.ecommerce.dto.response.ApiErrorResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.wrapper.CurrencyListWrapper;
import com.store.ecommerce.entity.Currency;
import com.store.ecommerce.repository.CurrencyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/currencies")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Currency", description = "APIs for managing currencies")
public class CurrencyController {
    private final CurrencyRepository currencyRepository;

    @Operation(
            summary = "Get all currencies",
            description = "Retrieve all currencies sorted by name (Admin only)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Currencies retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CurrencyListWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<List<Currency>>> getAllCurrencies() {

        List<Currency> currencies = currencyRepository.findAllByOrderByNameAsc();

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<Currency>>builder()
                        .success(true)
                        .message("Currencies retrieved successfully")
                        .data(currencies)
                        .build()
        );
    }
}
