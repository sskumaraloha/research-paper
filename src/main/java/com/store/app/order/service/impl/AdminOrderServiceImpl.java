package com.store.app.order.service.impl;

import com.store.app.common.dto.PageResponse;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.order.dto.AdminOrderDetailResponse;
import com.store.app.order.dto.AdminOrderSummaryResponse;
import com.store.app.order.entity.Order;
import com.store.app.order.entity.OrderItem;
import com.store.app.order.entity.OrderStatus;
import com.store.app.order.mapper.OrderMapper;
import com.store.app.order.repository.OrderRepository;
import com.store.app.order.service.AdminOrderService;
import com.store.app.order.service.OrderStatusTransitionService;
import com.store.app.payment.entity.Payment;
import com.store.app.payment.entity.PaymentMethod;
import com.store.app.payment.entity.PaymentStatus;
import com.store.app.payment.service.PaymentServiceRegistry;
import com.store.app.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderStatusTransitionService transitionService;
    private final OrderCancellationProcessor cancellationProcessor;
    private final PaymentServiceRegistry paymentServiceRegistry;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryResponse> searchOrders(String search,
                                                                OrderStatus status,
                                                                LocalDate fromDate,
                                                                LocalDate toDate,
                                                                int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "id"));

        String term = StringUtils.hasText(search) ? search.trim() : null;
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        // "to" is inclusive: everything before the following midnight.
        LocalDateTime to = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;

        return PageResponse.from(
                orderRepository.adminSearch(term, status, from, to, pageable)
                        .map(this::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrder(Long orderId) {
        return toDetail(requiredOrder(orderId));
    }

    @Override
    public AdminOrderDetailResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = requiredOrder(orderId);
        transitionService.assertTransition(order.getStatus(), newStatus);

        if (newStatus == OrderStatus.CANCELLED) {
            // Restock + payment settlement, same path as customer cancellation.
            cancellationProcessor.cancel(order, "Order cancelled by admin");
        } else {
            order.setStatus(newStatus);
            if (newStatus == OrderStatus.DELIVERED) {
                collectCodPaymentIfPending(order);
            }
        }

        Order saved = orderRepository.save(order);
        log.info("Order {} moved to {}", saved.getOrderNumber(), saved.getStatus());
        return toDetail(saved);
    }

    // ------------------------------------------------------------------

    /** COD is collected at the door: delivery marks a pending payment PAID. */
    private void collectCodPaymentIfPending(Order order) {
        Payment payment = order.getPayment();
        if (payment.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                && payment.getStatus() == PaymentStatus.PENDING) {
            paymentServiceRegistry.getService(payment.getPaymentMethod())
                    .confirmPayment(payment, null);
        }
    }

    private Order requiredOrder(Long orderId) {
        return orderRepository.findOneById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));
    }

    private AdminOrderSummaryResponse toSummary(Order order) {
        User customer = order.getUser();
        return new AdminOrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                customer.getFirstName() + " " + customer.getLastName(),
                customer.getPhoneNumber(),
                order.getStatus(),
                order.getPayment().getPaymentMethod(),
                order.getPayment().getStatus(),
                order.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }

    private AdminOrderDetailResponse toDetail(Order order) {
        User customer = order.getUser();
        return new AdminOrderDetailResponse(
                orderMapper.toResponse(order),
                customer.getFirstName() + " " + customer.getLastName(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                transitionService.allowedNextStatuses(order.getStatus()));
    }
}
