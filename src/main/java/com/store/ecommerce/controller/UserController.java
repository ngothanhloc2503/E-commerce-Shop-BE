package com.store.ecommerce.controller;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.dto.request.UserRequest;
import com.store.ecommerce.dto.request.UserStatusRequest;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.response.PagedResponse;
import com.store.ecommerce.dto.wrapper.*;
import com.store.ecommerce.service.UserService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import com.store.ecommerce.util.exporter.user.UserCsvExporter;
import com.store.ecommerce.util.exporter.user.UserExcelExporter;
import com.store.ecommerce.util.exporter.user.UserPdfExporter;
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
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Users", description = "APIs for managing users")
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "Get users (paginated)",
            description = "Retrieve users with pagination, filtering and sorting"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedUserWrapper.class))
    )
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<PagedResponse<UserDTO>>> getUsersByPage(
            PagingAndSortingHelper helper) {

        PagedResponse<UserDTO> data;

        if (helper.getPageSize() < 1) {
            List<UserDTO> users = userService.getAllUsers(
                    helper.getKeyword(),
                    helper.getSortField(),
                    helper.getSortDir()
            );

            data = PagedResponse.<UserDTO>builder()
                    .content(users)
                    .totalPages(1)
                    .totalItems((long) users.size())
                    .build();
        } else {
            Page<UserDTO> page = userService.getUsersByPage(helper);

            data = PagedResponse.<UserDTO>builder()
                    .content(page.getContent())
                    .totalPages(page.getTotalPages())
                    .totalItems(page.getTotalElements())
                    .build();
        }

        return ResponseEntity.ok(
                ApiSuccessResponse.<PagedResponse<UserDTO>>builder()
                        .success(true)
                        .message("Users retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get all users",
            description = "Retrieve all users without pagination"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserListWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all")
    public ResponseEntity<ApiSuccessResponse<List<UserDTO>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiSuccessResponse.<List<UserDTO>>builder()
                        .success(true)
                        .message("Users retrieved successfully")
                        .data(userService.getAllUsers())
                        .build()
        );
    }

    @Operation(
            summary = "Get user by ID",
            description = "Retrieve user details by ID"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserWrapper.class))

            ),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<UserDTO>> getUserById(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<UserDTO>builder()
                        .success(true)
                        .message("User retrieved successfully")
                        .data(userService.getUserById(id))
                        .build()
        );
    }

    @Operation(
            summary = "Create or update user",
            description = "Create or update user with optional profile photo"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "User saved successfully",
            content = @Content(schema = @Schema(implementation = UserWrapper.class))
    )
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<UserDTO>> saveUser(
            @RequestPart("user") @Valid UserRequest userDTO,
            @RequestPart(name = "filePhoto", required = false) MultipartFile photo
    ) throws IOException {

        UserDTO savedUser = userService.saveUser(userDTO, photo);

        return ResponseEntity.ok(
                ApiSuccessResponse.<UserDTO>builder()
                        .success(true)
                        .message("User saved successfully")
                        .data(savedUser)
                        .build()
        );
    }

    @Operation(
            summary = "Delete user",
            description = "Delete user by ID"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(
            responseCode = "200",
            description = "User deleted successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteUser(@PathVariable("id") Long id) {

        userService.delete(id);

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("User deleted successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Check email uniqueness",
            description = "Check if email is unique for user"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Check result",
            content = @Content(schema = @Schema(implementation = BooleanWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/email-unique")
    public ResponseEntity<ApiSuccessResponse<Boolean>> checkUniqueEmail(
            @RequestParam("id") Long id,
            @RequestParam("email") String email) {

        return ResponseEntity.ok(
                ApiSuccessResponse.<Boolean>builder()
                        .success(true)
                        .message("Check completed")
                        .data(userService.isEmailUnique(id, email))
                        .build()
        );
    }

    @Operation(
            summary = "Update user status",
            description = "Enable or disable user account"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status updated successfully",
            content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/enabled")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> updateUserEnabledStatus(
            @PathVariable("id") Long id,
            @RequestBody @Valid UserStatusRequest request) {

        userService.updateUserEnabledStatus(id, request.getStatus());

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("User status updated successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Export users to CSV",
            description = "Export all users to CSV file"
    )
    @ApiResponse(responseCode = "200", description = "CSV exported successfully")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/export/csv")
    public void exportToCsv(HttpServletResponse response) throws IOException {
        List<UserDTO> listUsers = userService.getAllUsers();
        UserCsvExporter exporter = new UserCsvExporter();
        
        exporter.export(response, listUsers);
    }

    @Operation(
            summary = "Export users to Excel",
            description = "Export all users to Excel file"
    )
    @ApiResponse(responseCode = "200", description = "Excel file exported successfully")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/export/excel")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        List<UserDTO> listUsers = userService.getAllUsers();
        UserExcelExporter exporter = new UserExcelExporter();

        exporter.export(response, listUsers);
    }

    @Operation(
            summary = "Export users to PDF",
            description = "Export all users to PDF file"
    )
    @ApiResponse(responseCode = "200", description = "PDF exported successfully")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/export/pdf")
    public void exportToPdf(HttpServletResponse response) throws IOException {
        List<UserDTO> listUsers = userService.getAllUsers();
        UserPdfExporter exporter = new UserPdfExporter();

        exporter.export(response, listUsers);
    }
}
