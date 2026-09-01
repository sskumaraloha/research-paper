package com.store.app.order.controller;

import com.store.app.exception.OperationNotAllowedException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.order.dto.UpdateOrderStatusRequest;
import com.store.app.order.entity.OrderStatus;
import com.store.app.order.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Admin order management pages (session + ROLE_ADMIN via /admin/**).
 */
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderPageController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public String listOrders(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {

        model.addAttribute("orders", adminOrderService.searchOrders(
                search, status, fromDate, toDate, page, 15));
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statusOptions", OrderStatus.values());
        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("detail", adminOrderService.getOrder(id));
        if (!model.containsAttribute("updateOrderStatusRequest")) {
            model.addAttribute("updateOrderStatusRequest", new UpdateOrderStatusRequest());
        }
        return "admin/orders/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @Valid @ModelAttribute UpdateOrderStatusRequest updateOrderStatusRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/orders/" + id;
        }

        try {
            var detail = adminOrderService.updateStatus(
                    id, updateOrderStatusRequest.getStatus());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order " + detail.order().orderNumber() + " is now "
                            + detail.order().status() + ".");
        } catch (OperationNotAllowedException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
