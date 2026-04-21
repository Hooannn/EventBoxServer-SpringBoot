package com.ht.eventbox.modules.order;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ht.eventbox.entities.Refund;

public interface RefundRepository extends JpaRepository<Refund, Long> {

}
