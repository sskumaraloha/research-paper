package com.store.app.auth.service;

import com.store.app.auth.dto.OtpResponse;
import com.store.app.auth.dto.SendOtpRequest;
import com.store.app.auth.dto.VerifyOtpRequest;

/**
 * OTP lifecycle operations. Implementations differ only in how the
 * OTP is delivered (console log, Twilio, another SMS gateway).
 */
public interface OtpService {

    /**
     * Generates and delivers a fresh 6-digit OTP, invalidating any
     * previously active OTP for the same phone number and purpose.
     * Also used for resending.
     *
     * @throws com.store.app.exception.ResourceNotFoundException
     *         if no user exists with the phone number
     * @throws com.store.app.exception.OtpException
     *         if the phone is already verified (REGISTRATION purpose)
     * @throws com.store.app.exception.OtpRateLimitException
     *         if the resend cooldown or hourly request limit is hit
     */
    OtpResponse sendOtp(SendOtpRequest request);

    /**
     * Verifies an OTP. On success the OTP is consumed (single use) and,
     * for the REGISTRATION purpose, the user's phone is marked verified.
     *
     * @throws com.store.app.exception.OtpException
     *         if there is no active OTP, it has expired, or the code is wrong
     * @throws com.store.app.exception.OtpRateLimitException
     *         if the maximum verification attempts are exceeded
     */
    OtpResponse verifyOtp(VerifyOtpRequest request);
}
