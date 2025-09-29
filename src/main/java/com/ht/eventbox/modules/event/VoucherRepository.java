package com.ht.eventbox.modules.event;

import com.ht.eventbox.entities.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    List<Voucher> findAllByEventId(Long eventId);

    List<Voucher> findAllByEventIdOrderByIdDesc(Long eventId);

    Optional<Voucher> findByIdAndEventId(Long id, Long eventId);

    boolean existsByCodeAndEventId(String code, Long eventId);

    List<Voucher> findAllByEventIdOrderByIdAsc(Long eventId);

    List<Voucher> findAllByEventIdAndPublicIsTrueAndActiveIsTrueOrderByIdAsc(Long eventId);
}
