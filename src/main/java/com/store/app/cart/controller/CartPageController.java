package com.store.app.cart.controller;

import com.store.app.cart.dto.AddToCartRequest;
import com.store.app.cart.service.CartService;
import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.security.StoreUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf cart pages for the authenticated customer.
 */
@Controller
@RequiredArgsConstructor
public class CartPageController {

    private static final String REDIRECT_CART = "redirect:/cart";

    private final CartService cartService;

    @GetMapping("/cart")
    public String viewCart(@AuthenticationPrincipal StoreUserDetails principal, Model model) {
        model.addAttribute("cart", cartService.getCart(userId(principal)));
        return "customer/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(
            @AuthenticationPrincipal StoreUserDetails principal,
            @Valid @ModelAttribute AddToCartRequest addToCartRequest,
            BindingResult bindingResult,
            @RequestParam(name = "productSlug", required = false) String productSlug,
            @RequestParam(name = "redirectTo", defaultValue = "product") String redirectTo,
            RedirectAttributes redirectAttributes) {

        String backToProduct = productSlug != null
                ? "redirect:/products/" + productSlug
                : REDIRECT_CART;
        String target = "cart".equals(redirectTo) ? REDIRECT_CART : backToProduct;

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return backToProduct;
        }

        try {
            cartService.addToCart(userId(principal), addToCartRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Added to your cart.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return backToProduct;
        }
        return target;
    }

    @PostMapping("/cart/items/{itemId}/increase")
    public String increase(@AuthenticationPrincipal StoreUserDetails principal,
                           @PathVariable Long itemId,
                           RedirectAttributes redirectAttributes) {
        return runCartAction(redirectAttributes,
                () -> cartService.increaseItemQuantity(userId(principal), itemId));
    }

    @PostMapping("/cart/items/{itemId}/decrease")
    public String decrease(@AuthenticationPrincipal StoreUserDetails principal,
                           @PathVariable Long itemId,
                           RedirectAttributes redirectAttributes) {
        return runCartAction(redirectAttributes,
                () -> cartService.decreaseItemQuantity(userId(principal), itemId));
    }

    @PostMapping("/cart/items/{itemId}/update")
    public String updateQuantity(@AuthenticationPrincipal StoreUserDetails principal,
                                 @PathVariable Long itemId,
                                 @RequestParam("quantity") int quantity,
                                 RedirectAttributes redirectAttributes) {
        return runCartAction(redirectAttributes,
                () -> cartService.updateItemQuantity(userId(principal), itemId, quantity));
    }

    @PostMapping("/cart/items/{itemId}/remove")
    public String removeItem(@AuthenticationPrincipal StoreUserDetails principal,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        return runCartAction(redirectAttributes,
                () -> cartService.removeItem(userId(principal), itemId));
    }

    @PostMapping("/cart/clear")
    public String clearCart(@AuthenticationPrincipal StoreUserDetails principal,
                            RedirectAttributes redirectAttributes) {
        return runCartAction(redirectAttributes,
                () -> cartService.clearCart(userId(principal)));
    }

    private String runCartAction(RedirectAttributes redirectAttributes, Runnable action) {
        try {
            action.run();
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_CART;
    }

    private Long userId(StoreUserDetails principal) {
        return principal.getUser().getId();
    }
}
