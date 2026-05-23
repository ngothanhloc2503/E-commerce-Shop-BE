package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Product;
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
public interface ProductRepository extends JpaRepository<Product, Long>, SearchRepository<Product, Long> {
    // For Staff
    public Optional<Product> findByName(String name);

    @Query("UPDATE Product p SET p.enabled = ?2 WHERE p.id = ?1")
    @Modifying
    public void updateEnabledStatus(Long id, boolean status);

    public Page<Product> findAll(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = ?1")
    public List<Product> findAllByCategory(Long categoryID);

    @Query("SELECT p FROM Product p WHERE p.name LIKE %?1% OR p.summary LIKE %?1% OR p.description LIKE %?1%")
    public Page<Product> findAll(String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.name LIKE %?1% OR p.summary LIKE %?1% OR p.description LIKE %?1%")
    public List<Product> findAll(String keyword, Sort sort);

    @Query("SELECT p FROM Product p WHERE p.category.id = ?1 AND (p.name LIKE %?2% OR "
            + "p.summary LIKE %?2% OR p.description LIKE %?2%)")
    public List<Product> findAllByCategory(Long categoryID, String keyword, Sort sort);

    @Query("SELECT p FROM Product p WHERE p.category.id = ?1")
    public Page<Product> findAllByCategory(Long categoryID, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = ?1 AND (p.name LIKE %?2% OR "
            + "p.summary LIKE %?2% OR p.description LIKE %?2%)")
    public Page<Product> searchByCategory(Long categoryID, String key, Pageable pageable);

    // For Customer
    @Query("SELECT p FROM Product p WHERE p.enabled = true")
    public List<Product> findAll(Sort sort);

    @Query("SELECT p FROM Product p WHERE p.alias = ?1"
            + " AND p.enabled = true")
    public Optional<Product> findByAlias(String alias);

    @Query("SELECT p FROM Product p WHERE p.enabled = true ORDER BY p.averageRating DESC")
    public List<Product> findAllEnabled();

    @Query("""
    SELECT p
    FROM Product p
    WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        AND (:averageRating IS NULL OR p.averageRating >= :averageRating)
        AND (:brandIds IS NULL OR p.brand.id IN :brandIds)
        AND p.enabled = true
    """)
    public Page<Product> searchProduct(String keyword, Float averageRating, List<Long> brandIds, Pageable pageable);

    @Query("""
        SELECT p
        FROM Product p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            AND p.enabled = true
    """)
    public List<Product> searchProduct(String keyword);
}
