package com.ht.eventbox.modules.order;

import com.ht.eventbox.entities.Order;
import com.ht.eventbox.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByItemsTicketEventShowIdAndStatusIsAndFulfilledAtBetween(
            Long eventShowId, OrderStatus status, LocalDateTime start, LocalDateTime end);

    List<Order> findAllByItemsTicketEventShowIdAndStatusIs(Long eventShowId, OrderStatus status);

    List<Order> findAllByItemsTicketEventShowIdAndStatusIsOrderByIdAsc(Long showId, OrderStatus orderStatus);

    @Query(value = """
            SELECT DISTINCT o
            FROM Order o
            JOIN o.user u
            JOIN o.items i
            JOIN i.ticket t
            JOIN t.eventShow s
            WHERE s.id = :showId
              AND o.status = :status
              AND (
                    :search IS NULL
                    OR :search = ''
                    OR LOWER(STR(o.id)) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, '')))
                        LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            ORDER BY o.id ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT o.id)
            FROM Order o
            JOIN o.user u
            JOIN o.items i
            JOIN i.ticket t
            JOIN t.eventShow s
            WHERE s.id = :showId
              AND o.status = :status
              AND (
                    :search IS NULL
                    OR :search = ''
                    OR LOWER(STR(o.id)) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, '')))
                        LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(u.firstName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(u.lastName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Order> searchAllByItemsTicketEventShowIdAndStatusIsOrderByIdAsc(
            @Param("showId") Long showId,
            @Param("status") OrderStatus orderStatus,
            @Param("search") String search,
            Pageable pageable);

    long deleteAllByStatusInAndExpiredAtBefore(List<OrderStatus> statuses, LocalDateTime now);

    long deleteAllByUserIdAndStatusIs(Long userId, OrderStatus orderStatus);

    long deleteByIdAndUserIdAndStatusInAndExpiredAtAfter(Long orderId, Long userId, List<OrderStatus> statuses, LocalDateTime now);

    Optional<Order> findByIdAndStatusIs(Long id, OrderStatus status);

    Optional<Order> findByIdAndStatusIn(Long id, List<OrderStatus> statuses);

    Optional<Order> findByIdAndUserIdAndStatusIn(Long orderId, Long userId, List<OrderStatus> statuses);

    Optional<Order> findByIdAndUserIdAndStatusInAndExpiredAtAfter(Long id, Long userId, Collection<OrderStatus> statuses, LocalDateTime expiredAt);

    boolean existsByVoucherId(Long voucherId);

    long countByVoucherIdAndStatusIs(Long voucherId, OrderStatus status);

    long countByUserIdAndVoucherIdAndStatusIn(Long userId, Long voucherId, List<OrderStatus> statuses);

    long countByUserIdAndVoucherId(Long userId, Long voucherId);

    long countByVoucherId(Long id);

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    List<Order> findAllByItemsTicketEventShowEventIdAndStatusIs(Long eventId, OrderStatus orderStatus);
}
