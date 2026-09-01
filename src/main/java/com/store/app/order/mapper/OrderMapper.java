package com.store.app.order.mapper;

import com.store.app.order.dto.OrderAddressResponse;
import com.store.app.order.dto.OrderItemResponse;
import com.store.app.order.dto.OrderResponse;
import com.store.app.order.dto.OrderSummaryResponse;
import com.store.app.order.entity.Order;
import com.store.app.order.entity.OrderItem;
import com.store.app.order.entity.ShippingAddress;
import org.springframework.stereotype.Component;

/**
 * Maps order entities to DTOs.
 */
@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPayment().getPaymentMethod(),
                order.getPayment().getStatus(),
                toAddressResponse(order.getShippingAddress()),
                order.getItems().stream().map(this::toItemResponse).toList(),
                countItems(order),
                order.getSubtotal(),
                order.getDiscount(),
                order.getTotalAmount(),
                order.isCancellable(),
                order.getCreatedAt()
        );
    }

    public OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                countItems(order),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProductName(),
                item.getSku(),
                item.getPriceAtPurchase(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }

    private OrderAddressResponse toAddressResponse(ShippingAddress address) {
        return new OrderAddressResponse(
                address.getFullName(),
                address.getPhoneNumber(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.getCountry()
        );
    }

    private int countItems(Order order) {
        return order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
    }
}
