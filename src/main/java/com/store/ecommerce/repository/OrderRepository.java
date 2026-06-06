package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Order;
import com.store.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product", "orderTrack"})
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"user"})
    List<Order> findAll();

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT o FROM Order o WHERE CONCAT(o.id, ' ', o.user.firstName, ' ', o.user.lastName, ' ', " +
            "o.firstName, ' ', o.lastName, ' ', o.paymentMethod, ' ', COALESCE(o.city, ''), ' ', o.state, ' ', " +
            "o.country, ' ', o.status, ' ', o.addressLine1, ' ', COALESCE(o.addressLine2, '')) LIKE %:keyword%")
    Page<Order> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT o FROM Order o WHERE CONCAT(o.id, ' ', o.user.firstName, ' ', o.user.lastName, ' ', " +
            "o.firstName, ' ', o.lastName, ' ', o.paymentMethod, ' ', COALESCE(o.city, ''), ' ', o.state, ' ', " +
            "o.country, ' ', o.status, ' ', o.addressLine1, ' ', COALESCE(o.addressLine2, '')) LIKE %:keyword%")
    List<Order> searchByKeyword(@Param("keyword") String keyword, Sort sort);

    @Query("SELECT NEW com.store.ecommerce.entity.Order(o.id, o.orderTime, o.productCost, o.subtotal, o.total)"
            + " FROM Order o WHERE o.orderTime BETWEEN :startTime AND :endTime ORDER BY o.orderTime ASC")
    List<Order> findByOrderTimeBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    // --- For Customer ---
    @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product"})
    Page<Order> findAllByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product"})
    List<Order> findAllByUser(User user, Sort sort);

    @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product", "orderTrack"})
    @Query("SELECT o FROM Order o WHERE o.id = :orderId AND o.user.id = :userId")
    Optional<Order> findByIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user", "orderDetails", "orderDetails.product"})
    List<Order> findByUserId(@Param("userId") Long userId);
}