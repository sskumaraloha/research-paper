package com.store.app.admin.service;

import com.store.app.admin.dto.AdminDashboardResponse;

/**
 * Aggregates the figures shown on the admin dashboard.
 */
public interface AdminDashboardService {

    AdminDashboardResponse getDashboard();
}
