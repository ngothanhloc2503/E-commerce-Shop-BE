package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @EntityGraph(attributePaths = {"product", "product.category", "product.brand"})
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Wishlist> findByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(w) > 0 FROM Wishlist w WHERE w.user.id = :userId AND w.product.id = :productId")
    boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    long countByUserId(Long userId);
}