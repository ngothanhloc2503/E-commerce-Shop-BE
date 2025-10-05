package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long>,
        SearchRepository<Brand, Long> {
    public Optional<Brand> findByName(String name);

//    @Query(value = "SELECT * FROM Brand b INNER JOIN brands_categories bc ON b.id = bc.brand_id WHERE bc.category_id = ?1")
    public List<Brand> findAllByCategories(Category category);

    // 1. Find all brands with categories eagerly fetched (no keyword)
    @Query(value = "SELECT DISTINCT b FROM Brand b LEFT JOIN FETCH b.categories",
            countQuery = "SELECT COUNT(b) FROM Brand b")
    Page<Brand> findAll(Pageable pageable);

    // 2. Find all brands by keyword, with categories eagerly fetched
    @Query(value = "SELECT DISTINCT b FROM Brand b LEFT JOIN FETCH b.categories " +
            "WHERE CONCAT(b.id, ' ', b.name) LIKE %?1%",
            countQuery = "SELECT COUNT(b) FROM Brand b WHERE CONCAT(b.id, ' ', b.name) LIKE %?1%")
    Page<Brand> findAll(String keyword, Pageable pageable);


    @Query("SELECT b FROM Brand b WHERE CONCAT(b.id, ' ', b.name) LIKE %?1%")
    public List<Brand> findAll(String keyword, Sort sort);
}
