package com.store.app.admin.service.impl;

import com.store.app.admin.dto.AdminDashboardResponse;
import com.store.app.admin.dto.DashboardStats;
import com.store.app.admin.dto.RecentOrderRow;
import com.store.app.admin.dto.TopProductRow;
import com.store.app.admin.service.AdminDashboardService;
import com.store.app.inventory.service.InventoryService;
import com.store.app.order.entity.Order;
import com.store.app.order.entity.OrderStatus;
import com.store.app.order.repository.OrderItemRepository;
import com.store.app.order.repository.OrderRepository;
import com.store.app.product.repository.ProductRepository;
import com.store.app.user.entity.RoleName;
import com.store.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final int RECENT_ORDERS_LIMIT = 8;
    private static final int TOP_PRODUCTS_LIMIT = 5;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        DashboardStats stats = new DashboardStats(
                userRepository.countByRolesName(RoleName.ROLE_CUSTOMER),
                productRepository.count(),
                orderRepository.count(),
                // Booked revenue: everything except cancelled orders.
                orderRepository.sumTotalAmountByStatusNot(OrderStatus.CANCELLED),
                orderRepository.countByCreatedAtAfter(LocalDate.now().atStartOfDay()),
                inventoryService.countLowStock(),
                inventoryService.countOutOfStock());

        List<RecentOrderRow> recentOrders = orderRepository.findTop8ByOrderByIdDesc().stream()
                .limit(RECENT_ORDERS_LIMIT)
                .map(this::toRecentRow)
                .toList();

        List<TopProductRow> topProducts = orderItemRepository.findTopSelling(
                OrderStatus.CANCELLED, PageRequest.of(0, TOP_PRODUCTS_LIMIT));

        return new AdminDashboardResponse(stats, recentOrders, topProducts);
    }

    private RecentOrderRow toRecentRow(Order order) {
        return new RecentOrderRow(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getFirstName() + " " + order.getUser().getLastName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt());
    }
}
