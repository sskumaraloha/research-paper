package com.store.app.cart.repository;

import com.store.app.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Total units in a user's cart (for the navbar badge). */
    @Query("select coalesce(sum(ci.quantity), 0) from CartItem ci "
            + "where ci.cart.user.id = :userId")
    int countUnitsByUserId(@Param("userId") Long userId);

    void deleteAllByProductId(Long productId);
}
