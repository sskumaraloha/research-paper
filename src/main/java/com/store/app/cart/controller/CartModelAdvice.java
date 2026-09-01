package com.store.app.cart.controller;

import com.store.app.cart.service.CartService;
import com.store.app.customer.controller.StorefrontController;
import com.store.app.security.StoreUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Supplies the navbar cart badge count on storefront and cart pages.
 */
@ControllerAdvice(assignableTypes = {StorefrontController.class, CartPageController.class})
@RequiredArgsConstructor
public class CartModelAdvice {

    private final CartService cartService;

    @ModelAttribute("cartItemCount")
    public Integer cartItemCount(@AuthenticationPrincipal StoreUserDetails principal) {
        return principal == null ? null : cartService.countItems(principal.getUser().getId());
    }
}
