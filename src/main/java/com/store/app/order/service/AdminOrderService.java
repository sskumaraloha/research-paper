package com.store.app.order.service;

import com.store.app.common.dto.PageResponse;
import com.store.app.order.dto.AdminOrderDetailResponse;
import com.store.app.order.dto.AdminOrderSummaryResponse;
import com.store.app.order.entity.OrderStatus;

import java.time.LocalDate;

/**
 * Order management for administrators (all orders, all customers).
 */
public interface AdminOrderService {

    /**
     * Paged order listing with optional text search (order number,
     * customer name, phone), status filter, and creation date range
     * (inclusive on both ends).
     */
    PageResponse<AdminOrderSummaryResponse> searchOrders(String search,
                                                         OrderStatus status,
                                                         LocalDate fromDate,
                                                         LocalDate toDate,
                                                         int page, int size);

    AdminOrderDetailResponse getOrder(Long orderId);

    /**
     * Moves an order to a new status, enforcing the state machine.
     * Transitioning to CANCELLED restocks and settles the payment;
     * transitioning to DELIVERED collects a pending COD payment.
     *
     * @throws com.store.app.exception.OperationNotAllowedException
     *         for an illegal transition
     */
    AdminOrderDetailResponse updateStatus(Long orderId, OrderStatus newStatus);
}
