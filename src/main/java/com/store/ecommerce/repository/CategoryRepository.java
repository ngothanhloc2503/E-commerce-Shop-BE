package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>,
        SearchRepository<Category, Long> {
    // For Staff
    public Optional<Category> findByName(String name);

    @Query("UPDATE Category c SET c.enabled = ?2 WHERE c.id = ?1")
    @Modifying
    public void updateEnabledStatus(Long id, boolean enabled);

    public Page<Category> findAll(Pageable pageable);

    @Query("SELECT c FROM Category c WHERE CONCAT(c.id, ' ',c.name, ' ', c.description) LIKE %?1%")
    public Page<Category> findAll(String keyword, Pageable pageable);

    @Query("SELECT c FROM Category c WHERE CONCAT(c.id, ' ',c.name, ' ', c.description) LIKE %?1%")
    public List<Category> findAll(String keyword, Sort sort);

    // For Customer
    @Query("SELECT c FROM Category c WHERE c.enabled = true")
    public List<Category> getAllCategoriesEnabled();

    @Query("SELECT c FROM Category c WHERE REPLACE(c.name, ' ', '-') = ?1 AND c.enabled = true")
    public Optional<Category> getCategoryByName(String name);
}
