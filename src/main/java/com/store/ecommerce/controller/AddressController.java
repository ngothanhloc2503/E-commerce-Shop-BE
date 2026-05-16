package com.store.ecommerce.controller;

import com.store.ecommerce.dto.response.AddressBookResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.response.MessageResponse;
import com.store.ecommerce.dto.wrapper.AddressBookWrapper;
import com.store.ecommerce.dto.wrapper.AddressWrapper;
import com.store.ecommerce.dto.wrapper.MessageResponseWrapper;
import com.store.ecommerce.entity.Address;
import com.store.ecommerce.service.AddressService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address-book")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
@Tag(name = "Address Book", description = "APIs for managing user address book")
public class AddressController {
    private final AddressService addressService;

    @Operation(
            summary = "Get address book",
            description = "Retrieve all addresses of the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Address book retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AddressBookWrapper.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("")
    public ResponseEntity<ApiSuccessResponse<AddressBookResponse>> getAddressBook(
            Authentication authentication) {

        List<Address> listAddresses = null;
        listAddresses = addressService.listAddressBook(authentication.getName());


        boolean primaryAddressAsDefault = true;
        for (Address address : listAddresses) {
            if (address.isDefaultForShipping()) {
                primaryAddressAsDefault = false;
                break;
            }
        }

        AddressBookResponse data = AddressBookResponse.builder()
                .addressBook(listAddresses)
                .primaryAddressAsDefault(primaryAddressAsDefault)
                .build();

        return ResponseEntity.ok(
                ApiSuccessResponse.<AddressBookResponse>builder()
                        .success(true)
                        .message("Address book retrieved successfully")
                        .data(data)
                        .build()
        );
    }

    @Operation(
            summary = "Get default address",
            description = "Retrieve the default shipping address of the user"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Default address retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AddressWrapper.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/default")
    public ResponseEntity<ApiSuccessResponse<Address>> getDefaultAddress(Authentication authentication) {

        Address address = addressService.getDefaultAddress(authentication.getName());

        return ResponseEntity.ok(
                ApiSuccessResponse.<Address>builder()
                        .success(true)
                        .message("Default address retrieved successfully")
                        .data(address)
                        .build()
        );
    }

    @Operation(
            summary = "Get address by ID",
            description = "Retrieve a specific address by ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Address retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AddressWrapper.class))
            ),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Address>> getAddressById(
            Authentication authentication,
            @PathVariable("id") Long id) {

        Address address = addressService.getByIdAndUserEmail(id, authentication.getName());

        return ResponseEntity.ok(
                ApiSuccessResponse.<Address>builder()
                        .success(true)
                        .message("Address retrieved successfully")
                        .data(address)
                        .build()
        );
    }

    @Operation(
            summary = "Create or update address",
            description = "Create a new address or update an existing one"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Address saved successfully",
                    content = @Content(schema = @Schema(implementation = AddressWrapper.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("")
    public ResponseEntity<ApiSuccessResponse<Address>> saveAddress(Authentication authentication,
                                                                   @RequestBody Address address) {

        Address saved = addressService.save(authentication.getName(), address);

        return ResponseEntity.ok(
                ApiSuccessResponse.<Address>builder()
                        .success(true)
                        .message("Address saved successfully")
                        .data(saved)
                        .build()
        );
    }

    @Operation(
            summary = "Set default address",
            description = "Set an address as default for shipping"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Default address updated successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/default/{id}")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> setDefaultAddress(
            Authentication authentication,
            @PathVariable("id") Long id) {

        addressService.setDefault(id, authentication.getName());

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Default address updated successfully")
                        .data(null)
                        .build()
        );
    }

    @Operation(
            summary = "Delete address",
            description = "Delete an address by ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Address deleted successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponseWrapper.class))
            ),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<MessageResponse>> deleteAddressById(
            Authentication authentication,
            @PathVariable("id") Long id) {

        addressService.delete(id, authentication.getName());

        return ResponseEntity.ok(
                ApiSuccessResponse.<MessageResponse>builder()
                        .success(true)
                        .message("Address deleted successfully")
                        .data(null)
                        .build()
        );

    }
}
