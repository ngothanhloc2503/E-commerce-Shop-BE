package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    public List<Currency> findAllByOrderByNameAsc();
}
