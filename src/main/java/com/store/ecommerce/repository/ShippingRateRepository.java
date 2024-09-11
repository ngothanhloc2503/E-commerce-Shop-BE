package com.store.ecommerce.repository;

import com.store.ecommerce.entity.ShippingRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long>,
        SearchRepository<ShippingRate, Long> {
    @Query("SELECT s FROM ShippingRate s WHERE s.country LIKE %?1% OR s.state LIKE %?1%")
    public Page<ShippingRate> findAll(String keyword, Pageable pageable);

    @Query("SELECT s FROM ShippingRate s WHERE s.country LIKE %?1% OR s.state LIKE %?1%")
    public List<ShippingRate> findAll(String keyword, Sort sort);

    @Query("UPDATE ShippingRate s SET s.codSupported = ?2 WHERE s.id = ?1")
    @Modifying
    public void updateCODSupported(Integer id, boolean enabled);

    @Query("SELECT s FROM ShippingRate s WHERE UPPER(s.country) = UPPER(?1) AND UPPER(s.state) = UPPER(?2)")
    public ShippingRate findByCountryAndState(String countryName, String state);
}
