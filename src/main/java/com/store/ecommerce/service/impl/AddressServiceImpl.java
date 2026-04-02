package com.store.ecommerce.service.impl;

import com.amazonaws.services.kms.model.ConflictException;
import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.User;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.AddressRepository;
import com.store.ecommerce.repository.UserRepository;
import com.store.ecommerce.service.AddressService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public List<Address> listAddressBook(String userEmail) throws NotFoundException {
        User user = getUserByEmail(userEmail);
        return addressRepository.findByUser(user);
    }

    @Override
    public Address save(String userEmail, Address address) throws NotFoundException {
        // Updating
        if (address.getId() != null) {
            if (address.getId() <= 0) {
                address.setId(null);
                address.setDefaultForShipping(false);
            } else {
                Address addressInDB = getByIdAndUserEmail(address.getId(), userEmail);
                address.setDefaultForShipping(addressInDB.isDefaultForShipping());
            }
        } else {
            address.setDefaultForShipping(false);
        }
        User user = getUserByEmail(userEmail);
        address.setUser(user);
        return addressRepository.save(address);
    }

    @Override
    public Address getByIdAndUserEmail(Long addressId, String userEmail) throws NotFoundException {
        User user = getUserByEmail(userEmail);

        Address addressInDB = addressRepository.findById(addressId).orElseThrow(
                () -> new NotFoundException("Could not find any address with id" + addressId));

        if (!Objects.equals(addressInDB.getUser().getId(), user.getId())) {
            throw new ConflictException("Address with id " + addressInDB.getId()
                    + " does not belong to the user have email " + user.getEmail());
        }

        return addressInDB;
    }

    @Override
    public void delete(Long addressId, String userEmail) throws NotFoundException {
        Address addressInDB = getByIdAndUserEmail(addressId, userEmail);

        if (addressInDB.isDefaultForShipping()) {
            throw new ConflictException("The address is being set as default so it cannot be deleted!");
        }

        addressRepository.deleteByIdAndUserId(addressInDB.getId(), addressInDB.getUser().getId());
    }

    @Override
    public void setDefault(Long addressId, String userEmail) throws NotFoundException {
        if (addressId > 0) {
            getByIdAndUserEmail(addressId, userEmail); // check address belong to user or not
            addressRepository.setDefaultAddress(addressId);
        }

        addressRepository.setNonDefaultForOthers(addressId, userEmail);
    }

    @Override
    public Address getDefaultAddress(String userEmail) throws NotFoundException {
        User user = getUserByEmail(userEmail);
        Address defaultAddress = addressRepository.findDefaultByUserId(user.getId());

        if (defaultAddress == null) {
            return new Address(user);
        }
        return defaultAddress;
    }

    private User getUserByEmail(String email) throws NotFoundException {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("Could not find any user have email: " + email));
    }
}
