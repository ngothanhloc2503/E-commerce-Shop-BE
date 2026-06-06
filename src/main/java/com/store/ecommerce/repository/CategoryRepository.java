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

    @Query("""
        SELECT c.id FROM Category c 
        WHERE c.enabled = true 
        AND (
            LOWER(REPLACE(REPLACE(c.name, '&', ' '), '-', ' ')) = LOWER(REPLACE(REPLACE(:name, '&', ' '), '-', ' '))
            OR LOWER(c.name) = LOWER(:name)
        )
    """)
    Optional<Long> findIdByNameFlexible(@Param("name") String name);

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

    // Recursive CTE: Duyệt cây sâu vô hạn
    @Query(value = """
        WITH RECURSIVE category_tree AS (
            -- Base case: Category gốc (cha cần tìm)
            SELECT id FROM categories WHERE id = :categoryId AND enabled = true
            
            UNION ALL
            
            -- Recursive case: Tìm tất cả con của các category đã tìm được
            SELECT c.id FROM categories c
            INNER JOIN category_tree ct ON c.parent_id = ct.id
            WHERE c.enabled = true
        )
        SELECT id FROM category_tree
        """, nativeQuery = true)
    List<Long> findCategoryAndAllDescendantIds(@Param("categoryId") Long categoryId);

    // ==================== For Customer ====================

    List<Category> findAllByEnabledTrue();

    @Query("SELECT c FROM Category c WHERE REPLACE(c.name, ' ', '-') = :name AND c.enabled = true")
    Optional<Category> getCategoryByName(@Param("name") String name);
}