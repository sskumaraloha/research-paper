package com.store.app.order.controller;

import com.store.app.exception.OperationNotAllowedException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.order.service.OrderService;
import com.store.app.security.StoreUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Customer order history pages (session + ROLE_CUSTOMER via /customer/**).
 */
@Controller
@RequestMapping("/customer/orders")
@RequiredArgsConstructor
public class OrderPageController {

    private final OrderService orderService;

    @GetMapping
    public String listOrders(@AuthenticationPrincipal StoreUserDetails principal,
                             @RequestParam(name = "page", defaultValue = "0") int page,
                             Model model) {
        model.addAttribute("orders",
                orderService.getOrders(principal.getUser().getId(), page, 10));
        return "customer/orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@AuthenticationPrincipal StoreUserDetails principal,
                              @PathVariable Long id, Model model) {
        model.addAttribute("order",
                orderService.getOrder(principal.getUser().getId(), id));
        return "customer/order-detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@AuthenticationPrincipal StoreUserDetails principal,
                              @PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(principal.getUser().getId(), id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order cancelled. Any reserved stock has been released.");
        } catch (OperationNotAllowedException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/orders/" + id;
    }
}
