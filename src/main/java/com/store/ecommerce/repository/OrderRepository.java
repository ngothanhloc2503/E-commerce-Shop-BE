package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Order;
import com.store.ecommerce.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, SearchRepository<Order, Long> {
    public List<Order> findAll();

    @Query("SELECT o FROM Order o WHERE CONCAT(o.id, ' ', o.user.firstName, ' ', o.user.lastName, ' ', " +
            "o.firstName, ' ', o.lastName, ' ', o.paymentMethod, ' ', COALESCE(o.city, ''), ' ', o.state, ' ', " +
            "o.country, ' ', o.status, ' ', o.addressLine1, ' ', COALESCE(o.addressLine2, '')) LIKE %?1%")
    public Page<Order> findAll(String keyword, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE CONCAT(o.id, ' ', o.user.firstName, ' ', o.user.lastName, ' ', " +
            "o.firstName, ' ', o.lastName, ' ', o.paymentMethod, ' ', COALESCE(o.city, ''), ' ', o.state, ' ', " +
            "o.country, ' ', o.status, ' ', o.addressLine1, ' ', COALESCE(o.addressLine2, '')) LIKE %?1%")
    public List<Order> findAll(String keyword, Sort sort);

    @Query("SELECT NEW com.store.ecommerce.entity.Order(o.id, o.orderTime, o.productCost, o.subtotal, o.total)"
            + " FROM Order o WHERE o.orderTime BETWEEN ?1 AND ?2 ORDER BY o.orderTime ASC")
    public List<Order> findByOrderTimeBetween(Date startTime, Date endTime);

    //For customer
    public Page<Order> findAllByUser(User user, Pageable pageable);

    public List<Order> findAllByUser(User user, Sort sort);

    @Query("SELECT o FROM Order o WHERE o.id = ?1 AND o.user.id = ?2")
    public Order findByIdAndUserId(Long orderId, Long userId);
}
