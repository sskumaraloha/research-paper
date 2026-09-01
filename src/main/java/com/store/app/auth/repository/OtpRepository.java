package com.store.app.auth.repository;

import com.store.app.auth.entity.Otp;
import com.store.app.auth.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    /** Most recent unverified OTP for a phone/purpose, regardless of expiry. */
    Optional<Otp> findTopByPhoneNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
            String phoneNumber, OtpPurpose purpose);

    /** Most recent OTP for a phone/purpose (used for resend cooldown). */
    Optional<Otp> findTopByPhoneNumberAndPurposeOrderByCreatedAtDesc(
            String phoneNumber, OtpPurpose purpose);

    /** Still-active (unverified, unexpired) OTPs for a phone/purpose. */
    List<Otp> findAllByPhoneNumberAndPurposeAndVerifiedFalseAndExpiryTimeAfter(
            String phoneNumber, OtpPurpose purpose, LocalDateTime now);

    /** How many OTPs were issued to a phone/purpose since a point in time. */
    long countByPhoneNumberAndPurposeAndCreatedAtAfter(
            String phoneNumber, OtpPurpose purpose, LocalDateTime since);
}
