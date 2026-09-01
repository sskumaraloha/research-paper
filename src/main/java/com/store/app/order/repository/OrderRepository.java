package com.store.app.order.repository;

import com.store.app.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Page<Order> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /** Ownership-scoped lookup: another user's order is simply not found. */
    @EntityGraph(attributePaths = {"items", "payment"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    boolean existsByOrderNumber(String orderNumber);
}
