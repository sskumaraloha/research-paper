package com.store.app.order.controller;

import com.store.app.common.dto.PageResponse;
import com.store.app.order.dto.AdminOrderDetailResponse;
import com.store.app.order.dto.AdminOrderSummaryResponse;
import com.store.app.order.dto.UpdateOrderStatusRequest;
import com.store.app.order.entity.OrderStatus;
import com.store.app.order.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Admin REST API for order management (JWT + ROLE_ADMIN via /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminOrderSummaryResponse>> searchOrders(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(adminOrderService.searchOrders(
                search, status, fromDate, toDate, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderDetailResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(adminOrderService.getOrder(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminOrderDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(adminOrderService.updateStatus(id, request.getStatus()));
    }
}
