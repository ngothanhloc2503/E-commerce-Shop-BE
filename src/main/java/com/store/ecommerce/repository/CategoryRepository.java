package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Category;
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
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ==================== For Staff ====================

    Optional<Category> findByName(String name);

    // Đổi tên từ findAll(keyword, ...) thành searchByKeyword để tránh xung đột
    @Query("SELECT c FROM Category c WHERE CONCAT(c.id, ' ', c.name, ' ', c.description) LIKE %:keyword%")
    Page<Category> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Category c WHERE CONCAT(c.id, ' ', c.name, ' ', c.description) LIKE %:keyword%")
    List<Category> searchByKeyword(@Param("keyword") String keyword, Sort sort);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Category c SET c.parent = null WHERE c.parent.id = :parentId")
    void detachChildren(@Param("parentId") Long parentId);

    @Modifying
    @Query("UPDATE Category c SET c.enabled = :enabled WHERE c.id = :id")
    void updateEnabledStatus(@Param("id") Long id, @Param("enabled") boolean enabled);

    // ==================== For Customer ====================

    List<Category> findAllByEnabledTrue();

    @Query("SELECT c FROM Category c WHERE REPLACE(c.name, ' ', '-') = :name AND c.enabled = true")
    Optional<Category> getCategoryByName(@Param("name") String name);
}