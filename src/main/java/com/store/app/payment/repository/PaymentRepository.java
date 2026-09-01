package com.store.app.payment.repository;

import com.store.app.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    /** Gateway callbacks identify payments by their external reference. */
    Optional<Payment> findByTransactionReference(String transactionReference);
}
