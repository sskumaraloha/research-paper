package com.store.app.order.repository;

import com.store.app.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Detaches a deleted product from historical order lines; the
     * snapshot columns keep the line meaningful.
     */
    @Modifying
    @Query("update OrderItem oi set oi.product = null where oi.product.id = :productId")
    void detachProduct(@Param("productId") Long productId);
}
