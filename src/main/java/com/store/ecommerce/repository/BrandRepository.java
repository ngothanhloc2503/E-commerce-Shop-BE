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

    public Page<Brand> findAll(Pageable pageable);

    @Query("SELECT b FROM Brand b WHERE CONCAT(b.id, ' ', b.name) LIKE %?1%")
    public Page<Brand> findAll(String keyword, Pageable pageable);

    @Query("SELECT b FROM Brand b WHERE CONCAT(b.id, ' ', b.name) LIKE %?1%")
    public List<Brand> findAll(String keyword, Sort sort);
}
