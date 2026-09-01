package com.store.app.wishlist.controller;

import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.security.StoreUserDetails;
import com.store.app.wishlist.service.WishlistService;
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
 * Customer wishlist pages (session + ROLE_CUSTOMER via /customer/**).
 */
@Controller
@RequestMapping("/customer/wishlist")
@RequiredArgsConstructor
public class WishlistPageController {

    private static final String REDIRECT_WISHLIST = "redirect:/customer/wishlist";

    private final WishlistService wishlistService;

    @GetMapping
    public String viewWishlist(@AuthenticationPrincipal StoreUserDetails principal,
                               Model model) {
        model.addAttribute("wishlist",
                wishlistService.getWishlist(principal.getUser().getId()));
        return "customer/wishlist";
    }

    @PostMapping("/add")
    public String addProduct(@AuthenticationPrincipal StoreUserDetails principal,
                             @RequestParam("productId") Long productId,
                             @RequestParam(name = "productSlug", required = false) String productSlug,
                             RedirectAttributes redirectAttributes) {
        try {
            wishlistService.addProduct(principal.getUser().getId(), productId);
            redirectAttributes.addFlashAttribute("successMessage", "Added to your wishlist.");
        } catch (DuplicateResourceException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return productSlug != null
                ? "redirect:/products/" + productSlug
                : REDIRECT_WISHLIST;
    }

    @PostMapping("/items/{itemId}/remove")
    public String removeItem(@AuthenticationPrincipal StoreUserDetails principal,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            wishlistService.removeItem(principal.getUser().getId(), itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Removed from wishlist.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_WISHLIST;
    }

    @PostMapping("/items/{itemId}/move-to-cart")
    public String moveToCart(@AuthenticationPrincipal StoreUserDetails principal,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            wishlistService.moveToCart(principal.getUser().getId(), itemId);
            redirectAttributes.addFlashAttribute("successMessage", "Moved to your cart.");
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_WISHLIST;
    }
}
