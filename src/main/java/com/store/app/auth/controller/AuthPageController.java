package com.store.app.auth.controller;

import com.store.app.auth.dto.ForgotPasswordRequest;
import com.store.app.auth.dto.RegistrationRequest;
import com.store.app.auth.dto.ResetPasswordRequest;
import com.store.app.auth.dto.ResetTokenResponse;
import com.store.app.auth.dto.VerifyOtpRequest;
import com.store.app.auth.dto.VerifyResetOtpRequest;
import com.store.app.auth.entity.OtpPurpose;
import com.store.app.auth.service.AuthService;
import com.store.app.auth.service.OtpService;
import com.store.app.auth.service.PasswordResetService;
import com.store.app.exception.DuplicateResourceException;
import com.store.app.exception.InvalidResetTokenException;
import com.store.app.exception.OtpException;
import com.store.app.exception.OtpRateLimitException;
import com.store.app.exception.PasswordMismatchException;
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
    private static final String FORGOT_PASSWORD_VIEW = "auth/forgot-password";
    private static final String VERIFY_RESET_OTP_VIEW = "auth/verify-reset-otp";
    private static final String RESET_PASSWORD_VIEW = "auth/reset-password";

    private final AuthService authService;
    private final OtpService otpService;
    private final PasswordResetService passwordResetService;

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

    // ------------------------------------------------------------------
    // Forgot password flow
    // ------------------------------------------------------------------

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        if (!model.containsAttribute("forgotPasswordRequest")) {
            model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        }
        return FORGOT_PASSWORD_VIEW;
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return FORGOT_PASSWORD_VIEW;
        }

        try {
            passwordResetService.sendOtp(request);
        } catch (ResourceNotFoundException | OtpException | OtpRateLimitException ex) {
            bindingResult.reject("otpError", ex.getMessage());
            return FORGOT_PASSWORD_VIEW;
        }

        redirectAttributes.addFlashAttribute("infoMessage",
                "An OTP has been sent to your phone number.");
        redirectAttributes.addAttribute("phone", request.getPhoneNumber());
        return "redirect:/verify-reset-otp";
    }

    @GetMapping("/verify-reset-otp")
    public String showVerifyResetOtpForm(
            @RequestParam(name = "phone", required = false) String phone, Model model) {
        if (!model.containsAttribute("verifyResetOtpRequest")) {
            VerifyResetOtpRequest form = new VerifyResetOtpRequest();
            form.setPhoneNumber(phone);
            model.addAttribute("verifyResetOtpRequest", form);
        }
        return VERIFY_RESET_OTP_VIEW;
    }

    @PostMapping("/verify-reset-otp")
    public String processVerifyResetOtp(
            @Valid @ModelAttribute("verifyResetOtpRequest") VerifyResetOtpRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return VERIFY_RESET_OTP_VIEW;
        }

        ResetTokenResponse tokenResponse;
        try {
            tokenResponse = passwordResetService.verifyOtp(request);
        } catch (OtpException | OtpRateLimitException | ResourceNotFoundException ex) {
            bindingResult.reject("otpError", ex.getMessage());
            return VERIFY_RESET_OTP_VIEW;
        }

        // The reset token travels as a flash attribute into a hidden form
        // field — never as a URL parameter (URLs end up in history and logs).
        ResetPasswordRequest resetForm = new ResetPasswordRequest();
        resetForm.setPhoneNumber(request.getPhoneNumber());
        resetForm.setResetToken(tokenResponse.resetToken());
        redirectAttributes.addFlashAttribute("resetPasswordRequest", resetForm);
        redirectAttributes.addFlashAttribute("infoMessage",
                "OTP verified. Choose your new password.");
        return "redirect:/reset-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(Model model) {
        // Without the flash attribute (direct visit or refresh) there is no
        // reset token, so the user must restart the flow.
        if (!model.containsAttribute("resetPasswordRequest")) {
            model.addAttribute("flowExpired", true);
            model.addAttribute("resetPasswordRequest", new ResetPasswordRequest());
        }
        return RESET_PASSWORD_VIEW;
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return RESET_PASSWORD_VIEW;
        }

        try {
            passwordResetService.resetPassword(request);
        } catch (InvalidResetTokenException | PasswordMismatchException
                 | ResourceNotFoundException ex) {
            bindingResult.reject("resetError", ex.getMessage());
            return RESET_PASSWORD_VIEW;
        }

        redirectAttributes.addFlashAttribute("loginMessage",
                "Your password has been reset successfully. Please log in.");
        return "redirect:/login";
    }
}
