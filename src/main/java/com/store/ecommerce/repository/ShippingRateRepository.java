package com.store.ecommerce.repository;

import com.store.ecommerce.entity.ShippingRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {

    @Query("SELECT s FROM ShippingRate s WHERE s.country LIKE %:keyword% OR s.state LIKE %:keyword%")
    Page<ShippingRate> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM ShippingRate s WHERE s.country LIKE %:keyword% OR s.state LIKE %:keyword%")
    List<ShippingRate> searchByKeyword(@Param("keyword") String keyword, Sort sort);

    @Query("SELECT s FROM ShippingRate s WHERE UPPER(s.country) = UPPER(:countryName) AND UPPER(s.state) = UPPER(:state)")
    Optional<ShippingRate> findByCountryAndState(@Param("countryName") String countryName, @Param("state") String state);

    @Modifying
    @Query("UPDATE ShippingRate s SET s.codSupported = :enabled WHERE s.id = :id")
    void updateCODSupported(@Param("id") Long id, @Param("enabled") boolean enabled);
}