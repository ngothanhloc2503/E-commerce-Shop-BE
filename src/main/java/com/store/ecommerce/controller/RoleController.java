package com.store.ecommerce.controller;

import com.store.ecommerce.dto.response.ApiErrorResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.wrapper.RoleListWrapper;
import com.store.ecommerce.entity.Role;
import com.store.ecommerce.service.RoleService;
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
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "APIs for managing user roles")
public class RoleController {
    private final RoleService roleService;

    @Operation(
            summary = "Get all roles",
            description = "Retrieve all available roles (Admin only)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Roles retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoleListWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<List<Role>>> getAllRoles() {

        List<Role> roles = roleService.getAllRole();

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<Role>>builder()
                        .success(true)
                        .message("Roles retrieved successfully")
                        .data(roles)
                        .build()
        );
    }
}
