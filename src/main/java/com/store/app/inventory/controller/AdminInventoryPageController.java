package com.store.app.inventory.controller;

import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.inventory.dto.AdjustStockRequest;
import com.store.app.inventory.dto.InventoryResponse;
import com.store.app.inventory.dto.StockUpdateRequest;
import com.store.app.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/**
 * Admin Thymeleaf pages for inventory under /admin/inventory,
 * protected by ROLE_ADMIN through the web security chain.
 */
@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryPageController {

    private static final String LIST_VIEW = "admin/inventory/list";

    private final InventoryService inventoryService;

    @GetMapping
    public String listInventory(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        return renderList("all", search, page, model);
    }

    @GetMapping("/low-stock")
    public String listLowStock(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        return renderList("low", search, page, model);
    }

    @GetMapping("/out-of-stock")
    public String listOutOfStock(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        return renderList("out", search, page, model);
    }

    @GetMapping("/transactions")
    public String listTransactions(
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        model.addAttribute("transactions",
                inventoryService.getTransactions(productId, page, 20));
        model.addAttribute("productId", productId);
        return "admin/inventory/transactions";
    }

    @GetMapping("/{productId}/update")
    public String showUpdateForm(@PathVariable Long productId, Model model) {
        InventoryResponse inventory = inventoryService.getByProductId(productId);
        model.addAttribute("inventory", inventory);

        if (!model.containsAttribute("increaseRequest")) {
            model.addAttribute("increaseRequest", newStockRequest(productId));
        }
        if (!model.containsAttribute("decreaseRequest")) {
            model.addAttribute("decreaseRequest", newStockRequest(productId));
        }
        if (!model.containsAttribute("adjustRequest")) {
            AdjustStockRequest adjust = new AdjustStockRequest();
            adjust.setProductId(productId);
            adjust.setNewStock(inventory.currentStock());
            model.addAttribute("adjustRequest", adjust);
        }
        return "admin/inventory/update";
    }

    @PostMapping("/increase")
    public String increaseStock(
            @Valid @ModelAttribute("increaseRequest") StockUpdateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return applyStockAction(request.getProductId(), bindingResult, redirectAttributes,
                () -> {
                    InventoryResponse updated = inventoryService.increaseStock(request);
                    return "Stock increased. New stock: " + updated.currentStock();
                });
    }

    @PostMapping("/decrease")
    public String decreaseStock(
            @Valid @ModelAttribute("decreaseRequest") StockUpdateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return applyStockAction(request.getProductId(), bindingResult, redirectAttributes,
                () -> {
                    InventoryResponse updated = inventoryService.decreaseStock(request);
                    return "Stock decreased. New stock: " + updated.currentStock();
                });
    }

    @PostMapping("/adjust")
    public String adjustStock(
            @Valid @ModelAttribute("adjustRequest") AdjustStockRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        return applyStockAction(request.getProductId(), bindingResult, redirectAttributes,
                () -> {
                    InventoryResponse updated = inventoryService.adjustStock(request);
                    return "Stock adjusted to " + updated.currentStock() + ".";
                });
    }

    // ------------------------------------------------------------------

    private String renderList(String filter, String search, int page, Model model) {
        model.addAttribute("inventoryPage",
                inventoryService.getInventory(filter, search, page, 15));
        model.addAttribute("filter", filter);
        model.addAttribute("basePath", switch (filter) {
            case "low" -> "/admin/inventory/low-stock";
            case "out" -> "/admin/inventory/out-of-stock";
            default -> "/admin/inventory";
        });
        model.addAttribute("search", search);
        model.addAttribute("lowStockCount", inventoryService.countLowStock());
        model.addAttribute("outOfStockCount", inventoryService.countOutOfStock());
        return LIST_VIEW;
    }

    private String applyStockAction(Long productId, BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    StockAction action) {
        String redirect = "redirect:/admin/inventory/" + productId + "/update";
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return redirect;
        }
        try {
            redirectAttributes.addFlashAttribute("successMessage", action.run());
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirect;
    }

    private StockUpdateRequest newStockRequest(Long productId) {
        StockUpdateRequest request = new StockUpdateRequest();
        request.setProductId(productId);
        return request;
    }

    @FunctionalInterface
    private interface StockAction {
        String run();
    }
}
