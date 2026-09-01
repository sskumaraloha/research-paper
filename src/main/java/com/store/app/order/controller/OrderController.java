package com.store.app.order.controller;

import com.store.app.common.dto.PageResponse;
import com.store.app.order.dto.OrderResponse;
import com.store.app.order.dto.OrderSummaryResponse;
import com.store.app.order.dto.PlaceOrderRequest;
import com.store.app.order.service.OrderService;
import com.store.app.security.StoreUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for checkout and order history
 * (JWT + ROLE_CUSTOMER via /api/customer/**).
 */
@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal StoreUserDetails principal,
            @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(userId(principal), request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> getOrders(
            @AuthenticationPrincipal StoreUserDetails principal,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getOrders(userId(principal), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(userId(principal), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal StoreUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(userId(principal), id));
    }

    private Long userId(StoreUserDetails principal) {
        return principal.getUser().getId();
    }
}
