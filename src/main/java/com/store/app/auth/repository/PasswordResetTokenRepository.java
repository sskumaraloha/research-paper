package com.store.app.auth.repository;

import com.store.app.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findTopByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(
            String phoneNumber);

    List<PasswordResetToken> findAllByPhoneNumberAndUsedFalse(String phoneNumber);
}
