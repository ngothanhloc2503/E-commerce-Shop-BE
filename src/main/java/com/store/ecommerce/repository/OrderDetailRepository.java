package com.store.ecommerce.repository;

import com.store.ecommerce.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    @Query("SELECT NEW com.store.ecommerce.entity.OrderDetail(o.product.category.name, o.quantity, " +
            "o.productCost, o.shippingCost, o.subtotal) " +
            "FROM OrderDetail o WHERE o.order.orderTime BETWEEN :startTime AND :endTime")
    List<OrderDetail> findWithCategoryAndTimeBetween(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime);

    @Query("SELECT NEW com.store.ecommerce.entity.OrderDetail(o.quantity, o.product.name, " +
            "o.productCost, o.shippingCost, o.subtotal) " +
            "FROM OrderDetail o WHERE o.order.orderTime BETWEEN :startTime AND :endTime")
    List<OrderDetail> findWithProductAndTimeBetween(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime);
}