package com.store.app.customer.controller;

import com.store.app.exception.AuthenticationFailedException;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.security.StoreUserDetails;
import com.store.app.user.dto.ChangePasswordRequest;
import com.store.app.user.dto.UpdateProfileRequest;
import com.store.app.user.dto.UserResponse;
import com.store.app.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Customer profile page (session + ROLE_CUSTOMER via /customer/**).
 */
@Controller
@RequestMapping("/customer/profile")
@RequiredArgsConstructor
public class ProfilePageController {

    private static final String PROFILE_VIEW = "customer/profile";
    private static final String REDIRECT_PROFILE = "redirect:/customer/profile";

    private final UserService userService;

    @GetMapping
    public String showProfile(@AuthenticationPrincipal StoreUserDetails principal,
                              Model model) {
        UserResponse profile = userService.getUserById(principal.getUser().getId());
        model.addAttribute("profile", profile);

        if (!model.containsAttribute("updateProfileRequest")) {
            UpdateProfileRequest form = new UpdateProfileRequest();
            form.setFirstName(profile.firstName());
            form.setLastName(profile.lastName());
            form.setEmail(profile.email());
            model.addAttribute("updateProfileRequest", form);
        }
        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
        return PROFILE_VIEW;
    }

    @PostMapping
    public String updateProfile(
            @AuthenticationPrincipal StoreUserDetails principal,
            @Valid @ModelAttribute("updateProfileRequest") UpdateProfileRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            prepareProfileModel(principal, model);
            return PROFILE_VIEW;
        }

        try {
            userService.updateProfile(principal.getUser().getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        } catch (DuplicateResourceException ex) {
            bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            prepareProfileModel(principal, model);
            return PROFILE_VIEW;
        }
        return REDIRECT_PROFILE;
    }

    @PostMapping("/password")
    public String changePassword(
            @AuthenticationPrincipal StoreUserDetails principal,
            @Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            preparePasswordModel(principal, model);
            return PROFILE_VIEW;
        }

        try {
            userService.changePassword(principal.getUser().getId(), request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password changed successfully.");
        } catch (AuthenticationFailedException ex) {
            bindingResult.rejectValue("currentPassword", "wrong", ex.getMessage());
            preparePasswordModel(principal, model);
            return PROFILE_VIEW;
        }
        return REDIRECT_PROFILE;
    }

    // ------------------------------------------------------------------

    /** Re-render after a failed profile submit: keep a fresh password form. */
    private void prepareProfileModel(StoreUserDetails principal, Model model) {
        model.addAttribute("profile",
                userService.getUserById(principal.getUser().getId()));
        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
    }

    /** Re-render after a failed password submit: keep a filled profile form. */
    private void preparePasswordModel(StoreUserDetails principal, Model model) {
        UserResponse profile = userService.getUserById(principal.getUser().getId());
        model.addAttribute("profile", profile);
        if (!model.containsAttribute("updateProfileRequest")) {
            UpdateProfileRequest form = new UpdateProfileRequest();
            form.setFirstName(profile.firstName());
            form.setLastName(profile.lastName());
            form.setEmail(profile.email());
            model.addAttribute("updateProfileRequest", form);
        }
    }
}
