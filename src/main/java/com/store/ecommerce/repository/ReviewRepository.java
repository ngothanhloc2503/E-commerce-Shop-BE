package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Product;
import com.store.ecommerce.entity.Review;
import com.store.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    public interface RatingCountProjection {
        Integer getRating();
        Long getCount();
    }

    @EntityGraph(attributePaths = {"user", "product"})
    Page<Review> findByProductAndApprovedTrue(Product product, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product"})
    Page<Review> findByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product"})
    List<Review> findByProductAndApprovedTrue(Product product);

    @EntityGraph(attributePaths = {"user", "product"})
    Optional<Review> findByIdAndUser(Long id, User user);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product = :product AND r.approved = true")
    Optional<Double> findAverageRatingByProduct(Product product);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product = :product AND r.approved = true")
    long countByProductAndApprovedTrue(Product product);

    @Query("SELECT r.rating AS rating, COUNT(r) AS count FROM Review r WHERE r.product = :product AND r.approved = true GROUP BY r.rating")
    List<RatingCountProjection> getRatingDistribution(@Param("product") Product product);

    boolean existsByProductAndUser(Product product, User user);
}