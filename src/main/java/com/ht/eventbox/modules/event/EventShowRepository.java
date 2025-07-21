package com.ht.eventbox.modules.event;

import com.ht.eventbox.entities.EventShow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventShowRepository extends JpaRepository<EventShow, Long> {
    List<EventShow> findAllByEventId(Long eventId);

    List<EventShow> findAllByEventIdOrderByIdAsc(Long eventId);
}
