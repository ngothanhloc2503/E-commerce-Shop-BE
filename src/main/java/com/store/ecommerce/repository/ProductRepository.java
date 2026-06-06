package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    List<Product> findAll();

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    Optional<Product> findByName(String name);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    Optional<Product> findByAliasAndEnabledTrue(String alias);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    List<Product> findByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    List<Product> findAllByEnabledTrue(Sort sort);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    Page<Product> findByCategoryIdInAndEnabledTrue(List<Long> categoryIds, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    Page<Product> findAllByEnabledTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword% OR p.summary LIKE %:keyword% OR p.description LIKE %:keyword%")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword% OR p.summary LIKE %:keyword% OR p.description LIKE %:keyword%")
    List<Product> searchByKeyword(@Param("keyword") String keyword, Sort sort);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND (p.name LIKE %:keyword% OR p.summary LIKE %:keyword% OR p.description LIKE %:keyword%)")
    List<Product> searchByCategoryIdAndKeyword(@Param("categoryId") Long categoryId,
                                               @Param("keyword") String keyword,
                                               Sort sort);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND (p.name LIKE %:keyword% OR p.summary LIKE %:keyword% OR p.description LIKE %:keyword%)")
    Page<Product> searchByCategoryIdAndKeyword(@Param("categoryId") Long categoryId,
                                               @Param("keyword") String keyword,
                                               Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            AND (:averageRating IS NULL OR p.averageRating >= :averageRating)
            AND (:brandIds IS NULL OR p.brand.id IN :brandIds)
            AND p.enabled = true
    """)
    Page<Product> searchProduct(@Param("keyword") String keyword,
                                @Param("averageRating") Float averageRating,
                                @Param("brandIds") List<Long> brandIds,
                                Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand", "images", "details"})
    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            AND p.enabled = true
    """)
    List<Product> searchProduct(@Param("keyword") String keyword);

    @Query("UPDATE Product p SET p.enabled = :status WHERE p.id = :id")
    @Modifying
    void updateEnabledStatus(@Param("id") Long id, @Param("status") boolean status);
}