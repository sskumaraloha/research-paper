package com.store.app.auth.controller;

import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.dto.VerifyOtpRequest;
import com.store.app.auth.entity.OtpPurpose;
import com.store.app.auth.service.AuthService;
import com.store.app.auth.service.OtpService;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.exception.OtpException;
import com.store.app.exception.OtpRateLimitException;
import com.store.app.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Serves and processes the customer registration and OTP verification pages.
 */
@Controller
@RequiredArgsConstructor
public class AuthPageController {

    private static final String REGISTER_VIEW = "auth/register";
    private static final String VERIFY_OTP_VIEW = "auth/verify-otp";

    private final AuthService authService;
    private final OtpService otpService;

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
                        + "Verify your phone number to finish setting it up.");
        redirectAttributes.addAttribute("phone", request.getPhoneNumber());
        return "redirect:/verify-otp";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/verify-otp")
    public String showOtpVerificationForm(
            @RequestParam(name = "phone", required = false) String phone, Model model) {
        if (!model.containsAttribute("verifyOtpRequest")) {
            VerifyOtpRequest form = new VerifyOtpRequest();
            form.setPhoneNumber(phone);
            form.setPurpose(OtpPurpose.REGISTRATION);
            model.addAttribute("verifyOtpRequest", form);
        }
        return VERIFY_OTP_VIEW;
    }

    @PostMapping("/verify-otp")
    public String processOtpVerification(
            @Valid @ModelAttribute("verifyOtpRequest") VerifyOtpRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return VERIFY_OTP_VIEW;
        }

        try {
            otpService.verifyOtp(request);
        } catch (OtpException | OtpRateLimitException | ResourceNotFoundException ex) {
            bindingResult.reject("otpError", ex.getMessage());
            return VERIFY_OTP_VIEW;
        }

        redirectAttributes.addFlashAttribute("loginMessage",
                "Your phone number has been verified successfully. You can now log in.");
        return "redirect:/login";
    }
}
