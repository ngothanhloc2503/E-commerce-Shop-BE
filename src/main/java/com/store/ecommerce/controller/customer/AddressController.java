package com.store.ecommerce.controller.customer;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.dto.response.AddressBookDTO;
import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.security.jwt.JwtUtil;
import com.store.ecommerce.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/address-book")
@PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {
    @Autowired
    private AddressService addressService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("")
    public ResponseEntity<?> getAddressBook(@RequestHeader("Authorization") String jwt) {
        List<Address> listAddresses = null;
        try {
            listAddresses = addressService.listAddressBook(jwtUtil.extractSubject(jwt));
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
    public ResponseEntity<?> getDefaultAddress(@RequestHeader("Authorization") String jwt) {
        try {
            return ResponseEntity.ok(addressService.getDefaultAddress(
                    jwtUtil.extractSubject(jwt)));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddressById(@RequestHeader("Authorization") String jwt,
                                            @PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(addressService.getByIdAndUserEmail(id,
                    jwtUtil.extractSubject(jwt)));
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveAddress(@RequestHeader("Authorization") String jwt,
                                         @RequestBody Address address) {
        try {
            return ResponseEntity.ok(addressService.save(jwtUtil.extractSubject(jwt), address));
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/default/{id}")
    public ResponseEntity<?> setDefaultAddress(@RequestHeader("Authorization") String jwt,
                                    @PathVariable("id") Long id) {
        try {
            addressService.setDefault(id, jwtUtil.extractSubject(jwt));
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ConflictException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
