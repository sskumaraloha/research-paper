package com.store.app.order.service;

import com.store.app.common.dto.PageResponse;
import com.store.app.order.dto.OrderResponse;
import com.store.app.order.dto.OrderSummaryResponse;
import com.store.app.order.dto.PlaceOrderRequest;

/**
 * Checkout and order management. All customer-facing methods are
 * scoped to the acting user.
 */
public interface OrderService {

    /**
     * Places an order from the user's cart, atomically: validates the
     * cart and address, reduces inventory per item under pessimistic
     * locks (SALE transactions referencing the order number), snapshots
     * items and address, records the payment, and clears the cart.
     * Any failure — including stock becoming unavailable mid-checkout —
     * rolls the whole transaction back; partial orders cannot exist.
     *
     * @throws com.store.app.exception.BusinessValidationException
     *         if the cart is empty, a product is no longer available,
     *         or stock is insufficient
     * @throws com.store.app.exception.ResourceNotFoundException
     *         if the address does not belong to the user
     */
    OrderResponse placeOrder(Long userId, PlaceOrderRequest request);

    PageResponse<OrderSummaryResponse> getOrders(Long userId, int page, int size);

    OrderResponse getOrder(Long userId, Long orderId);

    /**
     * Cancels a PENDING or CONFIRMED order, restocking each line via
     * RETURN inventory transactions. A PAID payment becomes REFUNDED,
     * an unpaid one FAILED.
     *
     * @throws com.store.app.exception.OperationNotAllowedException
     *         if the order is no longer cancellable
     */
    OrderResponse cancelOrder(Long userId, Long orderId);

    /** Detaches a deleted product from historical order lines. */
    void detachProductFromOrders(Long productId);
}
