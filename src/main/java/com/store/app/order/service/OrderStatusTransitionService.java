package com.store.app.order.service;

import com.store.app.exception.OperationNotAllowedException;
import com.store.app.order.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * The order state machine. All legal transitions live in one explicit
 * map; everything not listed is forbidden, so steps can be neither
 * skipped (PENDING &rarr; DELIVERED) nor reversed, and the terminal
 * states (DELIVERED, CANCELLED) accept no further changes.
 */
@Component
public class OrderStatusTransitionService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);
        transitions.put(OrderStatus.PENDING,
                Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.CONFIRMED,
                Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PROCESSING,
                Set.of(OrderStatus.PACKED, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PACKED,
                Set.of(OrderStatus.SHIPPED));
        transitions.put(OrderStatus.SHIPPED,
                Set.of(OrderStatus.DELIVERED));
        transitions.put(OrderStatus.DELIVERED, Set.of());
        transitions.put(OrderStatus.CANCELLED, Set.of());
        ALLOWED_TRANSITIONS = transitions;
    }

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /** Legal next statuses from the given state (empty for terminal states). */
    public Set<OrderStatus> allowedNextStatuses(OrderStatus from) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
    }

    /**
     * @throws OperationNotAllowedException if the transition is illegal,
     *         naming the legal next steps
     */
    public void assertTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            Set<OrderStatus> allowed = allowedNextStatuses(from);
            throw new OperationNotAllowedException(
                    "Cannot change order status from " + from + " to " + to + ". "
                            + (allowed.isEmpty()
                            ? from + " is a final status."
                            : "Allowed next statuses: " + allowed));
        }
    }
}
