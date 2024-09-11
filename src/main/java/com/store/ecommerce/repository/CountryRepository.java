package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    public List<Country> findAllByOrderByNameAsc();

    public Optional<Country> findByNameIgnoreCase(String countryName);

    public Optional<Country> findByCode(String code);
}
