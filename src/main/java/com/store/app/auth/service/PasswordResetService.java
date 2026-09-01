package com.store.app.auth.service;

import com.store.app.auth.dto.ForgotPasswordRequest;
import com.store.app.auth.dto.OtpResponse;
import com.store.app.auth.dto.ResetPasswordRequest;
import com.store.app.auth.dto.ResetTokenResponse;
import com.store.app.auth.dto.VerifyResetOtpRequest;

/**
 * Forgot-password flow: request OTP, verify OTP (issuing a single-use
 * reset token), then reset the password with that token.
 */
public interface PasswordResetService {

    /**
     * Sends a FORGOT_PASSWORD OTP to a registered phone number.
     *
     * @throws com.store.app.exception.ResourceNotFoundException
     *         if no account exists with the phone number
     * @throws com.store.app.exception.OtpRateLimitException
     *         if the resend cooldown or hourly limit is hit
     */
    OtpResponse sendOtp(ForgotPasswordRequest request);

    /**
     * Verifies the FORGOT_PASSWORD OTP and issues a short-lived,
     * single-use reset token. Any previously issued unused token
     * for the phone number is invalidated.
     *
     * @throws com.store.app.exception.OtpException
     *         if the OTP is missing, expired, or wrong
     */
    ResetTokenResponse verifyOtp(VerifyResetOtpRequest request);

    /**
     * Resets the password. Requires a valid, unexpired, unused reset
     * token for the phone number. The new password is BCrypt-encoded,
     * the token is consumed, and all previously issued JWTs become
     * invalid (their issue time predates the password change).
     *
     * @throws com.store.app.exception.InvalidResetTokenException
     *         if the token is wrong, expired, or already used
     * @throws com.store.app.exception.PasswordMismatchException
     *         if newPassword and confirmPassword differ
     */
    OtpResponse resetPassword(ResetPasswordRequest request);
}
