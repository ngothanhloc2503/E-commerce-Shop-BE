package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Brand;
import com.store.ecommerce.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByName(String name);

    List<Brand> findAllByCategories(Category category);

    @Override
    @EntityGraph(attributePaths = {"categories"})
    Page<Brand> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"categories"})
    @Query("SELECT DISTINCT b FROM Brand b WHERE CONCAT(b.id, ' ', b.name) LIKE %:keyword%")
    Page<Brand> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"categories"})
    @Query("SELECT DISTINCT b FROM Brand b WHERE CONCAT(b.id, ' ', b.name) LIKE %:keyword%")
    List<Brand> searchByKeyword(@Param("keyword") String keyword, Sort sort);
}