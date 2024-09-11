package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Country;
import com.store.ecommerce.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {
    public List<State> findByCountryOrderByNameAsc(Country country);

    @Query("SELECT s FROM State s WHERE UPPER(s.country.name) = UPPER(?1) AND UPPER(s.name) = UPPER(?2)")
    Optional<State> findByCountryAndNameIgnoreCase(String countryName, String stateName);
}
