package com.ht.eventbox.modules.order;

import com.ht.eventbox.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaypalOrderId(String id);
}
