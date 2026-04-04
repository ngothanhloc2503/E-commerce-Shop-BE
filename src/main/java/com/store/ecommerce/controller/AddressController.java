package com.store.ecommerce.controller;

import com.store.ecommerce.dto.response.AddressBookDTO;
import com.store.ecommerce.entity.Address;
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
        listAddresses = addressService.listAddressBook(authentication.getName());


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

        return ResponseEntity.ok(addressService.getDefaultAddress(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddressById(Authentication authentication,
                                            @PathVariable("id") Long id) {

        return ResponseEntity.ok(addressService.getByIdAndUserEmail(id, authentication.getName()));
    }

    @PostMapping("")
    public ResponseEntity<?> saveAddress(Authentication authentication,
                                         @RequestBody Address address) {

        return ResponseEntity.ok(addressService.save(authentication.getName(), address));
    }

    @PutMapping("/default/{id}")
    public ResponseEntity<?> setDefaultAddress(Authentication authentication,
                                               @PathVariable("id") Long id) {

        addressService.setDefault(id, authentication.getName());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddressById(Authentication authentication,
                                               @PathVariable("id") Long id) {

        addressService.delete(id, authentication.getName());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
