package com.store.app.admin.dto;

import java.math.BigDecimal;

/**
 * Headline numbers of the admin dashboard.
 */
public record DashboardStats(
        long totalCustomers,
        long totalProducts,
        long totalOrders,
        BigDecimal totalSales,
        long todaysOrders,
        long lowStockProducts,
        long outOfStockProducts
) {
}
