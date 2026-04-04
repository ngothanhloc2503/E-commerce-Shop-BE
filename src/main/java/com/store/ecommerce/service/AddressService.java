package com.store.ecommerce.service;

import com.store.ecommerce.entity.Address;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;

import java.util.List;

public interface AddressService {
    List<Address> listAddressBook(String userEmail) throws NotFoundException;

    Address save(String userEmail, Address address) throws NotFoundException;

    Address getByIdAndUserEmail(Long addressId, String userEmail) throws NotFoundException, ConflictException;

    void delete(Long addressId, String userEmail) throws NotFoundException, ConflictException;

    void setDefault(Long addressId, String userEmail) throws NotFoundException;

    Address getDefaultAddress(String userEmail) throws NotFoundException;
}
