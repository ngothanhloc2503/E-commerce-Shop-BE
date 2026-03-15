package com.store.ecommerce.controller;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.response.AddressBookDTO;
import com.store.ecommerce.entity.Address;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address-book")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @GetMapping("")
    public ResponseEntity<?> getAddressBook(Authentication authentication) {
        List<Address> listAddresses = null;
        try {
            listAddresses = addressService.listAddressBook(authentication.getName());
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }

        boolean primaryAddressAsDefault = true;
        for (Address address : listAddresses) {
            if (address.isDefaultForShipping()) {
                primaryAddressAsDefault = false;
                break;
            }
        }

        return ResponseEntity.ok(AddressBookDTO.builder()
                .addressBook(listAddresses)
                .primaryAddressAsDefault(primaryAddressAsDefault)
                .build());
    }

    @GetMapping("/default")
    public ResponseEntity<?> getDefaultAddress(Authentication authentication) {
        try {
            return ResponseEntity.ok(addressService.getDefaultAddress(authentication.getName()));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddressById(Authentication authentication,
                                            @PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(addressService.getByIdAndUserEmail(id, authentication.getName()));
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("")
    public ResponseEntity<?> saveAddress(Authentication authentication,
                                         @RequestBody Address address) {
        try {
            return ResponseEntity.ok(addressService.save(authentication.getName(), address));
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/default/{id}")
    public ResponseEntity<?> setDefaultAddress(Authentication authentication,
                                    @PathVariable("id") Long id) {
        try {
            addressService.setDefault(id, authentication.getName());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
