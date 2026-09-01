package com.store.app.cart.controller;

import com.store.app.address.controller.AddressPageController;
import com.store.app.cart.service.CartService;
import com.store.app.customer.controller.ProfilePageController;
import com.store.app.customer.controller.StorefrontController;
import com.store.app.order.controller.CheckoutPageController;
import com.store.app.order.controller.OrderPageController;
import com.store.app.security.StoreUserDetails;
import com.store.app.wishlist.controller.WishlistPageController;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Supplies the navbar cart badge count on every customer-facing page.
 */
@ControllerAdvice(assignableTypes = {
        StorefrontController.class, CartPageController.class,
        CheckoutPageController.class, OrderPageController.class,
        WishlistPageController.class, AddressPageController.class,
        ProfilePageController.class})
@RequiredArgsConstructor
public class CartModelAdvice {

    private final CartService cartService;

    @ModelAttribute("cartItemCount")
    public Integer cartItemCount(@AuthenticationPrincipal StoreUserDetails principal) {
        return principal == null ? null : cartService.countItems(principal.getUser().getId());
    }
}
