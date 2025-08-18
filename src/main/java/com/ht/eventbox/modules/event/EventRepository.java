package com.ht.eventbox.modules.event;

import com.ht.eventbox.entities.Event;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.enums.OrganizationRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrganizationId(Long organizationId);

    Optional<Event> findByIdAndOrganizationUserOrganizationsUserIdAndOrganizationUserOrganizationsRoleIs(Long id, Long userId, OrganizationRole organizationRole);

    List<Event> findAllByOrganizationIdAndStatusIsNot(Long orgId, EventStatus status);

    List<Event> findAllByOrganizationIdAndStatusIsNotOrderByIdAsc(Long orgId, EventStatus status);

    Optional<Event> findByIdAndStatusIsNot(Long eventId, EventStatus eventStatus);

    Optional<Event> findByIdAndStatusIs(Long eventId, EventStatus eventStatus);

    List<Event> findAllByStatusInOrderByIdAsc(Collection<EventStatus> status);

    Page<Event> findByStatusIn(Collection<EventStatus> status, Pageable pageable);

    Page<Event> findByStatusInAndShowsEndTimeAfter(Collection<EventStatus> status, LocalDateTime now, Pageable pageable);

    Page<Event> findDistinctByStatusInAndShowsEndTimeAfter(Collection<EventStatus> status, LocalDateTime now, Pageable pageable);

    Page<Event> findByStatusInAndFeaturedIsTrueAndShowsEndTimeAfter(Collection<EventStatus> status, LocalDateTime now, Pageable pageable);

    Page<Event> findByStatusInAndTrendingIsTrueAndShowsEndTimeAfter(Collection<EventStatus> status, LocalDateTime now, Pageable pageable);

    Page<Event> findDistinctByStatusInAndFeaturedIsTrueAndShowsEndTimeAfter(Collection<EventStatus> status, LocalDateTime now, Pageable pageable);

    Page<Event> findDistinctByStatusInAndTrendingIsTrueAndShowsEndTimeAfter(Collection<EventStatus> status, LocalDateTime now, Pageable pageable);

    Page<Event> findByCategoriesId(Long categoryId, Pageable pageable);

    Page<Event> findByCategoriesIdAndStatusIs(Long categoryId, EventStatus status, Pageable pageable);

    Page<Event> findDistinctByCategoriesIdAndStatusIsAndShowsEndTimeAfter(Long categoryId, EventStatus status, LocalDateTime now, Pageable pageable);

    boolean existsByOrganizationId(Long orgId);

    List<Event> findAllByOrganizationIdAndStatusIsOrderByIdAsc(Long organizationId, EventStatus status);

    Optional<Event> findByShowsId(Long showId);

    @Query("""
    SELECT DISTINCT e FROM Event e
    JOIN e.shows s
    JOIN e.categories c
    WHERE e.status = :status
      AND s.endTime > :now
      AND (
        LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))
      )
      AND (:categoryIds IS NULL OR c.id IN :categoryIds)
    """)
    List<Event> searchEvents(
            @Param("query") String query,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("status") EventStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
    SELECT DISTINCT e FROM Event e
    JOIN e.shows s
    JOIN e.categories c
    WHERE e.status = :status
      AND s.endTime > :now
      AND (
        LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%'))
      )
      AND (LOWER(e.address) LIKE LOWER(CONCAT('%', :province, '%')))
      AND (:categoryIds IS NULL OR c.id IN :categoryIds)
    """)
    List<Event> searchEvents(
            @Param("query") String query,
            @Param("province") String province,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("status") EventStatus status,
            @Param("now") LocalDateTime now
    );

    boolean existsByCategoriesId(Long id);
}
