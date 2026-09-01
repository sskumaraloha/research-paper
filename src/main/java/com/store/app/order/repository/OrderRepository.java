package com.store.app.order.repository;

import com.store.app.order.entity.Order;
import com.store.app.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Page<Order> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /** Ownership-scoped lookup: another user's order is simply not found. */
    @EntityGraph(attributePaths = {"items", "payment"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    boolean existsByOrderNumber(String orderNumber);

    /** Admin lookup with everything the detail page needs. */
    @EntityGraph(attributePaths = {"items", "payment", "user"})
    Optional<Order> findOneById(Long id);

    /**
     * Admin listing: optional text search (order number, customer name,
     * phone), status filter, and creation date range.
     */
    @EntityGraph(attributePaths = {"items", "payment", "user"})
    @Query("""
            select o from Order o join o.user u
            where (:search is null or :search = ''
                or lower(o.orderNumber) like lower(concat('%', :search, '%'))
                or lower(u.firstName) like lower(concat('%', :search, '%'))
                or lower(u.lastName) like lower(concat('%', :search, '%'))
                or u.phoneNumber like concat('%', :search, '%'))
              and (:status is null or o.status = :status)
              and (:fromDate is null or o.createdAt >= :fromDate)
              and (:toDate is null or o.createdAt < :toDate)
            """)
    Page<Order> adminSearch(@Param("search") String search,
                            @Param("status") OrderStatus status,
                            @Param("fromDate") LocalDateTime fromDate,
                            @Param("toDate") LocalDateTime toDate,
                            Pageable pageable);

    /** Booked revenue: all orders except those in the excluded status. */
    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.status <> :excluded")
    BigDecimal sumTotalAmountByStatusNot(@Param("excluded") OrderStatus excluded);

    long countByCreatedAtAfter(LocalDateTime since);

    List<Order> findTop8ByOrderByIdDesc();
}
