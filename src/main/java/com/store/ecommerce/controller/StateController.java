package com.store.ecommerce.controller;

import com.store.ecommerce.dto.StateDTO;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.wrapper.MessageResponseWrapper;
import com.store.ecommerce.dto.wrapper.StateWrapper;
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

@RestController
@RequestMapping("/api/states")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "States", description = "APIs for managing states")
public class StateController {
    private final StateService stateService;

    @Operation(
            summary = "Create or update state",
            description = "Save a state (create new or update existing)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "State saved successfully",
                    content = @Content(schema = @Schema(implementation = StateWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Country not found")
    })
    @PostMapping("")
    public ResponseEntity<ApiSuccessResponse<StateDTO>> saveState(
            @RequestPart(name = "state") StateDTO stateDTO) {

        StateDTO saved = stateService.saveState(stateDTO);

        return ResponseEntity.ok(
                ApiSuccessResponse.<StateDTO>builder()
                        .success(true)
                        .message("State saved successfully")
                        .data(saved)
                        .build()
        );
    }

    @Operation(
            summary = "Delete state",
            description = "Delete a state by ID"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "State deleted successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "404", description = "State not found")
    })
    @DeleteMapping("/{stateId}")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteStateByID(
            @PathVariable("stateId") Long id) {

        stateService.deleteStateByID(id);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("State deleted successfully")
                        .data(null)
                        .build()
        );
    }
}
