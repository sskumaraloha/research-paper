package com.store.app.auth.entity;

import com.store.app.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A single-use token proving that its holder completed FORGOT_PASSWORD
 * OTP verification. Required to actually reset the password.
 * <p>
 * {@code tokenHash} holds a BCrypt hash — the plain token is returned
 * to the client exactly once and never persisted.
 */
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = @Index(name = "idx_prt_phone", columnList = "phone_number")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "token_hash", nullable = false, length = 100)
    private String tokenHash;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "used", nullable = false)
    private boolean used;

    public PasswordResetToken(String phoneNumber, String tokenHash, LocalDateTime expiryTime) {
        this.phoneNumber = phoneNumber;
        this.tokenHash = tokenHash;
        this.expiryTime = expiryTime;
        this.used = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public void markUsed() {
        this.used = true;
    }
}
