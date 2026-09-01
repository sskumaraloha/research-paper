package com.store.app.address.controller;

import com.store.app.address.dto.AddressRequest;
import com.store.app.address.dto.AddressResponse;
import com.store.app.address.service.AddressService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Customer address-book pages (session + ROLE_CUSTOMER via /customer/**).
 */
@Controller
@RequestMapping("/customer/addresses")
@RequiredArgsConstructor
public class AddressPageController {

    private static final String LIST_VIEW = "customer/addresses";
    private static final String FORM_VIEW = "customer/address-form";
    private static final String REDIRECT_LIST = "redirect:/customer/addresses";

    private final AddressService addressService;

    @GetMapping
    public String listAddresses(@AuthenticationPrincipal StoreUserDetails principal,
                                Model model) {
        model.addAttribute("addresses",
                addressService.getAddresses(principal.getUser().getId()));
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("addressRequest")) {
            model.addAttribute("addressRequest", new AddressRequest());
        }
        model.addAttribute("formTitle", "Add address");
        model.addAttribute("formAction", "/customer/addresses");
        return FORM_VIEW;
    }

    @PostMapping
    public String createAddress(@AuthenticationPrincipal StoreUserDetails principal,
                                @Valid @ModelAttribute("addressRequest") AddressRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formTitle", "Add address");
            model.addAttribute("formAction", "/customer/addresses");
            return FORM_VIEW;
        }
        addressService.createAddress(principal.getUser().getId(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Address added.");
        return REDIRECT_LIST;
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@AuthenticationPrincipal StoreUserDetails principal,
                               @PathVariable Long id, Model model) {
        if (!model.containsAttribute("addressRequest")) {
            AddressResponse address =
                    addressService.getAddress(principal.getUser().getId(), id);
            AddressRequest form = new AddressRequest();
            form.setFullName(address.fullName());
            form.setPhoneNumber(address.phoneNumber());
            form.setAddressLine1(address.addressLine1());
            form.setAddressLine2(address.addressLine2());
            form.setCity(address.city());
            form.setState(address.state());
            form.setPincode(address.pincode());
            form.setCountry(address.country());
            form.setDefaultAddress(address.defaultAddress());
            model.addAttribute("addressRequest", form);
        }
        model.addAttribute("formTitle", "Edit address");
        model.addAttribute("formAction", "/customer/addresses/" + id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String updateAddress(@AuthenticationPrincipal StoreUserDetails principal,
                                @PathVariable Long id,
                                @Valid @ModelAttribute("addressRequest") AddressRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formTitle", "Edit address");
            model.addAttribute("formAction", "/customer/addresses/" + id);
            return FORM_VIEW;
        }
        try {
            addressService.updateAddress(principal.getUser().getId(), id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Address updated.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/{id}/delete")
    public String deleteAddress(@AuthenticationPrincipal StoreUserDetails principal,
                                @PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            addressService.deleteAddress(principal.getUser().getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Address deleted.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_LIST;
    }

    @PostMapping("/{id}/default")
    public String setDefault(@AuthenticationPrincipal StoreUserDetails principal,
                             @PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        try {
            addressService.setDefaultAddress(principal.getUser().getId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Default address updated.");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_LIST;
    }
}
