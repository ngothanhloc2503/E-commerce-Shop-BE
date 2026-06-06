package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Address;
import com.store.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUser(User user);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    Optional<Address> findByUserIdAndDefaultForShippingTrue(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Address a WHERE a.id = :id AND a.user.id = :userId")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Address a SET a.defaultForShipping = true WHERE a.id = :id AND a.user.id = :userId")
    void setDefaultAddress(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Address a SET a.defaultForShipping = false WHERE a.id != :defaultAddressId AND a.user.id = :userId")
    void setNonDefaultForOthers(@Param("defaultAddressId") Long defaultAddressId, @Param("userId") Long userId);
}