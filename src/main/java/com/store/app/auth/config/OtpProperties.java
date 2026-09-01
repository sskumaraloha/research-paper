package com.store.app.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tunable OTP settings, bound from the {@code otp.*} configuration keys.
 *
 * @param provider              which OtpService implementation delivers OTPs
 *                              ({@code dummy} logs to the console; an SMS
 *                              provider such as {@code twilio} can be added later)
 * @param expiryMinutes         how long an OTP stays valid
 * @param maxAttempts           maximum verification attempts per OTP
 * @param resendCooldownSeconds minimum delay between two OTP requests
 * @param maxRequestsPerHour    maximum OTPs issued per phone/purpose per hour
 */
@ConfigurationProperties(prefix = "otp")
public record OtpProperties(
        @DefaultValue("dummy") String provider,
        @DefaultValue("5") int expiryMinutes,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("60") long resendCooldownSeconds,
        @DefaultValue("5") int maxRequestsPerHour
) {
}
