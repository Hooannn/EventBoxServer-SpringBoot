package com.ht.eventbox.modules.order;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ht.eventbox.entities.Capture;

public interface CaptureRepository extends JpaRepository<Capture, Long> {

}
