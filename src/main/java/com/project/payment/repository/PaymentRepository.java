package com.project.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.payment.domain.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	Optional<Payment> findByOrderId(String orderId);

	Optional<Payment> findByPaymentKeyAndMember_Email(String paymentKey, String email);
}
