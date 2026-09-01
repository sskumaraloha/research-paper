package com.store.app.order.controller;

import com.store.app.address.service.AddressService;
import com.store.app.cart.dto.CartResponse;
import com.store.app.cart.service.CartService;
import com.store.app.exception.BusinessValidationException;
import com.store.app.exception.ResourceNotFoundException;
import com.store.app.order.dto.OrderResponse;
import com.store.app.order.dto.PlaceOrderRequest;
import com.store.app.order.service.OrderService;
import com.store.app.security.StoreUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Checkout page for authenticated customers.
 */
@Controller
@RequiredArgsConstructor
public class CheckoutPageController {

    private final CartService cartService;
    private final AddressService addressService;
    private final OrderService orderService;

    @GetMapping("/checkout")
    public String showCheckout(@AuthenticationPrincipal StoreUserDetails principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Long userId = principal.getUser().getId();
        CartResponse cart = cartService.getCart(userId);
        if (cart.items().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Your cart is empty — add something before checking out.");
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("addresses", addressService.getAddresses(userId));
        if (!model.containsAttribute("placeOrderRequest")) {
            PlaceOrderRequest form = new PlaceOrderRequest();
            addressService.getAddresses(userId).stream()
                    .filter(a -> a.defaultAddress())
                    .findFirst()
                    .ifPresent(a -> form.setAddressId(a.id()));
            model.addAttribute("placeOrderRequest", form);
        }
        return "customer/checkout";
    }

    @PostMapping("/checkout/place-order")
    public String placeOrder(@AuthenticationPrincipal StoreUserDetails principal,
                             @Valid @ModelAttribute PlaceOrderRequest placeOrderRequest,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/checkout";
        }

        try {
            OrderResponse order = orderService.placeOrder(
                    principal.getUser().getId(), placeOrderRequest);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order " + order.orderNumber() + " placed successfully!");
            return "redirect:/customer/orders/" + order.id();
        } catch (BusinessValidationException | ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/checkout";
        }
    }
}
