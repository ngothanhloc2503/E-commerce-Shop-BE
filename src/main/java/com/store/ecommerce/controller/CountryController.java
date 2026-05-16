package com.store.ecommerce.controller;

import com.store.ecommerce.dto.StateDTO;
import com.store.ecommerce.dto.request.CountryRequest;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.wrapper.CountryListWrapper;
import com.store.ecommerce.dto.wrapper.CountryWrapper;
import com.store.ecommerce.dto.wrapper.MessageResponseWrapper;
import com.store.ecommerce.dto.wrapper.StateListWrapper;
import com.store.ecommerce.entity.Country;
import com.store.ecommerce.service.CountryService;
import com.store.ecommerce.service.StateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
@Tag(name = "Country", description = "APIs for managing countries and states")
public class CountryController {
    private final CountryService countryService;
    private final StateService stateService;

    @Operation(
            summary = "Get all countries",
            description = "Retrieve all countries sorted by name"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Countries retrieved successfully",
            content = @Content(schema = @Schema(implementation = CountryListWrapper.class))
    )
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<List<Country>>> getAllCountries() {

        List<Country> countries = countryService.findAllByOrderByNameAsc();

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<Country>>builder()
                        .success(true)
                        .message("Countries retrieved successfully")
                        .data(countries)
                        .build()
        );
    }

    @Operation(
            summary = "Get states by country",
            description = "Retrieve list of states by country name"
    )
    @ApiResponse(
            responseCode = "200",
            description = "States retrieved successfully",
            content = @Content(schema = @Schema(implementation = StateListWrapper.class))
    )
    @GetMapping("/{countryName}/states")
    public ResponseEntity<ApiSuccessResponse<List<StateDTO>>> getListStatesByCountryName(
            @PathVariable("countryName") String countryName) {

        List<StateDTO> states = stateService.listStatesByCountryName(countryName);

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<StateDTO>>builder()
                        .success(true)
                        .message("States retrieved successfully")
                        .data(states)
                        .build()
        );
    }

    @Operation(
            summary = "Create or update country",
            description = "Create or update a country (Admin only)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Country saved successfully",
                    content = @Content(schema = @Schema(implementation = CountryWrapper.class))
            ),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Country>> saveCountry(
            @RequestBody CountryRequest request) {

        Country savedCountry = countryService.saveCountry(request);

        return ResponseEntity.ok(
                ApiSuccessResponse.<Country>builder()
                        .success(true)
                        .message("Country saved successfully")
                        .data(savedCountry)
                        .build()
        );
    }

    @Operation(
            summary = "Delete country",
            description = "Delete a country by ID (Admin only)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Country deleted successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteCountryByID(
            @PathVariable("id") Long id) {

        countryService.deleteByID(id);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Country deleted successfully")
                        .data(null)
                        .build()
        );
    }
}
