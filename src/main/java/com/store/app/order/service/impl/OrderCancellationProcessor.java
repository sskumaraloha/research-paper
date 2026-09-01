package com.store.app.order.service.impl;

import com.store.app.inventory.dto.StockUpdateRequest;
import com.store.app.inventory.entity.InventoryTransactionType;
import com.store.app.inventory.service.InventoryService;
import com.store.app.order.entity.Order;
import com.store.app.order.entity.OrderItem;
import com.store.app.order.entity.OrderStatus;
import com.store.app.payment.entity.Payment;
import com.store.app.payment.service.PaymentServiceRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The one cancellation path shared by customer cancellations and admin
 * status changes: restocks every line still linked to a catalog product
 * (RETURN transactions referencing the order) and settles the payment
 * through its payment strategy. Runs inside the caller's transaction.
 */
@Component
@RequiredArgsConstructor
public class OrderCancellationProcessor {

    private final InventoryService inventoryService;
    private final PaymentServiceRegistry paymentServiceRegistry;

    public void cancel(Order order, String remarks) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) {
                continue;
            }
            StockUpdateRequest restock = new StockUpdateRequest();
            restock.setProductId(item.getProduct().getId());
            restock.setQuantity(item.getQuantity());
            restock.setTransactionType(InventoryTransactionType.RETURN);
            restock.setReference(order.getOrderNumber());
            restock.setRemarks(remarks);
            inventoryService.increaseStock(restock);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Payment payment = order.getPayment();
        paymentServiceRegistry.getService(payment.getPaymentMethod()).cancelPayment(payment);
    }
}
