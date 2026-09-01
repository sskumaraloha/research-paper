package com.store.app.admin.dto;

import java.util.List;

/**
 * The complete admin dashboard payload.
 */
public record AdminDashboardResponse(
        DashboardStats stats,
        List<RecentOrderRow> recentOrders,
        List<TopProductRow> topProducts
) {
}
