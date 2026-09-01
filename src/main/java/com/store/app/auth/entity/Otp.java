package com.store.app.auth.entity;

import com.store.app.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A one-time password issued to a phone number for a specific purpose.
 * <p>
 * {@code otpCode} holds a BCrypt hash of the code, never the plain value,
 * so a database leak exposes no usable OTPs.
 */
@Entity
@Table(
        name = "otps",
        indexes = @Index(name = "idx_otps_phone_purpose", columnList = "phone_number, purpose")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Otp extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    /** BCrypt hash of the 6-digit code. */
    @Column(name = "otp_code", nullable = false, length = 100)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    public Otp(String phoneNumber, String otpCodeHash, OtpPurpose purpose,
               LocalDateTime expiryTime) {
        this.phoneNumber = phoneNumber;
        this.otpCode = otpCodeHash;
        this.purpose = purpose;
        this.expiryTime = expiryTime;
        this.verified = false;
        this.attemptCount = 0;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    public void markVerified() {
        this.verified = true;
    }

    /** Makes this OTP unusable by expiring it immediately. */
    public void invalidate() {
        this.expiryTime = LocalDateTime.now();
    }
}
