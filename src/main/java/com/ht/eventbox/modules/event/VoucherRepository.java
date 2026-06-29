package com.ht.eventbox.modules.event;

import com.ht.eventbox.entities.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    List<Voucher> findAllByEventId(Long eventId);

    List<Voucher> findAllByEventIdOrderByIdDesc(Long eventId);

    Optional<Voucher> findByIdAndEventId(Long id, Long eventId);

    boolean existsByCodeAndEventId(String code, Long eventId);

    List<Voucher> findAllByEventIdOrderByIdAsc(Long eventId);

    @Query(value = """
            SELECT v
            FROM Voucher v
            WHERE v.event.id = :eventId
              AND (
                    :search IS NULL
                    OR :search = ''
                    OR LOWER(STR(v.id)) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY v.id ASC
            """,
            countQuery = """
            SELECT COUNT(v)
            FROM Voucher v
            WHERE v.event.id = :eventId
              AND (
                    :search IS NULL
                    OR :search = ''
                    OR LOWER(STR(v.id)) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Voucher> searchAllByEventIdOrderByIdAsc(
            @Param("eventId") Long eventId,
            @Param("search") String search,
            Pageable pageable);

    List<Voucher> findAllByEventIdAndIsPublicTrueAndIsActiveTrueOrderByIdAsc(Long eventId);

    List<Voucher> findAllByEventIdAndIsPublicTrueAndIsActiveTrueAndValidFromIsLessThanEqualAndValidToIsGreaterThanEqualOrderByIdAsc(Long eventId, LocalDateTime now1, LocalDateTime now2);

    @Query("SELECT v FROM Voucher v JOIN Order o ON v.id = o.voucher.id WHERE o.id = ?2 AND o.user.id = ?1")
    Optional<Voucher> findByUserIdAndOrderId(Long userId, Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Voucher> findByCodeIgnoreCaseAndEventId(String code, Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Voucher> findByCodeIgnoreCaseAndEventIdAndIsActiveTrue(String code, Long eventId);
}
