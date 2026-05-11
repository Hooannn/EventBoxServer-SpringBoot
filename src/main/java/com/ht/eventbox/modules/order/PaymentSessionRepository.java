package com.ht.eventbox.modules.order;

import com.ht.eventbox.entities.PaymentSession;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentSessionRepository extends JpaRepository<PaymentSession, Long> {

    Optional<PaymentSession> findByOrderIdAndProvider(Long id, String provider);

}
