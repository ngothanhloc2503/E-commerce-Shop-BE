package com.store.ecommerce.repository;

import com.store.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("DELETE FROM CartItem c WHERE c.cart.id = ?1")
    @Modifying
    public void deleteByCartId(Long cart);
}
