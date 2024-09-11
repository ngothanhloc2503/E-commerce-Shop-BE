package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    public List<Address> findByUser(User user);

    @Query("SELECT a FROM Address a WHERE a.id = ?1 AND a.user.id = ?2")
    public Address findByIdAndUserId(Long id, Long userId);

    @Query("DELETE FROM Address a WHERE a.id = ?1 AND a.user.id = ?2")
    @Modifying
    public void deleteByIdAndUserId(Long id, Long userID);

    @Query("UPDATE Address a SET a.defaultForShipping = true WHERE a.id = ?1")
    @Modifying
    public void setDefaultAddress(Long id);

    @Query("UPDATE Address a SET a.defaultForShipping = false WHERE a.id != ?1 AND a.user.email = ?2")
    @Modifying
    public void setNonDefaultForOthers(Long defaultAddressId, String userEmail);

    @Query("SELECT a FROM Address a WHERE a.user.id = ?1 AND a.defaultForShipping = true")
    public Address findDefaultByUserId(Long userId);
}
