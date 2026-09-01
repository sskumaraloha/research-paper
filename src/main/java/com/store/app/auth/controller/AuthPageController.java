package com.store.app.auth.controller;

import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.service.AuthService;
import com.store.app.exception.DuplicateResourceException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Serves and processes the customer registration page.
 */
@Controller
@RequiredArgsConstructor
public class AuthPageController {

    private static final String REGISTER_VIEW = "auth/register";

    private final AuthService authService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registrationRequest")) {
            model.addAttribute("registrationRequest", new RegistrationRequest());
        }
        return REGISTER_VIEW;
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return REGISTER_VIEW;
        }

        try {
            authService.registerCustomer(request);
        } catch (DuplicateResourceException ex) {
            if (ex.getField() != null) {
                bindingResult.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            } else {
                bindingResult.reject("duplicate", ex.getMessage());
            }
            return REGISTER_VIEW;
        }

        redirectAttributes.addFlashAttribute("registrationSuccess",
                "Your account has been created successfully. "
                        + "You can log in once login is available.");
        return "redirect:/register";
    }
}
