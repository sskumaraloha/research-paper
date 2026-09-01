package com.store.app.admin.controller;

import com.store.app.admin.dto.AdminDashboardResponse;
import com.store.app.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin dashboard page (session + ROLE_ADMIN via the /admin/** rule).
 */
@Controller
@RequiredArgsConstructor
public class AdminDashboardPageController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        AdminDashboardResponse dashboard = adminDashboardService.getDashboard();
        model.addAttribute("stats", dashboard.stats());
        model.addAttribute("recentOrders", dashboard.recentOrders());
        model.addAttribute("topProducts", dashboard.topProducts());
        return "admin/dashboard";
    }
}
