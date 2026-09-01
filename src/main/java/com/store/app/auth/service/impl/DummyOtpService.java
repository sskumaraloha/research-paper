package com.store.app.auth.service.impl;

import com.store.app.auth.config.OtpProperties;
import com.store.app.auth.repository.OtpRepository;
import com.store.app.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Development OTP delivery: logs the code to the console instead of
 * sending an SMS. Active when {@code otp.provider=dummy} (the default).
 * <p>
 * To integrate a real SMS gateway later, add e.g. a {@code TwilioOtpService}
 * extending {@link AbstractOtpService}, implement {@code deliverOtp} with the
 * gateway call, annotate it with
 * {@code @ConditionalOnProperty(name = "otp.provider", havingValue = "twilio")},
 * and set {@code otp.provider: twilio} in configuration. No other code changes.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "otp.provider", havingValue = "dummy", matchIfMissing = true)
public class DummyOtpService extends AbstractOtpService {

    public DummyOtpService(OtpRepository otpRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           OtpProperties properties) {
        super(otpRepository, userRepository, passwordEncoder, properties);
    }

    @Override
    protected void deliverOtp(String phoneNumber, String otpCode) {
        // DEVELOPMENT ONLY: real providers must never log the plain OTP.
        log.info("OTP for {} is {}", phoneNumber, otpCode);
    }
}
