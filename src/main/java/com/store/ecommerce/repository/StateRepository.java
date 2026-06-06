package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Country;
import com.store.ecommerce.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {

    List<State> findByCountryOrderByNameAsc(Country country);

    List<State> findByCountryIdOrderByNameAsc(Long countryId);

    @Query("SELECT s FROM State s WHERE UPPER(s.country.name) = UPPER(:countryName) AND UPPER(s.name) = UPPER(:stateName)")
    Optional<State> findByCountryAndNameIgnoreCase(
            @Param("countryName") String countryName,
            @Param("stateName") String stateName);
}