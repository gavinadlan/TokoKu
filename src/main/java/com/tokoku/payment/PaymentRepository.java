package com.tokoku.payment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Webhook yang sama datang dua kali serentak: baris dikunci sehingga diproses satu per satu
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.providerRef = :ref")
    Optional<Payment> findByProviderRefForUpdate(@Param("ref") String ref);

    Optional<Payment> findByProviderRef(String ref);

    Optional<Payment> findFirstByOrderIdAndStatus(Long orderId, PaymentStatus status);

    List<Payment> findByOrderId(Long orderId);
}
